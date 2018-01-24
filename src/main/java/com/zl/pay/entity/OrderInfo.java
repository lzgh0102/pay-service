package com.zl.pay.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Created by zhaolei on 2018/1/13.
 */
@Entity
public class OrderInfo {
    @Id
    private String orderNo;
    private String tradeOrderNo;
    private String totalFee;
    private String payType;
    private String payStatus;
    private String payAccount;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getTradeOrderNo() {
        return tradeOrderNo;
    }

    public String getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(String totalFee) {
        this.totalFee = totalFee;
    }

    public void setTradeOrderNo(String tradeOrderNo) {
        this.tradeOrderNo = tradeOrderNo;
    }

    public String getPayType() {
        return payType;
    }

    public void setPayType(String payType) {
        this.payType = payType;
    }

    public String getPayStatus() {
        return payStatus;
    }

    public void setPayStatus(String payStatus) {
        this.payStatus = payStatus;
    }

    public String getPayAccount() {
        return payAccount;
    }

    public void setPayAccount(String payAccount) {
        this.payAccount = payAccount;
    }
}
