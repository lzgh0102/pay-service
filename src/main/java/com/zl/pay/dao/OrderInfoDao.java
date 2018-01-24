package com.zl.pay.dao;

import com.zl.pay.entity.OrderInfo;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by zhaolei on 2018/1/13.
 */
public interface OrderInfoDao extends CrudRepository<OrderInfo, String> {
    OrderInfo findByOrderNo(String orderNo);
}
