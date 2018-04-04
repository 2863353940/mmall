package com.mmall.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车VO类
 */
public class CartVo {

    private List<CartProductVo> cartProductVoList;  //购物车内的商品
    private BigDecimal cartTotalPrice;              //购物车内商品总价
    private Boolean allChecked;                     //是否已经都勾选
    private String imageHost;                       //商品图片host地址

    public List<CartProductVo> getCartProductVoList() {
        return cartProductVoList;
    }

    public void setCartProductVoList(List<CartProductVo> cartProductVoList) {
        this.cartProductVoList = cartProductVoList;
    }

    public BigDecimal getCartTotalPrice() {
        return cartTotalPrice;
    }

    public void setCartTotalPrice(BigDecimal cartTotalPrice) {
        this.cartTotalPrice = cartTotalPrice;
    }

    public Boolean getAllChecked() {
        return allChecked;
    }

    public void setAllChecked(Boolean allChecked) {
        this.allChecked = allChecked;
    }

    public String getImageHost() {
        return imageHost;
    }

    public void setImageHost(String imageHost) {
        this.imageHost = imageHost;
    }
}
