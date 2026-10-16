package com.alvis.media.service;

import com.alvis.media.domain.User;
import com.alvis.media.viewmodel.student.user.UserRegisterVM;

/**
 * 用户注册Service。
 * 单独抽出而非并入 UserService：注册需要 AuthenticationService 加密密码，
 * 而 AuthenticationServiceImpl 依赖 UserService，并入会形成循环依赖。
 */
public interface UserRegisterService {

    /**
     * 注册一个新用户
     *
     * @param vm 注册入参（已完成 Bean Validation 校验）
     * @return 注册成功后的用户（含回填主键，密码字段脱敏为 null）
     */
    User register(UserRegisterVM vm);

    /**
     * 用户名是否已被占用
     *
     * @param userName 用户名
     * @return true 已存在
     */
    boolean isUserNameTaken(String userName);
}