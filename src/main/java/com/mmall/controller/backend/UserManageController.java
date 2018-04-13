package com.mmall.controller.backend;

import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.CookieUtil;
import com.mmall.util.JsonUtil;
import com.mmall.util.RedisShardedPoolUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Created by Administrator on 2018/4/2.
 */
@Controller
@RequestMapping("/manage/user")
public class UserManageController {

    @Autowired
    private IUserService iUserService;

    /**
     * 后台用户登录
     * @param username
     * @param password
     * @param session
     * @return
     */
    @RequestMapping(value = "login.do", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<User> login(String username, String password, HttpSession session, HttpServletResponse httpServletResponse){
        ServerResponse<User> response = iUserService.login(username, password);
        if(response.isSuccess()){
            User user = response.getData();
            //判断登录用户是否为管理员用户
            if(user.getRole() == Const.Role.ROLE_ADMIN){
                user.setPassword(StringUtils.EMPTY);
                // session.setAttribute(Const.CURRENT_USER, user);

                // 新增redis共享cookie，session的方式
                CookieUtil.writLoginToken(httpServletResponse, session.getId());
                RedisShardedPoolUtil.setEx(session.getId(), JsonUtil.obj2String(user), Const.RedisCacheExTime.REDIS_SESSION_EXTIME);

                return response;
            }else{
                return ServerResponse.createByErrorMessage("不是管理员用户，无法登陆");
            }
        }
        return response;
    }




























}
