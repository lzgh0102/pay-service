package com.zl.pay.service;

import com.zl.pay.dao.OrderInfoDao;
import com.zl.pay.entity.OrderInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by zhaolei on 2018/1/15.
 */
@Service
public class OrderInfoService {
    @Autowired
    OrderInfoDao orderInfoDao;

    public Boolean isPaid(String orderNo) {
        OrderInfo orderInfo = orderInfoDao.findByOrderNo(orderNo);
        if (orderInfo == null) {
            return false;
        }
        String payStatus = orderInfo.getPayStatus();
        if ("Y".equals(payStatus)) {
            return true;
        }
        return false;
    }

    public String getTotalFeeByOrderNo(String orderNo) {
        OrderInfo orderInfo = orderInfoDao.findByOrderNo(orderNo);
        return orderInfo.getTotalFee();
    }
}
