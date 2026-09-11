package org.example.config;

import cn.dev33.satoken.exception.NotLoginException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * @author hq
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 拦截 Sa-Token 的未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    public Map<String, Object> handleNotLoginException(NotLoginException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 401);
        result.put("msg", "未登录或Token已失效，请重新登录");
        result.put("data", null);
        return result;
    }
}
