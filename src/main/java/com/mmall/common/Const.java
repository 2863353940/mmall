package com.mmall.common;

/**
 * Created by Administrator on 2018/4/1.
 */
public class Const {

    //当前用户
    public static final String CURRENT_USER = "currentUser";

    public static final String EMAIL = "email";
    public static final String USERNAME = "username";

    //用一个接口类来把常量进行分组（没有枚举类那么重）
    public interface Role{
        int ROLE_CUSTOMER = 0;  //普通用户
        int ROLE_ADMIN = 1;     //管理员
    }

}
