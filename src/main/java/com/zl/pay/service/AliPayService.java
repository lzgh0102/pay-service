package com.zl.pay.service;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhaolei on 2018/1/19.
 */
@Service
public class AliPayService {

    private static final Logger logger = LoggerFactory.getLogger(AliPayService.class);

    @Value("${ali.pay.appid}")
    public String appid;

    @Value("${ali.pay.privatekey}")
    public String privatekey;

    @Value("${ali.pay.alipublickey}")
    public String aliPublickey;

    @Value("${domain.name}")
    private String domainName;

    @Autowired
    OrderInfoService orderInfoService;

    public String placeOrder(String orderNo) {
        AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do", appid, privatekey, "json", "GBK", aliPublickey, "RSA2");
        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
        String notifyUrl = domainName + "/pay/alipay/notify";
        request.setNotifyUrl(notifyUrl);

        Map<String, Object> paraMap = new HashMap<>();
        paraMap.put("out_trade_no", orderNo);
        paraMap.put("total_amount", orderInfoService.getTotalFeeByOrderNo(orderNo));
        paraMap.put("subject", "忻州立泊-立体停车");
        request.setBizContent(JSON.toJSONString(paraMap));
        AlipayTradePrecreateResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            logger.error("", e);
        }
        if (response.isSuccess()) {
            String qrCode = response.getQrCode();
            return qrCode;
        } else {
            logger.error("调用失败");
        }
        return null;
    }
}
