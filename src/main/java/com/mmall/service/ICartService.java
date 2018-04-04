package com.mmall.service;

import com.mmall.common.ServerResponse;
import com.mmall.vo.CartVo;

/**
 * Created by Administrator on 2018/4/4.
 */
public interface ICartService {

    /**
     * 查询购物车所有商品
     * @param userId
     * @return
     */
    ServerResponse<CartVo> list (Integer userId);

    /**
     * 增加商品至购物车并返回购物车中所有商品的信息
     * @param userId    用户id
     * @param productId 商品id
     * @param count     数量
     * @return
     */
    ServerResponse<CartVo> add(Integer userId, Integer productId, Integer count);


    /**
     * 购物车更新方法(点击购物车中的加减号)
     * @param userId
     * @param productId
     * @param count
     * @return
     */
    ServerResponse<CartVo> update(Integer userId,Integer productId,Integer count);

    /**
     * 根据用户删除购物车下商品
     * @param userId
     * @param productIds    多个商品id逗号分隔
     * @return
     */
    ServerResponse<CartVo> deleteProduct(Integer userId,String productIds);

    /**
     * 全选、全反选、单选、单反选
     * @param userId
     * @param productId
     * @param checked
     * @return
     */
    ServerResponse<CartVo> selectOrUnSelect (Integer userId,Integer productId,Integer checked);


    /**
     * 查询购物车商品数量
     * @param userId
     * @return
     */
    ServerResponse<Integer> getCartProductCount(Integer userId);





}
