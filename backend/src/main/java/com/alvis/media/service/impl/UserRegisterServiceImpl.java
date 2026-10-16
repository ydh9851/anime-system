package com.alvis.media.service.impl;

import com.alvis.media.domain.User;
import com.alvis.media.domain.UserEventLog;
import com.alvis.media.domain.enums.RoleEnum;
import com.alvis.media.domain.enums.UserStatusEnum;
import com.alvis.media.repository.UserEventLogMapper;
import com.alvis.media.repository.UserMapper;
import com.alvis.media.service.AuthenticationService;
import com.alvis.media.service.UserRegisterService;
import com.alvis.media.viewmodel.student.user.UserRegisterVM;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;

@Service
public class UserRegisterServiceImpl implements UserRegisterService {

    private final UserMapper userMapper;
    private final UserEventLogMapper userEventLogMapper;
    private final AuthenticationService authenticationService;

    @Autowired
    public UserRegisterServiceImpl(UserMapper userMapper,
                                   UserEventLogMapper userEventLogMapper,
                                   AuthenticationService authenticationService) {
        this.userMapper = userMapper;
        this.userEventLogMapper = userEventLogMapper;
        this.authenticationService = authenticationService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User register(UserRegisterVM vm) {
        if (isUserNameTaken(vm.getUserName())) {
            throw new IllegalArgumentException("用户名已被注册");
        }
        Date now = new Date();
        User user = new User();
        user.setUserUuid(UUID.randomUUID().toString());
        user.setUserName(vm.getUserName().trim());
        user.setPassword(authenticationService.pwdEncode(vm.getPassword()));
        if (StringUtils.isNotBlank(vm.getRealName())) {
            user.setRealName(vm.getRealName().trim());
        }
        if (StringUtils.isNotBlank(vm.getPhone())) {
            user.setPhone(vm.getPhone().trim());
        }
        user.setRole(vm.getUserLevel() == null ? RoleEnum.VIP.getCode() : vm.getUserLevel());
        user.setStatus(UserStatusEnum.Enable.getCode());
        user.setDeleted(false);
        user.setCreateTime(now);
        user.setLastActiveTime(now);
        userMapper.insertSelective(user);
        writeRegisterLog(user, now);
        user.setPassword(null);
        return user;
    }

    @Override
    public boolean isUserNameTaken(String userName) {
        if (StringUtils.isBlank(userName)) {
            return false;
        }
        return userMapper.getUserByUserName(userName.trim()) != null;
    }

    private void writeRegisterLog(User user, Date now) {
        UserEventLog eventLog = new UserEventLog();
        eventLog.setUserId(user.getId());
        eventLog.setUserName(user.getUserName());
        eventLog.setRealName(user.getRealName());
        eventLog.setContent(user.getUserName() + " 注册了动漫数据分析与推荐系统");
        eventLog.setCreateTime(now);
        userEventLogMapper.insertSelective(eventLog);
    }
}