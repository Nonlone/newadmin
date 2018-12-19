package com.feitai.admin.mop.advert.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.feitai.admin.mop.advert.dao.entity.AdvertBlock;
import com.feitai.admin.mop.advert.dao.entity.AdvertEditCopy;
import com.feitai.admin.mop.advert.dao.entity.AdvertEditCopy.AdvertEditCopyContent;
import com.feitai.admin.mop.advert.dao.entity.AdvertGroupBlock;
import com.feitai.admin.mop.advert.dao.mapper.AdvertBlockItemMapper;
import com.feitai.admin.mop.advert.dao.mapper.AdvertBlockMapper;
import com.feitai.admin.mop.advert.dao.mapper.AdvertGroupBlockMapper;
import com.feitai.admin.mop.advert.enums.AdvertBlockStatusEnum;
import com.feitai.admin.mop.advert.enums.AdvertEditCopyRelTypeEnum;
import com.feitai.admin.mop.base.BusinessException;
import com.feitai.admin.mop.base.ListUtils;
import com.feitai.admin.mop.base.RpcResult;
import com.feitai.utils.SnowFlakeIdGenerator;
import com.feitai.utils.http.OkHttpClientUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.util.Sqls;

import java.io.IOException;
import java.util.*;

/**
 * @Author qiuyunlong
 */
@Service
public class AdvertBlockService {
	@Autowired
	private AdvertBlockMapper advertBlockMapper;

	@Autowired
	private AdvertBlockItemMapper advertBlockItemMapper;

	@Autowired
	private AdvertGroupBlockMapper advertGroupBlockMapper;

	@Autowired
	private AdvertEditCopyService advertEditCopyService;

	@Autowired
	private AdvertGroupService advertGroupService;

	@Autowired
	private AdvertItemService advertItemService;

	@Value("${mop.advert.block.evictCache.url}")
	private String blockEvictUrl;

	public PageInfo<AdvertBlock> list(int pageNo, int pageSize, Long blockId, Date startTime, Date endTime, Integer status, String orderField, String order) {
		PageHelper.startPage(pageNo, pageSize);
		Example example = new Example(AdvertBlock.class);
		Example.Criteria criteria = example.createCriteria();
		if (status != null) {
			criteria.andEqualTo("status", status);
		}
		if (startTime != null) {
			criteria.andGreaterThanOrEqualTo("beginTime", startTime);
		}
		if (endTime != null) {
			criteria.andLessThanOrEqualTo("endTime", endTime);
		}
		if (blockId != null) {
            criteria.andEqualTo("id", blockId);
        }
		if (orderField != null) {
			if ("DESC".equals(order)) {
				example.orderBy(orderField).desc();
			} else {
				example.orderBy(orderField).asc();
			}
		}
		List<AdvertBlock> advertBlockList = advertBlockMapper.selectByExample(example);
		PageInfo pageInfo = new PageInfo(advertBlockList);
		List<Object> list = new ArrayList<Object>();

		if (null != advertBlockList && !advertBlockList.isEmpty()) {
			for (AdvertBlock block : advertBlockList) {
				JSONObject obj = (JSONObject) JSONObject.toJSON(block);
				Long count = advertBlockItemMapper.countByBlockId(block.getId());

				List<Long> groupIds = new ArrayList<Long>();

				if (0 < block.getEditCopyId()) {
					AdvertEditCopy editCopy = advertEditCopyService.getEditCopy(block.getEditCopyId());

					AdvertEditCopyContent content = JSONObject.parseObject(editCopy.getContent(),
							AdvertEditCopyContent.class);
					groupIds = content.getRelations();
					obj = (JSONObject) JSONObject.parseObject(content.getEditInfo());
				}
				else{
					groupIds = advertGroupBlockMapper.queryGroupIdsByBlockId(block.getId());
				}
				
				long childEditCount = advertEditCopyService.countByTargetRelId(block.getId(), AdvertEditCopyRelTypeEnum.ADVERT_BLOCK);

				obj.put("originalStatus", block.getStatus());
				obj.put("groupIds", StringUtils.join(groupIds, ","));
				obj.put("itemCount", null == count ? 0 : count);
				obj.put("childEditCount", childEditCount);
				list.add(obj);
			}
		}
		pageInfo.setList(list);
 		return pageInfo;
	}

	public void addBlock(AdvertBlock advertBlock, String groupIds) {
		long id = SnowFlakeIdGenerator.getDefaultNextId();
		advertBlock.setId(id);
		advertBlock.setCode(String.valueOf(id));
		advertBlock.setCreatedTime(new Date());
		advertBlock.setPlayTime(1);
		advertBlock.setStatus(1);
		advertBlock.setVersion(System.currentTimeMillis());
		advertBlock.setUpdateTime(new Date());
		
		if (StringUtils.isNotEmpty(groupIds)) {
			String[] groupIdArray = groupIds.split(",");
			for (String groupId : groupIdArray) {
				advertGroupBlockMapper
						.insert(new AdvertGroupBlock(null, Long.parseLong(groupId), advertBlock.getId(), 0, new Date()));
			}
		}
		
		advertBlockMapper.insertSelective(advertBlock);
	}

	public void updateBlock(AdvertBlock advertBlock, String groupIds, String operator) {
		
		AdvertBlock block = get(advertBlock.getId());

		// 新建状态不需要保存编辑副本
		if (AdvertBlockStatusEnum.NEW.getValue() != block.getStatus() 
				|| 0 < block.getEditCopyId()) {
			updateBlockEditCopy(block, advertBlock, groupIds, operator);
			return;
		}

		advertBlock.setUpdateTime(new Date());
		advertBlock.setVersion(System.currentTimeMillis());
		
		
		List<Long> groupIdList = new ArrayList<Long>();

		if (StringUtils.isNotBlank(groupIds)) {
			groupIdList = ListUtils.toLongList(groupIds, ",");
		} else {
			groupIdList = advertBlockItemMapper.queryBlockIdsByItemId(block.getId());
		}

		updateGroupBlock(advertBlock, groupIdList);
		
		advertBlockMapper.updateByPrimaryKeySelective(advertBlock);
	}
	
	
	private void updateBlockEditCopy(AdvertBlock readBlock, AdvertBlock updateBlock, String groupIdString, String operator) {
		AdvertBlock refBlock = readBlock;

		if (0 < readBlock.getEditCopyId()) {
			refBlock = advertEditCopyService.getEditInfoObj(readBlock.getEditCopyId(), AdvertBlock.class);
		}

		updateBlock.setEditCopyId(refBlock.getEditCopyId());
		updateBlock.setVersion(refBlock.getVersion());
		updateBlock.setStatus(refBlock.getStatus());
		updateBlock.setCreatedTime(refBlock.getCreatedTime());
		updateBlock.setUpdateTime(new Date());
		updateBlock.setPublishTime(refBlock.getPublishTime());

		List<Long> groupIds = new ArrayList<Long>();
		if (StringUtils.isNotBlank(groupIdString)) {
			groupIds = ListUtils.toLongList(groupIdString, ",");
		} else {
			groupIds = getGroupIdsWithEditCopy(readBlock.getId(),readBlock.getEditCopyId());
		}

		// 更新编辑副本
		updateWithEditCopy(readBlock.getId(), readBlock.getEditCopyId(), updateBlock, groupIds, operator);
	}

	public List<AdvertBlock> list() {
		return advertBlockMapper.selectAll();
	}

	public int updateVersion(long id) {
		Example example = Example.builder(AdvertGroupBlock.class)
				.andWhere(Sqls.custom().andEqualTo("blockId", id)).build();
		
		List<AdvertGroupBlock> list = advertGroupBlockMapper.selectByExample(example);

		// 1.先更新block版本
		AdvertBlock update = new AdvertBlock();
		update.setId(id);
		update.setUpdateTime(new Date());
		update.setVersion(System.currentTimeMillis());
		int flag = advertBlockMapper.updateByPrimaryKeySelective(update);

		// 2.再更新group版本
		list.forEach(advertGroupBlock -> advertGroupService.updateVersion(advertGroupBlock.getGroupId()));

		return flag;
	}

	public void delete(long blockId) {
		if (0 >= blockId) {
			throw new BusinessException("广告模块不存在");
		}

		Example example = new Example(AdvertBlock.class);
		example.createCriteria().andEqualTo("id", blockId);
		AdvertBlock read = advertBlockMapper.selectOneByExample(example);

		if (null == read) {
			throw new BusinessException("广告模块不存在");
		}

		if (1 != read.getStatus()) {
			throw new BusinessException("广告模块状态不支持删除操作");
		}
		
		if (0 < itemCount(blockId)) {
			throw new BusinessException("广告模块存在关联广告内容不支持删除操作");
		}

		Example delExample = new Example(AdvertBlock.class);
		delExample.createCriteria().andEqualTo("id", blockId);
		advertBlockMapper.deleteByExample(delExample);
	}
	

	public void updateStatus(long blockId, AdvertBlockStatusEnum updateStatus, String operator) {

		if (0 >= blockId) {
			throw new BusinessException("广告模块不存在");
		}

		AdvertBlock read = get(blockId);
		
		AdvertBlock updateEntity = read;

		if (null == read) {
			throw new BusinessException("广告模块不存在");
		}

		if (updateStatus.getValue() == read.getStatus()) {
			return;
		}
		
		if (0 < read.getEditCopyId()) {
			updateEntity = getAdvertBlockFromEditCopy(read.getEditCopyId());
		}
		
		updateEntity.setId(blockId);
		updateEntity.setStatus(updateStatus.getValue());

		List<Long> groupIds = getGroupIdsWithEditCopy(blockId, read.getEditCopyId());
		
		// 更新编辑副本
		updateWithEditCopy(blockId, read.getEditCopyId(), updateEntity, groupIds, operator);
	}
	
	private List<Long> getGroupIdsWithEditCopy(long blockId, long editCopyId) {

		if (0 < editCopyId) {
			return getGroupIdsFromEditCopy(editCopyId);
		}

		return advertGroupBlockMapper.queryGroupIdsByBlockId(blockId);
	}
	
	private List<Long> getGroupIdsFromEditCopy(long editCopyId) {
		AdvertEditCopy editCopy = advertEditCopyService.getEditCopy(editCopyId);

		if (null == editCopy) {
			return null;
		}

		AdvertEditCopyContent content = JSONObject.parseObject(editCopy.getContent(), AdvertEditCopyContent.class);

		if (null == content) {
			return null;
		}

		return content.getRelations();
	}


	private AdvertBlock getAdvertBlockFromEditCopy(long editCopyId) {
		AdvertEditCopy editCopy = advertEditCopyService.getEditCopy(editCopyId);

		if (null == editCopy) {
			return null;
		}

		AdvertEditCopyContent content = JSONObject.parseObject(editCopy.getContent(), AdvertEditCopyContent.class);

		if (null == content) {
			return null;
		}

		return content.getEditInfoObj(AdvertBlock.class);
	}
	

	public AdvertBlock get(long blockId) {
		return advertBlockMapper.selectByPrimaryKey(blockId);
	}

	public AdvertBlock getWithEditCopy(long blockId) {
		AdvertBlock block = advertBlockMapper.selectByPrimaryKey(blockId);
		if (0 == block.getEditCopyId()) {
			return block;
		}
		return getAdvertBlockFromEditCopy(block.getEditCopyId());
	}
	
	private long itemCount(long blockId) {
		Long count = advertBlockItemMapper.countByBlockId(blockId);
		return null == count ? 0L : count;
	}

	
	/**
	 * 更新编辑副本内容
	 * 
	 * @param blockId
	 * @param editCopyId
	 * @param editInfo
	 * @param operator
	 */
	private void updateWithEditCopy(long blockId, long editCopyId, AdvertBlock editInfo,
			List<Long> editBlockIds, String operator) {

		if (0 == editCopyId) {
			long copyId = SnowFlakeIdGenerator.getDefaultNextId();
			editInfo.setEditCopyId(copyId);

			// 添加副本
			advertEditCopyService.addEditCopy(copyId, AdvertEditCopyRelTypeEnum.ADVERT_BLOCK, blockId, editInfo,
					AdvertEditCopyRelTypeEnum.ADVERT_GROUP, editBlockIds, operator);

			AdvertBlock update = new AdvertBlock();
			update.setId(blockId);
			update.setEditCopyId(copyId);
			advertBlockMapper.updateByPrimaryKeySelective(update);
		} else {
			advertEditCopyService.updateEditCopy(editCopyId, AdvertEditCopyRelTypeEnum.ADVERT_ITEM, blockId, editInfo,
					AdvertEditCopyRelTypeEnum.ADVERT_BLOCK, editBlockIds, operator);
		}
	}
	
	public void publishEditCopy(long blockId, String operator) {
		publishAdvertBlockEditCopy(blockId, operator);
		
		List<Long> itemIds = advertEditCopyService.queryRelIdsByTargetRelId(blockId, AdvertEditCopyRelTypeEnum.ADVERT_BLOCK);
		
		if (ListUtils.isEmpty(itemIds)) {
			return;
		}
		
		for (Long itemId : itemIds) {
			advertItemService.publishAdvertItemEditCopy(itemId, operator);
		}
	}

	/**
	 * 发布广告模块编辑副本为正式数据
	 * 
	 * @param blockId
	 * @param operator
	 */
	public void publishAdvertBlockEditCopy(long blockId, String operator) {

		// 1.查询广告模块
		AdvertBlock read = get(blockId);

		// 判断广告模块是否存在
		if (null == read) {
			throw new BusinessException("广告模块不存在");
		}

		// 是否关联有判断编辑副本
		if (0 >= read.getEditCopyId()) {
			return;
		}

		AdvertEditCopy editCopy = advertEditCopyService.getEditCopy(read.getEditCopyId());

		// 判断广告模块编辑副本是否存在
		if (null == editCopy) {
			throw new BusinessException("广告模块编辑副本不存在");
		}

		// 2.发布编辑副本内容
		AdvertEditCopyContent content = JSONObject.parseObject(editCopy.getContent(), AdvertEditCopyContent.class);

		AdvertBlock block = content.getEditInfoObj(AdvertBlock.class);
		block.setEditCopyId(0L);
		block.setPublishTime(new Date());

		// 更新编辑副本内容
		advertBlockMapper.updateByPrimaryKeySelective(block);

		updateGroupBlock(block, content.getRelations());

		// 3.更新编辑副本发布信息记录
		advertEditCopyService.publishEditCopy(editCopy.getId(), operator);

		// 4.执行版本更新
		updateVersion(blockId);

		// 5.执行清除缓存
		evictCache(blockId);
	}
	
	
	/**
	 * 重置副本
	 * @param blockId
	 * @param operator
	 */
	public void resetAdvertBlockEditCopy(long blockId, String operator) {
		// 1.查询广告模块
		AdvertBlock read = get(blockId);

		// 判断广告模块是否存在
		if (null == read) {
			throw new BusinessException("广告模块不存在");
		}

		// 是否关联有判断编辑副本
		if (0 >= read.getEditCopyId()) {
			return;
		}

		advertEditCopyService.deleteEditCopy(read.getEditCopyId());

		AdvertBlock block = new AdvertBlock();
		block.setId(blockId);
		block.setEditCopyId(0L);

		// 更新编辑副本内容
		advertBlockMapper.updateByPrimaryKeySelective(block);
	}
	
	
	private void updateGroupBlock(AdvertBlock block, List<Long> groupIds) {
		Example example = new Example(AdvertGroupBlock.class);
		example.createCriteria().andEqualTo("blockId", block.getId());
		
		List<AdvertGroupBlock> advertGroupBlockList = advertGroupBlockMapper.selectByExample(example);
		
		Map<Long, Boolean> groupMap = new HashMap<>();

		if (null != groupIds && 0 < groupIds.size()) {
			for (Long groupId : groupIds) {
				groupMap.put(groupId, false);
			}
		}

		advertGroupBlockList.forEach(advertGroupBlock -> {
			if (groupMap.containsKey(advertGroupBlock.getGroupId())) {
				groupMap.put(advertGroupBlock.getGroupId(), true);
			} else {
				advertGroupBlockMapper.deleteByPrimaryKey(advertGroupBlock.getId());
			}
		});
		groupMap.forEach((groupId, exists) -> {
			if (!exists) {
				advertGroupBlockMapper.insert(new AdvertGroupBlock(null, groupId, block.getId(), 0, new Date()));
			}
		});
	}
	

	public boolean evictCache(long blockId) {
		RpcResult result = null;
		try {
			result = doEvictCache(blockId);
		} catch (IOException e) {
			throw new BusinessException("缓存更新失败", e);
		}
		if (null != result && result.isSuccess()) {
            return true;
        } else {
            throw new BusinessException("缓存更新失败" + (null != result ? result.getMessage() : ""));
        }
	}

	private RpcResult doEvictCache(long id) throws IOException {
		JSONObject param = new JSONObject();
		param.put("blockId", id);
		JSONObject data = new JSONObject();
		data.put("data", param);
		String resultStr = OkHttpClientUtils.postReturnBody(blockEvictUrl, data);
		return JSON.parseObject(resultStr, RpcResult.class);
	}

}
