package com.mmall.service;

import com.github.pagehelper.PageInfo;
import com.mmall.common.ServerResponse;
import com.mmall.vo.OrderVo;

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
     * 取消未支付订单
     * @param userId
     * @param orderNo
     * @return
     */
    ServerResponse<String> cancel(Integer userId,Long orderNo);


    /**
     * 获取购物车中勾选的商品
     * @param userId
     * @return
     */
    ServerResponse getOrderCartProduct(Integer userId);


    /**
     * 查询订单详细
     * @param userId
     * @param orderNo
     * @return
     */
    ServerResponse<OrderVo> getOrderDetail(Integer userId, Long orderNo);


    /**
     * 查询订单列表
     * @param userId
     * @param pageNum
     * @param pageSize
     * @return
     */
    ServerResponse<PageInfo> getOrderList(Integer userId, int pageNum, int pageSize);





    /**********************************************************后台接口************************************************************************/

    /**
     * 后台查询列表
     * @param pageNum
     * @param pageSize
     * @return
     */
    ServerResponse<PageInfo> manageList(int pageNum,int pageSize);

    /**
     * 后台查询详细
     * @param orderNo
     * @return
     */
    ServerResponse<OrderVo> manageDetail(Long orderNo);

    /**
     * 后台订单搜索
     * @param orderNo
     * @param pageNum
     * @param pageSize
     * @return
     */
    ServerResponse<PageInfo> manageSearch(Long orderNo,int pageNum,int pageSize);

    /**
     * 后台订单发货
     * @param orderNo
     * @return
     */
    ServerResponse<String> manageSendGoods(Long orderNo);

    /*************************************************************OVER************************************************************************/










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
     * @param path      tomact下 webapp下upload文件夹    本地路径tomcat、webapps、ROOT、upload
     * @return
     */
    ServerResponse pay(Long orderNo, Integer userId, String path);


    /**
     * 支付宝回调
     * @param params
     * @return
     */
    ServerResponse aliCallback(Map<String,String> params);


    /***********************************************************支付宝支付OVER******************************************************************/









    /**
     * hour个小时以内未付款的订单，进行关闭
     * @param hour
     */
    void closeOrder(int hour);







}
