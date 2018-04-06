package com.mmall.dao;

import com.mmall.pojo.Order;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OrderMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Order record);

    int insertSelective(Order record);

    Order selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Order record);

    int updateByPrimaryKey(Order record);

    /**
     * 根据订单号和用户id查询订单
     * @param userId
     * @param orderNo
     * @return
     */
    Order selectByUserIdAndOrderNo(@Param("userId") Integer userId, @Param("orderNo") Long orderNo);

    /**
     * 根据订单号查询订单
     * @param orderNo
     * @return
     */
    Order selectByOrderNo(Long orderNo);


    /**
     * 查询用户的所有订单
     * @param userId
     * @return
     */
    List<Order> selectByUserId(Integer userId);








    /**********************************************************后台接口************************************************************************/

    /**
     * 后台查询所有订单
     * @return
     */
    List<Order> selectAllOrder();


    /*************************************************************OVER************************************************************************/
































}