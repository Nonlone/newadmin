package com.feitai.admin.mop.advert.dao.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.ToString;

import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

/**
* AdvertBlock数据实体类
*/
@Data
@ToString(callSuper = true)
@Table(name = "t_advert_block")
public class AdvertBlock implements Serializable{
    /**
	 *
	 */
	private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id
    private Long id;
    /**
     * 编辑副本ID
     */
    private Long editCopyId;
    /**
     * 广告块代码
     */
    private String code;
    /**
     * 广告块类型
     * AdvertBlockTypeEnum
     */
    private Integer blockType;
    /**
     * 标题
     */
    private String title;
    /**
     * 描述
     */
    private String remark;
    /**
     * 开始时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date beginTime;
    /**
     * 结束时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 版本号
     */
    private Long version;
    /**
     * 激活版本号
     */
    private Long activeVersion;
    /**
     * 发布时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date publishTime;
    /**
     * 创建时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date createdTime;
    /**
     * 更新时间
     */
    private Date updateTime;
    /**
     * 播放时间
     */
    private Integer playTime;
    /**
     * 展示限制数量
     */
    private Integer showLimit;
}


