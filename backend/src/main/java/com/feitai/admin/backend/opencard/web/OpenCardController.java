/**
 * @author
 * @version 1.0
 * @desc 用户持有对应信用产品记录
 * @since 2018-08-06 09:28:35
 */

package com.feitai.admin.backend.opencard.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.feitai.admin.backend.auth.service.AuthDataService;
import com.feitai.admin.backend.auth.service.AuthdataLinkfaceLivenessIdnumberVerificationService;
import com.feitai.admin.backend.auth.service.AuthdataLinkfaceLivenessSelfieVerificationService;
import com.feitai.admin.backend.config.service.AppConfigService;
import com.feitai.admin.backend.customer.service.*;
import com.feitai.admin.backend.opencard.entity.CardMore;
import com.feitai.admin.backend.opencard.service.CardService;
import com.feitai.admin.backend.opencard.service.TongDunDataService;
import com.feitai.admin.backend.properties.MapProperties;
import com.feitai.admin.backend.customer.service.PhotoService;
import com.feitai.admin.core.annotation.LogAnnotation;
import com.feitai.admin.core.service.*;
import com.feitai.admin.core.vo.ListItem;
import com.feitai.admin.core.web.BaseListableController;
import com.feitai.admin.core.web.PageBulider;
import com.feitai.jieya.server.dao.attach.model.PhotoAttach;
import com.feitai.jieya.server.dao.authdata.model.BaseAuthData;
import com.feitai.jieya.server.dao.base.constant.CardStatus;
import com.feitai.jieya.server.dao.callback.model.linkface.LinkfaceLivenessIdNumberVerifcation;
import com.feitai.jieya.server.dao.callback.model.linkface.LinkfaceLivenessSelfieVerification;
import com.feitai.jieya.server.dao.data.model.*;
import com.feitai.jieya.server.dao.product.model.Product;
import com.feitai.jieya.server.dao.user.model.User;
import com.feitai.utils.CollectionUtils;
import com.feitai.utils.Desensitization;
import com.feitai.utils.StringUtils;
import com.feitai.utils.datetime.DateTimeStyle;
import com.feitai.utils.datetime.DateUtils;
import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletRequest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


@Controller
@RequestMapping(value = "/backend/opencard")
@Slf4j
public class OpenCardController extends BaseListableController<CardMore> {

    @Autowired
    private CardService cardService;

    @Autowired
    private AppConfigService appConfigService;

    @Autowired
    private AuthDataService authDataService;

    @Autowired
    private IdCardService idcardService;

    @Autowired
    private UserService userService;

    @Autowired
    private PersonService personService;

    @Autowired
    private WorkService workService;

    @Autowired
    private ContactService contactService;

    @Autowired
    private AreaService areaService;

    @Autowired
    private AuthdataLinkfaceLivenessIdnumberVerificationService authdataLinkfaceLivenessIdnumberVerificationService;

    @Autowired
    private AuthdataLinkfaceLivenessSelfieVerificationService authdataLinkfaceLivenessSelfieVerificationService;

    @Autowired
    private MapProperties mapProperties;

    @Autowired
    private PhotoService photoService;
    
    @Autowired
    private TongDunDataService tongDunDataService;


    @RequestMapping("/index")
    public String index() {
        return "/backend/opencard/index";
    }


    @RequiresPermissions("/backend/opencard:list")
    @RequestMapping(value = "list")
    @ResponseBody
    public Object listPage(ServletRequest request) {
        //根据request获取page
        int pageNo = PageBulider.getPageNo(request);
        int pageSize = PageBulider.getPageSize(request);
        Page<CardMore> cardMorePage = list(getSql(request, getSelectMultiTable()), pageNo, pageSize, getCountSqls(request), SelectMultiTable.COUNT_ALIAS);
        List<CardMore> cardMoreList = cardMorePage.getContent();
        List<JSONObject> resultList = new ArrayList();
        //遍历page中内容，修改或添加非数据库的自定义字段
        for (CardMore cardMore : cardMoreList) {
            if (cardMore.getEnable()) {
                JSONObject json = (JSONObject) JSON.toJSON(cardMore);
                if (cardMore.getStatus().getValue() > CardStatus.REJECT_BOUNDARY) {
                    json.put("cardStatusName", "<span style='color:#FF4500'>" + mapProperties.getCardStatus(cardMore.getStatus()) + "</span>");
                } else {
                    json.put("cardStatusName", "<span style='color:#32CD32'>" + mapProperties.getCardStatus(cardMore.getStatus()) + "</span>");
                }

                List<String> authList = authDataService.convertBaseAuthDataListToString(authDataService.getAuthValueString(cardMore.getId()));
                if (!CollectionUtils.isEmpty(authList)) {
                    json.put("auths", Joiner.on("<br/>").join(authList));
                } else {
                    json.put("auths", "无");
                }
                resultList.add(json);
            }
        }
        return switchContent(cardMorePage, resultList);
    }


    /***
     * 获取详细页面
     * @param id
     * @return
     */
    @RequiresUser
    @RequestMapping(value = "detail/{id}", method = RequestMethod.GET)
    public ModelAndView detail(@PathVariable("id") Long id) {
        ModelAndView model = new ModelAndView("/backend/opencard/detail");
        //授信信息
        CardMore card = cardService.findOne(id);
        if (card != null) {
            model.addObject("cardStatus", mapProperties.getCardStatus(card.getStatus()));
            if (StringUtils.isNotBlank(card.getRejectReason())) {
                model.addObject("rejectReason", mapProperties.getValveReject(card.getRejectReason()));
            }
        }
        // 用户信息
        User user = userService.findOne(card.getUserId());
        if (user != null) {
            model.addObject("user", user);
            model.addObject("userAuth", mapProperties.getUserAuth(user.getAuthStatus()));
            model.addObject("hyPhone", Desensitization.phone(user.getPhone()));
        }

        // 征信项
        List<BaseAuthData> baseAuthDataList = authDataService.getAuthValueString(card.getId());
        if (!CollectionUtils.isEmpty(baseAuthDataList)) {
            List<String> authList = authDataService.convertBaseAuthDataListToString(baseAuthDataList);
            model.addObject("authsList", baseAuthDataList);
            model.addObject("auths", Joiner.on("<br/>").join(authList));
        } else {
            model.addObject("auths", '无');
        }

        //获取对应地址
        List<JSONObject> areaList = new ArrayList<>();
        LocationData authArea = areaService.findByCardIdInAuth(id);
        if (authArea != null) {
            JSONObject jsonObject = (JSONObject) JSON.toJSON(authArea);
            jsonObject.put("segmentName", mapProperties.getSegment(authArea.getSegment()));
            areaList.add(jsonObject);
        }
        LocationData openCardArea = areaService.findByCardIdInOpenCard(id);
        if (openCardArea != null) {
            JSONObject jsonObject = (JSONObject) JSON.toJSON(openCardArea);
            jsonObject.put("segmentName", mapProperties.getSegment(openCardArea.getSegment()));
            areaList.add(jsonObject);
        }         
        // 提交审批时间
        if (!Objects.isNull(card.getSubmitTime())) {
            model.addObject("submitTime", DateUtils.format(card.getSubmitTime(), DateTimeStyle.DEFAULT_YYYY_MM_DD_HH_MM_SS));
        } else {
            model.addObject("submitTime", "无");
        }
        // 审批结果时间
        if (!Objects.isNull(card.getCreditTime())) {
            model.addObject("creditTime", DateUtils.format(card.getCreditTime(), DateTimeStyle.DEFAULT_YYYY_MM_DD_HH_MM_SS));
        } else {
            model.addObject("creditTime", "无");
        }

        model.addObject("card", card);
        model.addObject("createdTime", DateUtils.format(card.getCreatedTime(), DateTimeStyle.DEFAULT_YYYY_MM_DD_HH_MM_SS));

        model.addObject("areaList", areaList);
        //
        if(card!=null){
        TongDunData tongDunData=tongDunDataService.getTonDunData(card.getUserId(), card.getId());
        if(tongDunData!=null){
        	model.addObject("tongDunData",tongDunData);
        }
        }
        return model;
    }


    @Override
    protected DynamitSupportService<CardMore> getService() {
        return this.cardService;
    }


    private SelectMultiTable getSelectMultiTable() {
        return SelectMultiTable.builder(CardMore.class)
                .leftJoin(User.class, "user", new OnCondition[]{
                        new OnCondition(SelectMultiTable.ConnectType.AND, "userId", Operator.EQ, "id"),
                }).leftJoin(IdCardData.class, "idCard", new OnCondition[]{
                        new OnCondition(SelectMultiTable.ConnectType.AND, "userId", Operator.EQ, "userId")
                }).leftJoin(Product.class, "product", new OnCondition[]{
                        new OnCondition(SelectMultiTable.ConnectType.AND, "productId", Operator.EQ, "id")
                });
    }

    private String getCountSqls(ServletRequest request) {
        StringBuffer sbSql = new StringBuffer();
        sbSql.append(getSelectMultiTable().buildCountSqlString());
        sbSql.append(getService().buildSqlWhereCondition(bulidSearchParamsList(request), SelectMultiTable.MAIN_ALAIS));
        return sbSql.toString();
    }


    private String getOneSql(Object id) {
        String sql = SelectMultiTable.builder(CardMore.class)
                .leftJoin(User.class, "user", new OnCondition[]{
                        new OnCondition(SelectMultiTable.ConnectType.AND, "userId", Operator.EQ, "id"),
                }).leftJoin(IdCardData.class, "idCard", new OnCondition[]{
                        new OnCondition(SelectMultiTable.ConnectType.AND, "userId", Operator.EQ, "userId")
                }).leftJoin(Product.class, "product", new OnCondition[]{
                        new OnCondition(SelectMultiTable.ConnectType.AND, "productId", Operator.EQ, "id")
                }).buildSqlString() + " where maintable.id = '" + id + "' ";
        return sql;
    }

}
