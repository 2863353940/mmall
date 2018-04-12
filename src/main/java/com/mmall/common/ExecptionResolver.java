package com.mmall.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.json.MappingJacksonJsonView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by zwy on 2018/4/13.
 * 全局异常处理
 */
@Slf4j
@Component
public class ExecptionResolver implements HandlerExceptionResolver {
    @Override
    public ModelAndView resolveException(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) {

        log.error("{} Execption", httpServletRequest.getRequestURI(), e);

        // 当使用是jackson2.x的时候使用MappingJackson2JsonView，此工程中使用的是1.9。
        ModelAndView modelAndView = new ModelAndView(new MappingJacksonJsonView()); // new MappingJacksonJsonView()
        modelAndView.addObject("status",ResponseCode.ERROR.getCode());
        modelAndView.addObject("msg", "接口异常,详情请查看服务端日志的异常信息");
        modelAndView.addObject("data", e.toString());

        return modelAndView;
    }
}
