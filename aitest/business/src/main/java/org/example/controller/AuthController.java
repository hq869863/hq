//package org.example.controller;
//
//import cn.dev33.satoken.stp.StpUtil;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * @author hq
// */
//@RestController
//public class AuthController {
//    @GetMapping("/login")
//    public Map<String, Object> login(@RequestParam String user) {
//        // 这里校验数据库
//        StpUtil.login(user);
//        Map<String, Object> result = new HashMap<>();
//        result.put("message", "登录成功");
//        result.put("token", StpUtil.getTokenValue());
//        return result;
//    }
//
//    @GetMapping("/logout")
//    public String logout() {
//        StpUtil.logout();
//        return "退出成功";
//    }
//}
