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
@RequestMapping("/user")
public class UserController {

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

            //session.setAttribute(Const.CURRENT_USER, response.getData());

            /*
                二期变更   -------->
                解决tomcat集群场景下共享session
             */

            // 1.为登录用户生成Token（这里使用的是sessionid，随意），并存储到Cookie
            CookieUtil.writLoginToken(httpServletResponse, session.getId());

            // 2.将用户登录信息序列化之后存放redis中，键值为Token，有效时间30分钟
            RedisShardedPoolUtil.setEx(session.getId(), JsonUtil.obj2String(response.getData()), Const.RedisCacheExTime.REDIS_SESSION_EXTIME);
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
    public ServerResponse<String> logout(HttpServletRequest request, HttpServletResponse response){
        //session.removeAttribute(Const.CURRENT_USER);

        // 从cookie中读取token
        String loginToken = CookieUtil.readLoginToken(request);
        // 从cookie中删除token
        CookieUtil.delLoginToken(request, response);
        // 从redis中删除用户信息
        RedisShardedPoolUtil.del(loginToken);

        return ServerResponse.createBySuccess();
    }

    /**
     * 注册
     * @param user
     * @return
     */
    @RequestMapping(value = "register.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> register(User user){
        return iUserService.register(user);
    }

    /**
     * 检验用户名或者Email
     * @param str   要校验的值
     * @param type  要校验的类型
     * @return
     */
    @RequestMapping(value = "check_valid.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> checkValid(String str, String type){
        return iUserService.checkValid(str, type);
    }

    /**
     * 获取用户信息
     * @param request
     * @return
     */
    @RequestMapping(value = "get_user_info.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> getUserInfo(HttpServletRequest request){
        // User user = (User) session.getAttribute(Const.CURRENT_USER);

        // 1.读取cookie中的用户token
        String loginToken = CookieUtil.readLoginToken(request);

        // 2.获取redis中用户的信息
        String jsonUser = RedisShardedPoolUtil.get(loginToken);

        // 3.对用户信息的json字符串进行反序列化
        User user = JsonUtil.string2Obj(jsonUser, User.class);

        if(user != null){
            return ServerResponse.createBySuccess(user);
        }
        return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
    }

    /**
     * 忘记密码获取问题
     * @param username
     * @return
     */
    @RequestMapping(value = "forget_get_question.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetGetQuestion(String username){
        return iUserService.selectQuestion(username);
    }

    /**
     * 忘记密码回答问题，如果正确则将Token放入本地缓存中
     * @param username
     * @param question
     * @param answer
     * @return
     */
    @RequestMapping(value = "forget_check_answer.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetCheckAnswer(String username, String question, String answer){
        return iUserService.checkAnswer(username, question, answer);
    }

    /**
     * 忘记密码修改密码
     * @param username
     * @param passwordNew
     * @param forgetToken
     * @return
     */
    @RequestMapping(value = "forget_rest_password.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetRestPassword(String username, String passwordNew, String forgetToken){
        return iUserService.forgetRestPassword(username, passwordNew, forgetToken);
    }


    /**
     * 登录状态修改密码
     * @param session
     * @param passwordOld
     * @param passwordNew
     * @return
     */
    @RequestMapping(value = "reset_password.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> resetPassword(HttpSession session, String passwordOld, String passwordNew){
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorMessage("用户未登录");
        }
        return iUserService.resetPassword(passwordOld, passwordNew, user);
    }

    /**
     * 登录状态下更新用户信息
     * @param session
     * @param user
     * @return
     */
    @RequestMapping(value = "update_information.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> update_information(HttpSession session, User user){
        User currentUser = (User) session.getAttribute(Const.CURRENT_USER);
        if(currentUser == null){
            return ServerResponse.createByErrorMessage("用户未登录");
        }
        user.setId(currentUser.getId());
        user.setUsername(currentUser.getUsername());
        ServerResponse<User> response = iUserService.updateInformation(user);
        if(response.isSuccess()){
            session.setAttribute(Const.CURRENT_USER, response.getData());
        }
        return response;
    }

    /**
     * 获取当前用户个人信息，如果没有登录提示需要登录
     * @param session
     * @return
     */
    @RequestMapping(value = "get_information.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> get_information(HttpSession session){
        User currentUser = (User) session.getAttribute(Const.CURRENT_USER);
        if(currentUser == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登录，需要强制登录status=10");
        }
        return iUserService.getInformation(currentUser.getId());
    }













}
