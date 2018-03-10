package com.zl.pay.controller;

import com.zl.pay.dao.OrderInfoDao;
import com.zl.pay.entity.OrderInfo;
import com.zl.pay.service.AliPayService;
import com.zl.pay.service.OrderInfoService;
import com.zl.pay.service.WeiXinPayService;
import com.zl.pay.utils.AmountUtils;
import com.zl.pay.utils.WXPayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhaolei on 2018/1/12.
 */
@Controller
@RequestMapping("/pay")
public class PayController {

    @Autowired
    WeiXinPayService weixinPayService;
    @Autowired
    AliPayService aliPayService;
    @Autowired
    OrderInfoDao orderInfoDao;
    @Autowired
    OrderInfoService orderInfoService;

    @Value("${domain.name}")
    private String domainName;

    private static final Logger logger = LoggerFactory.getLogger(PayController.class);

    @RequestMapping("/order")
    @ResponseBody
    public Map<String, Object> pay(HttpServletRequest request) {

        Map result = new HashMap();

        try {
            String type = request.getParameter("Ha-Sign");
            String orderNo = request.getParameter("Ha-OutTradeNo");
            String totalAmount = request.getParameter("Ha-TotalAmount");
            String subject = request.getParameter("Ha-Subject");
            String body = request.getParameter("Ha-Body");


            if (orderNo == null || "".equals(orderNo)) {
                result.put("Ha-status", "fail:orderNo can not be null");
                return result;
            }

            if ("order".equals(type)) {
                if (totalAmount == null || "".equals(totalAmount)) {
                    result.put("Ha-status", "fail:totalAmount can not be null");
                    return result;
                }
                boolean isExist = orderInfoDao.exists(orderNo);
                if (isExist) {
                    result.put("Ha-status", "fail:orderNo is duplicated");
                    return result;
                }
                OrderInfo orderInfo = new OrderInfo();
                orderInfo.setOrderNo(orderNo);
                orderInfo.setPayStatus("N");
                String totalFee = AmountUtils.changeF2Y(totalAmount);
                orderInfo.setTotalFee(totalFee);
                orderInfoDao.save(orderInfo);

                // 返回二维码连接
                result.put("Ha-status", "success");
                String qrCodeAddress = domainName + "/pay/place-order?orderNo=" + orderNo;
                result.put("Ha-QRCodeAddress", qrCodeAddress);
                return result;
            } else if ("query".equals(type)) {
                boolean isPaid = orderInfoService.isPaid(orderNo);
                String status = "false";
                if (isPaid) {
                    status = "success";
                }
                OrderInfo order = orderInfoDao.findByOrderNo(orderNo);
                String payType = order.getPayType();
                String custome = order.getPayAccount();

                result.put("Ha-status", status);
                result.put("Ha-type", payType);
                result.put("Ha-Customer", custome);
                return result;
            }
        } catch (Exception e) {
            logger.error("", e);
        }
        return null;
    }

    @RequestMapping("/place-order")
    public ModelAndView placeOrder(HttpServletRequest request) {

        String userAgent = request.getHeader("user-agent");
        String orderNo = request.getParameter("orderNo");

        ModelAndView mv = new ModelAndView();

        if (userAgent == null) {
            // return mv;
        } else if (userAgent.contains("MicroMessenger")) {
            String oauthUrl = weixinPayService.getOauthUrl(orderNo);
            mv.setViewName("redirect:" + oauthUrl);
        } else if (userAgent.contains("AliApp")) {
            mv.setViewName("redirect:ali-order/place?orderNo=" + orderNo);
        }
        return mv;
    }

    @RequestMapping("/weixin-order/place")
    public ModelAndView palceWeixinOrder(HttpServletRequest request) {
        String code = request.getParameter("code");
        String orderNo = request.getParameter("state");
        String openId = weixinPayService.getOpenidByCode(code);
        Map<String, String> responseMap = weixinPayService.placeOrder(orderNo, openId);
        String prepayId = responseMap.get("prepay_id");

        ModelAndView mv = new ModelAndView();
        mv.setViewName("weixin-pay");
        String appid = weixinPayService.appid;
        String timeStamp = System.currentTimeMillis() + "";
        String nonceStr = WXPayUtil.generateNonceStr();
        String packages = "prepay_id=" + prepayId;

        Map paramMap = new HashMap();
        paramMap.put("appid", appid);
        paramMap.put("timeStamp", timeStamp);
        paramMap.put("nonceStr", nonceStr);
        paramMap.put("package", packages);
        try {
            String paySign = WXPayUtil.generateSignature(paramMap, weixinPayService.mchKey);
            mv.addObject("appId", weixinPayService.appid);
            mv.addObject("timeStamp", timeStamp);
            mv.addObject("nonceStr", nonceStr);
            mv.addObject("package", packages);
            mv.addObject("paySign", paySign);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mv;
    }

    @RequestMapping("/ali-order/place")
    public ModelAndView palceAliOrder(HttpServletRequest request) {
        String orderNo = request.getParameter("orderNo");
        String qrCode = aliPayService.placeOrder(orderNo);
        ModelAndView mv = new ModelAndView();
        mv.setViewName("redirect:" + qrCode);
        return mv;
    }

    @RequestMapping("/alipay/notify")
    @ResponseBody
    public String aliPayNotify(HttpServletRequest request) {
        String outTradeNo = request.getParameter("out_trade_no");
        String tradeStatus = request.getParameter("trade_status");
        if ("TRADE_SUCCESS".equals(tradeStatus)) {
            String payAccount = request.getParameter("buyer_logon_id");
            OrderInfo orderInfo = orderInfoDao.findByOrderNo(outTradeNo);
            orderInfo.setOrderNo(outTradeNo);
            orderInfo.setPayStatus("Y");
            orderInfo.setPayType("ali");
            orderInfo.setPayAccount(payAccount);
            orderInfoDao.save(orderInfo);
        }
        return "success";
    }
}
