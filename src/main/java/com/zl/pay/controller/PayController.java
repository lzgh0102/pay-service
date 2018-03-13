package com.zl.pay.controller;

import com.zl.pay.dao.OrderInfoDao;
import com.zl.pay.domain.PayStauts;
import com.zl.pay.entity.OrderInfo;
import com.zl.pay.service.AliPayService;
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

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
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

    @Value("${domain.name}")
    private String domainName;

    private static final Logger logger = LoggerFactory.getLogger(PayController.class);

    private static final String PAY_TYPE_ALI = "ali";
    private static final String PAY_TYPE_WECHAT = "wechat";

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
                orderInfo.setPayStatus(PayStauts.NOT_PAY);
                String totalFee = AmountUtils.changeF2Y(totalAmount);
                orderInfo.setTotalFee(totalFee);
                orderInfoDao.save(orderInfo);

                // 返回二维码连接
                result.put("Ha-status", "success");
                String qrCodeAddress = domainName + "/pay/place-order?orderNo=" + orderNo;
                result.put("Ha-QRCodeAddress", qrCodeAddress);
                return result;
            } else if ("query".equals(type)) {

                OrderInfo order = orderInfoDao.findByOrderNo(orderNo);
                String payType = order.getPayType();
                String custome = order.getPayAccount();
                String payStatus = order.getPayStatus();

                String status = "false";
                if (PayStauts.PAID.equals(payStatus)) {
                    status = "success";
                } else if (PayStauts.PAYING.equals(payStatus)) {
                    status = "paying";
                }

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
        OrderInfo orderInfo = orderInfoDao.findByOrderNo(orderNo);
        String payStatus = orderInfo.getPayStatus();
        if (PayStauts.NOT_PAY.equals(payStatus)) {
            orderInfo.setPayStatus(PayStauts.PAYING);
            orderInfoDao.save(orderInfo);
        }

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
        String timeStamp = System.currentTimeMillis() / 1000 + "";
        String nonceStr = WXPayUtil.generateNonceStr();
        String packages = "prepay_id=" + prepayId;

        Map paramMap = new HashMap();
        paramMap.put("appId", appid);
        paramMap.put("timeStamp", timeStamp);
        paramMap.put("nonceStr", nonceStr);
        paramMap.put("package", packages);
        paramMap.put("signType", "MD5");
        try {
            String paySign = WXPayUtil.generateSignature(paramMap, weixinPayService.mchKey);
            mv.addObject("appId", appid);
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
            orderInfo.setPayStatus(PayStauts.PAID);
            orderInfo.setPayType(PAY_TYPE_ALI);
            orderInfo.setPayAccount(payAccount);
            orderInfoDao.save(orderInfo);
        }
        return "success";
    }

    @RequestMapping(value = "/weixin/notify")
    public void wechatPayNotify(HttpServletRequest request, HttpServletResponse response) {
        ServletInputStream instream;
        StringBuffer sb = new StringBuffer();
        try {
            instream = request.getInputStream();
            int len;
            byte[] buffer = new byte[1024];
            while ((len = instream.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, len));
            }
            instream.close();
        } catch (IOException e) {
            logger.error("", e);
        }

        try {
            Map requestMap = WXPayUtil.xmlToMap(sb.toString());

            String outTradeNo = request.getParameter("out_trade_no");
            String payAccount = request.getParameter("out_trade_no");

            OrderInfo orderInfo = orderInfoDao.findByOrderNo(outTradeNo);
            orderInfo.setOrderNo(outTradeNo);
            orderInfo.setPayStatus(PayStauts.PAID);
            orderInfo.setPayType(PAY_TYPE_WECHAT);
            orderInfo.setPayAccount(payAccount);
            orderInfoDao.save(orderInfo);

        } catch (Exception e) {
            logger.error("", e);
        }

        // 返回信息，防止微信重复发送报文
        String result = "<xml>"
                + "<return_code><![CDATA[SUCCESS]]></return_code>"
                + "<return_msg><![CDATA[OK]]></return_msg>"
                + "</xml>";
        PrintWriter out = null;
        try {
            out = new PrintWriter(response.getOutputStream());
        } catch (IOException e) {
            logger.error("", e);
        }
        out.print(result);
        out.flush();
        out.close();
    }
}