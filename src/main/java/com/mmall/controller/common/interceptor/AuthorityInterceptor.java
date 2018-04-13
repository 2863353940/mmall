package com.mmall.controller.common.interceptor;

import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.util.CookieUtil;
import com.mmall.util.JsonUtil;
import com.mmall.util.RedisShardedPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Administrator on 2018/4/13.
 * 权限管理拦截器，拦截后台管理员权限
 */
@Slf4j
public class AuthorityInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        log.info("preHandle");

        // 请求中Controller中的方法名
        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // 解析HandlerMethod
        String methodName = handlerMethod.getMethod().getName();    // 获取方法名
        String className = handlerMethod.getBean().getClass().getName();   // 获取类名


        /****************************************解析请求参数，具体参数key以及value是什么，打印日志*******************************************/
        StringBuffer requestParamBuffer = new StringBuffer();
        Map paramMap = request.getParameterMap();
        Iterator it = paramMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String mapKey = (String) entry.getKey();
            String mapValue = "";
            // request这个参数的map，里面的value返回的是一个String[]
            Object obj = entry.getValue();
            if (obj instanceof String[]) {
                mapValue = Arrays.toString((String[]) obj);
            }
            requestParamBuffer.append(mapKey).append("=").append(mapValue);
        }
        log.info("权限拦截器拦截到请求,className:{},methodName:{},param:{}", className, methodName, requestParamBuffer.toString());
        /*******************************************************日志打印结束**************************************************************/



        /*****************************************权限验证*******************************************************************************/

        User user = null;
        String loginToken = CookieUtil.readLoginToken(request);
        if (StringUtils.isNotEmpty(loginToken)) {
            String userJsonStr = RedisShardedPoolUtil.get(loginToken);
            user = JsonUtil.string2Obj(userJsonStr, User.class);
        }
        if (user == null || user.getRole().intValue() != Const.Role.ROLE_ADMIN) {
            response.reset();                                           // 这里要添加reset，否则报异常 getWriter() has already been called for this response.
            response.setCharacterEncoding("UTF-8");                     // 这里要设置编码，否则会乱码
            response.setContentType("application/json;charset=UTF-8");  // 这里要设置返回值的类型，因为全部是json接口。

            PrintWriter out = response.getWriter();
            if(user == null){
                if("com.mmall.controller.backend.ProductManageController".equals(className) && "richtextImgUpload".equals(methodName)){
                    Map<String, Object> resultMap = new HashMap<String, Object>();
                    resultMap.put("success",false);
                    resultMap.put("msg","请登录管理员");
                    out.print(JsonUtil.obj2String(resultMap));
                }else{
                    out.print(JsonUtil.obj2String(ServerResponse.createByErrorMessage("拦截器拦截，用户未登录")));
                }
            }else{
                if("com.mmall.controller.backend.ProductManageController".equals(className) && "richtextImgUpload".equals(methodName)){
                    Map<String, Object> resultMap = new HashMap<String, Object>();
                    resultMap.put("success",false);
                    resultMap.put("msg","无权限操作");
                    out.print(JsonUtil.obj2String(resultMap));
                }else {
                    out.print(JsonUtil.obj2String(ServerResponse.createByErrorMessage("拦截器拦截，用户无权限操作")));
                }
            }
            out.flush();
            out.close();
            return false;   // 返回false.即不会调用controller里的方法
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}
