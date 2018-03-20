package com.zl.pay.service;

import com.alibaba.fastjson.JSONObject;
import com.zl.pay.utils.AmountUtils;
import com.zl.pay.utils.HttpClientUtil;
import com.zl.pay.utils.WXPayUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhaolei on 2018/1/13.
 */
@Service
public class WeiXinPayService {

    @Value("${weixin.pay.appid}")
    public String appid;

    @Value("${weixin.pay.appsecret}")
    public String appsecret;

    @Value("${domain.name}")
    private String domainName;

    @Value("${weixin.pay.mch.id}")
    private String mchId;

    @Value("${weixin.pay.mch.key}")
    public String mchKey;

    @Autowired
    OrderInfoService orderInfoService;

    public Map<String, String> placeOrder(String orderNo, String openId) {
        Map<String, String> orderMap = new HashMap<>();
        orderMap.put("appid", appid);
        orderMap.put("mch_id", mchId);
        orderMap.put("nonce_str", WXPayUtil.generateNonceStr());
        orderMap.put("body", "忻州立泊-立体停车");
        orderMap.put("out_trade_no", orderNo);
        orderMap.put("total_fee", AmountUtils.changeY2F(orderInfoService.getTotalFeeByOrderNo(orderNo)));
        orderMap.put("spbill_create_ip", "123.12.12.123");
        orderMap.put("notify_url", domainName + "/pay/weixin/notify");
        orderMap.put("trade_type", "JSAPI");
        orderMap.put("openid", openId);
        try {
            String xmlRequest = WXPayUtil.generateSignedXml(orderMap, mchKey);
            String unifiedorderUrl = "https://api.mch.weixin.qq.com/pay/unifiedorder";
            String xmlResponse = HttpClientUtil.httpPostRequest(unifiedorderUrl, xmlRequest);
            Map<String, String> mapResponse = WXPayUtil.xmlToMap(xmlResponse);
            return mapResponse;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getOauthUrl(String orderNo) {
        String url = "https://open.weixin.qq.com/connect/oauth2/authorize?appid=APPID&redirect_uri=REDIRECT_URI&response_type=code&scope=snsapi_base&state=STATE#wechat_redirect";
        String redirectUri = domainName.concat("/pay/weixin-order/place");
        try {
            String encodedRedirectUri = URLEncoder.encode(redirectUri, "UTF-8");
            String newUrl = url.replace("APPID", appid).replace("REDIRECT_URI", encodedRedirectUri).replace("STATE", orderNo);
            return newUrl;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getOpenidByCode(String code) {
        String url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code";
        String newUrl = url.replace("APPID", appid).replace("SECRET", appsecret).replace("CODE", code);
        String result = HttpClientUtil.httpGetRequest(newUrl);
        JSONObject jsonResult = JSONObject.parseObject(result);
        String openid = jsonResult.getString("openid");
        return openid;
    }

}
