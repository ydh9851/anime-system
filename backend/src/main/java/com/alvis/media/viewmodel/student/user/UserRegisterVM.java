package com.alvis.media.viewmodel.student.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册入参。
 * 原为死代码（白名单已开 /api/student/user/register 但无对应实现），
 * 现作为注册接口的正式入参，开启 Bean Validation 校验。
 */
@Data
public class UserRegisterVM {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度须在 3~20 之间")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{2,19}$",
            message = "用户名须以字母开头，仅可包含字母、数字、下划线")
    private String userName;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度须在 6~32 之间")
    private String password;

    @Size(max = 50, message = "真实姓名过长")
    private String realName;

    @Pattern(regexp = "^$|^1[3-9][0-9]{9}$", message = "手机号格式不正确")
    private String phone;

    /** 角色：1 普通用户 / 2 VIP，默认 2（与 SecurityConfigurer 的 /api/student/** 权限一致） */
    private Integer userLevel = 2;
}
