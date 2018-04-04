package com.mmall.dao;

import com.mmall.pojo.Cart;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CartMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Cart record);

    int insertSelective(Cart record);

    Cart selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Cart record);

    int updateByPrimaryKey(Cart record);

    /**
     * 根据用户id和商品id查询
     * @param userId
     * @param productId
     * @return
     */
    Cart selectCartByUserIdProductId(@Param("userId") Integer userId, @Param("productId")Integer productId);

    /**
     * 查询用户在购物车中的所有商品
     * @param userId
     * @return
     */
    List<Cart> selectCartByUserId(Integer userId);

    /**
     * 查询用户购物车中未选中商品的数量
     * @param userId
     * @return
     */
    int selectCartProductCheckedStatusByUserId(Integer userId);

    /**
     * 根据用户删除购物车下商品
     * @param userId
     * @param productIdList
     * @return
     */
    int deleteByUserIdProductIds(@Param("userId") Integer userId,@Param("productIdList")List<String> productIdList);

    /**
     * 全选、全反选、单选、单反选
     * @param userId
     * @param productId
     * @param checked
     * @return
     */
    int checkedOrUncheckedProduct(@Param("userId") Integer userId,@Param("productId")Integer productId,@Param("checked") Integer checked);

    /**
     * 查询购物车商品总数
     * @param userId
     * @return
     */
    int selectCartProductCount(@Param("userId") Integer userId);

    /**
     * 查询购物车选中的商品集合
     * @param userId
     * @return
     */
    List<Cart> selectCheckedCartByUserId(Integer userId);




















}