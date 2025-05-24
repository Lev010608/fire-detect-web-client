package com.example.exception;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.example.common.Result;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice(basePackages="com.example.controller")
public class GlobalExceptionHandler {

    private static final Log log = LogFactory.get();


    //统一异常处理@ExceptionHandler,主要用于Exception
    @ExceptionHandler(Exception.class)
    @ResponseBody//返回json串
    public Result error(HttpServletRequest request, Exception e){
        log.error("异常信息：",e);
        return Result.error();
    }

    @ExceptionHandler(CustomException.class)
    @ResponseBody//返回json串
    public Result customError(HttpServletRequest request, CustomException e){
        return Result.error(e.getCode(), e.getMsg());
    }

    //处理客户端中断连接异常（忽略，不记录错误日志）
    @ExceptionHandler(ClientAbortException.class)
    @ResponseBody
    public void handleClientAbortException(HttpServletRequest request, ClientAbortException e) {
        // 客户端中断连接是正常现象，不需要记录错误日志
        log.debug("客户端中断连接: " + request.getRequestURI());
        // 不返回任何内容，因为连接已断开
    }
}
