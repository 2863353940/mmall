package com.mmall.dao;

import com.mmall.pojo.OrderItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OrderItemMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(OrderItem record);

    int insertSelective(OrderItem record);

    OrderItem selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(OrderItem record);

    int updateByPrimaryKey(OrderItem record);


    /**
     * 获取某个订单下的所有商品
     * @param orderNo
     * @param userId
     * @return
     */
    List<OrderItem> getByOrderNoUserId(@Param("orderNo") Long orderNo, @Param("userId") Integer userId);


    /**
     * 批量插入订单商品
     * @param orderItemList
     */
    void batchInsert(@Param("orderItemList") List<OrderItem> orderItemList);


    /**
     * 根据订单号查询订单商品信息(后台)
     * @param orderNo
     * @return
     */
    List<OrderItem> getByOrderNo(@Param("orderNo")Long orderNo);


}