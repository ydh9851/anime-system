package com.alvis.media.controller;

import com.alvis.media.base.BaseApiController;
import com.alvis.media.base.RestResponse;
import com.alvis.media.base.SystemCode;
import com.alvis.media.service.UserRegisterService;
import com.alvis.media.viewmodel.student.user.UserRegisterVM;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户注册接口。
 * POST /api/student/user/register         注册（免登录，白名单见 application.yml）
 * GET  /api/student/user/register/exists  用户名是否可注册
 * 登录由 Spring Security 的 RestLoginAuthenticationFilter 处理（POST /api/user/login）。
 */
@RestController("RegisterController")
@RequestMapping("/api/student/user/register")
@AllArgsConstructor
public class RegisterController extends BaseApiController {

    private final UserRegisterService userRegisterService;

    /**
     * 注册新用户
     */
    @PostMapping
    public RestResponse<Integer> register(@RequestBody @Valid UserRegisterVM model) {
        return RestResponse.ok(userRegisterService.register(model).getId());
    }

    /**
     * 用户名是否已被占用
     */
    @GetMapping("/exists")
    public RestResponse<Boolean> exists(@RequestParam String userName) {
        if (userRegisterService.isUserNameTaken(userName)) {
            return RestResponse.fail(SystemCode.AuthError.getCode(), "用户名已被注册");
        }
        return RestResponse.ok(Boolean.TRUE);
    }
}
