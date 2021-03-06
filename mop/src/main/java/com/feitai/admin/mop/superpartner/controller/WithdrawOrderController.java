package com.feitai.admin.mop.superpartner.controller;


import com.alibaba.fastjson.JSON;
import com.feitai.admin.core.service.DynamitSupportService;
import com.feitai.admin.core.service.Page;
import com.feitai.admin.core.vo.AjaxResult;
import com.feitai.admin.core.web.BaseListableController;
import com.feitai.admin.mop.superpartner.dao.entity.OrderReceiverInfo;
import com.feitai.admin.mop.superpartner.dao.entity.WithdrawOrder;
import com.feitai.admin.mop.superpartner.request.WithdrawOrderQueryRequest;
import com.feitai.admin.mop.superpartner.request.WithdrawOrderUpdateRequest;
import com.feitai.admin.mop.superpartner.service.AdminWithdrawOrderService;
import com.feitai.admin.mop.superpartner.vo.OrderDetailInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * @Author qiuyunlong
 */
@Slf4j
@Controller
@RequestMapping("/mop/partner/withdrawOrder")
public class WithdrawOrderController extends BaseListableController<WithdrawOrder>{

    @InitBinder
    public void initDate(WebDataBinder webDataBinder){
        webDataBinder.addCustomFormatter(new DateFormatter("yyyy-MM-dd HH:mm:ss"));
        webDataBinder.addCustomFormatter(new DateFormatter("yyyy-MM-dd"));
    }

    @Autowired
    private AdminWithdrawOrderService adminWithdrawOrderService;

    @RequiresPermissions("/mop/partner/withdrawOrder:list")
    @RequestMapping()
    public String index() {
        return "/mop/withdrawOrder/index";
    }

    @RequiresPermissions("/mop/partner/withdrawOrder:list")
    @RequestMapping("list")
    @ResponseBody
    public Object listWithdrawOrder(@ModelAttribute WithdrawOrderQueryRequest queryRequest) {
        List<WithdrawOrder> withdrawOrders = adminWithdrawOrderService.listWithdrawOrder(
                queryRequest.getUserId(),
                queryRequest.getStatus(),
                queryRequest.getOrderId(),
                queryRequest.getStartTime(),
                queryRequest.getEndTime());
        Page<WithdrawOrder> withdrawOrderPages = buildPageByExemple(withdrawOrders, queryRequest.getPageIndex(), queryRequest.getLimit());
        return withdrawOrderPages;
    }

    //@RequiresPermissions("/mop/partner/withdraywOrder:list")
    @RequestMapping("detail")
    @ResponseBody
    public Object getWithdrawOrderDetail(@ModelAttribute WithdrawOrderQueryRequest queryRequest) {
        OrderReceiverInfo orderReceiverInfo = adminWithdrawOrderService.getOrderReceiverInfo(queryRequest.getOrderId());
        OrderDetailInfo orderDetailInfo = new OrderDetailInfo();
        BeanUtils.copyProperties(orderReceiverInfo, orderDetailInfo);
        orderDetailInfo.setIdCardDetail(JSON.parseObject(orderReceiverInfo.getIdCardDetail()));
        return orderDetailInfo;
    }

    @RequiresPermissions("/mop/partner/withdrawOrder:update")
    @RequestMapping("status/update")
    @ResponseBody
    public Object updateWithdrawOrderStatus(@ModelAttribute WithdrawOrderUpdateRequest updateRequest) throws IOException {
        adminWithdrawOrderService.updateWithdrawOrderStatus(
                updateRequest.getUserId(),
                updateRequest.getOrderId(),
                updateRequest.getStatus(),
                SecurityUtils.getSubject().getPrincipal().toString(),
                updateRequest.getRemark(),
                updateRequest.getType());
        return BaseListableController.successResult;
    }

    @RequiresPermissions("/mop/partner/withdrawOrder:list")
    @RequestMapping("output")
    public String downloadWithdrawOrderInfo(@ModelAttribute WithdrawOrderQueryRequest queryRequest, HttpServletResponse res, HttpServletRequest request) throws IOException {
        log.info(" session id is " + request.getSession().getId());
        File file = adminWithdrawOrderService.createWithdrawOrderFile(
                queryRequest.getUserId(),
                queryRequest.getStatus(),
                queryRequest.getOrderId(),
                queryRequest.getStartTime(),
                queryRequest.getEndTime(),
                request.getSession().getId());
        res.setHeader("content-type", "application/octet-stream");
        res.setContentType("application/octet-stream");
        res.setHeader("Content-Disposition", "attachment;filename=" + file.getName());
        Files.copy(file.toPath(), res.getOutputStream());
        Files.delete(file.toPath());
        adminWithdrawOrderService.removeDownloadInfo(request.getSession().getId());
        return null;
    }

    @RequiresPermissions("/mop/partner/withdrawOrder:list")
    @RequestMapping("output/count")
    @ResponseBody
    public Object countDownloadWithdrawOrderInfo(@ModelAttribute WithdrawOrderQueryRequest queryRequest) {

        boolean flag = adminWithdrawOrderService.isLargeDownloadData(
                queryRequest.getUserId(),
                queryRequest.getStatus(),
                queryRequest.getOrderId(),
                queryRequest.getStartTime(),
                queryRequest.getEndTime());
        return new AjaxResult(true, flag ? "yes" : "no");
    }

    @RequiresPermissions("/mop/partner/withdrawOrder:list")
    @RequestMapping("output/info")
    @ResponseBody
    public Object getDownloadInfo(HttpServletRequest request) {
        log.info(" session id is " + request.getSession().getId());
        return adminWithdrawOrderService.getDownloadInfo(request.getSession().getId());
    }

    @Override
    protected DynamitSupportService<WithdrawOrder> getService() {
        return null;
    }
}
