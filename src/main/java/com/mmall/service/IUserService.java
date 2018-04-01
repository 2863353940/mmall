package com.mmall.service;

import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;

/**
 * Created by Administrator on 2018/4/1.
 */
public interface IUserService {

    /**
     * 登录验证
     * @param username
     * @param password
     * @return
     */
    ServerResponse<User> login(String username, String password);

    /**
     * 注册
     * @param user
     * @return
     */
    ServerResponse<String> register(User user);

    /**
     * 邮箱或用户名验证
     * @param str
     * @param type
     * @return
     */
    ServerResponse<String> checkValid(String str, String type);

}
