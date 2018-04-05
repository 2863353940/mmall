package com.mmall.service;

import com.mmall.common.ServerResponse;

import java.util.Map;

/**
 * Created by Administrator on 2018/4/5.
 */
public interface IOrderService {


    /**
     * 创建未支付订单
     * @param userId
     * @param shippingId    收货地址id
     * @return
     */
    //todo 事务控制
    ServerResponse createOrder(Integer userId,Integer shippingId);


















    /**
     * 查询订单是否支付
     * @param userId
     * @param orderNo
     * @return
     */
    ServerResponse queryOrderPayStatus(Integer userId,Long orderNo);



    /***********************************************************支付宝支付******************************************************************/

    /**
     * 支付宝预下单接口(返回支付二维码)
     * @param orderNo   订单号
     * @param userId    用户id
     * @param path      tomact下 webapp下upload文件夹    本地路径tomcat\webapps\ROOT\upload
     * @return
     */
    ServerResponse pay(Long orderNo, Integer userId, String path);


    /**
     * 支付宝回调
     * @param params
     * @return
     */
    ServerResponse aliCallback(Map<String,String> params);













}
