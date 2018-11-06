package com.feitai.admin.backend.creditdata.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.feitai.admin.backend.creditdata.service.CreditDataService;
import com.feitai.utils.Desensitization;
import com.feitai.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletRequest;
import java.util.Map;

/**
 * detail:渲染模板获取数据
 * author:longhaoteng
 * date:2018/8/28
 */
@Controller
@RequestMapping(value = "/backend/data/acquire")
public class CommonController {

    @Autowired
    private CreditDataService creditDataService;


    @Autowired
    private com.feitai.admin.backend.data.service.AuthdataMoxieService AuthdataMoxieService;

    private static final String NULL_STRING = "nulldata";

    /***
     * 获取贷后邦/运营商/征信信息的数据
     */
    @RequestMapping(value = "getData",method = RequestMethod.POST)
    @ResponseBody
    public Object getData(ServletRequest request) {
        String cardId = request.getParameter("cardId");
        String type = request.getParameter("type");
        String source = "";
        switch (type) {
            case "bond"://贷后邦
                source = "DAIHOUBANG";
                break;
            case "credit"://征信
                source = "SUANHUA";
                break;
        }
        String json = null;
        if ("operator".equals(type)) {
           String message = AuthdataMoxieService.getMessageByCardId(cardId);
            JSONObject object = new JSONObject();
            object.put("message",message);
            return object;
        } else {
            json = creditDataService.findByCardIdAndSource(Long.parseLong(cardId), source);

            if (type.equals("bond") && !json.equals(NULL_STRING)) {
                Map map = JSON.parseObject(json, Map.class);
                //脱敏手机
                String user_phone = (String) map.get("user_phone");
                String hyPhone = Desensitization.phone(user_phone);
                map.replace("user_phone", user_phone, hyPhone);
                //脱敏身份证
                String user_idcard = (String) map.get("user_idcard");
                String hyIdcard = Desensitization.idCard(user_idcard);
                map.replace("user_idcard", user_idcard, hyIdcard);
                return map;
            }
        }
        if(json!=null&&StringUtils.isNotBlank(json)&&!json.equals(NULL_STRING)){
            return JSON.parseObject(json, Map.class);
        }else{
            return "";
        }
    }
}