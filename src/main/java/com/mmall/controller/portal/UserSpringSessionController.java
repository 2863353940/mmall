package com.mmall.controller.portal;

import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.CookieUtil;
import com.mmall.util.JsonUtil;
import com.mmall.util.RedisShardedPoolUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Created by Administrator on 2018/4/1.
 */
@Controller
@RequestMapping("/user/springsession")
public class UserSpringSessionController {

    @Autowired
    @Qualifier("iUserService")
    private IUserService iUserService;

    /**
     * 用户登录
     * @param httpServletResponse
     * @param username
     * @param password
     * @param session
     * @return
     */
    @RequestMapping(value = "login.do", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<User> login(HttpServletResponse httpServletResponse, String username, String password, HttpSession session){
        ServerResponse<User> response = iUserService.login(username, password);

        // 判断service返回的结果是否为成功状态
        if(response.isSuccess()){

            session.setAttribute(Const.CURRENT_USER, response.getData());

//            /*
//                二期变更   -------->
//                解决tomcat集群场景下共享session
//             */
//
//            // 1.为登录用户生成Token（这里使用的是sessionid，随意），并存储到Cookie
//            CookieUtil.writLoginToken(httpServletResponse, session.getId());
//
//            // 2.将用户登录信息序列化之后存放redis中，键值为Token，有效时间30分钟
//            RedisShardedPoolUtil.setEx(session.getId(), JsonUtil.obj2String(response.getData()), Const.RedisCacheExTime.REDIS_SESSION_EXTIME);
        }
        return response;
    }

    /**
     * 登出
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "logout.do", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<String> logout(HttpSession session, HttpServletRequest request, HttpServletResponse response){

        int i = 0;
        int j = 122/i;

        session.removeAttribute(Const.CURRENT_USER);

//        // 从cookie中读取token
//        String loginToken = CookieUtil.readLoginToken(request);
//        // 从cookie中删除token
//        CookieUtil.delLoginToken(request, response);
//        // 从redis中删除用户信息
//        RedisShardedPoolUtil.del(loginToken);

        return ServerResponse.createBySuccess();
    }


    /**
     * 获取用户信息
     * @param request
     * @return
     */
    @RequestMapping(value = "get_user_info.do", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<User> getUserInfo(HttpSession session, HttpServletRequest request){
        User user = (User) session.getAttribute(Const.CURRENT_USER);

//        // 1.读取cookie中的用户token
//        String loginToken = CookieUtil.readLoginToken(request);
//
//        // 2.获取redis中用户的信息
//        String jsonUser = RedisShardedPoolUtil.get(loginToken);
//
//        // 3.对用户信息的json字符串进行反序列化
//        User user = JsonUtil.string2Obj(jsonUser, User.class);

        if(user != null){
            return ServerResponse.createBySuccess(user);
        }
        return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
    }



}
