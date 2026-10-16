package com.alvis.media.controller.admin;

import com.alvis.media.base.BaseApiController;
import com.alvis.media.base.RestResponse;
import com.alvis.media.base.SystemCode;
import com.alvis.media.domain.User;
import com.alvis.media.domain.UserEventLog;
import com.alvis.media.domain.enums.UserStatusEnum;
import com.alvis.media.domain.other.KeyValue;
import com.alvis.media.service.AuthenticationService;
import com.alvis.media.service.UserEventLogService;
import com.alvis.media.service.UserService;
import com.alvis.media.utility.DateTimeUtil;
import com.alvis.media.utility.PageInfoHelper;
import com.alvis.media.viewmodel.admin.user.UserCreateVM;
import com.alvis.media.viewmodel.admin.user.UserEventLogVM;
import com.alvis.media.viewmodel.admin.user.UserEventPageRequestVM;
import com.alvis.media.viewmodel.admin.user.UserPageRequestVM;
import com.alvis.media.viewmodel.admin.user.UserResponseVM;
import com.alvis.media.viewmodel.admin.user.UserUpdateVM;
import com.github.pagehelper.PageInfo;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 用户管理接口。
 *
 * RESTful 设计：
 *   POST   /api/admin/user                    新增用户
 *   PUT    /api/admin/user/{id}               修改指定用户
 *   PUT    /api/admin/user/profile            修改本人信息
 *   PUT    /api/admin/user/{id}/status       启用 / 禁用
 *   DELETE /api/admin/user/{id}               删除（逻辑删除 deleted=true）
 *   GET    /api/admin/user/{id}               用户详情
 *   POST   /api/admin/user/page/list          分页查询（组合条件放请求体，用 POST 更清晰）
 *   GET    /api/admin/user/names              用户名下拉（模糊查询）
 *
 * 登录 / 注册不在本类：登录由 Spring Security 的 RestLoginAuthenticationFilter 处理
 * （POST /api/user/login），注册见 RegisterController（POST /api/student/user/register）。
 */
@RestController("AdminUserController")
@RequestMapping("/api/admin/user")
@AllArgsConstructor
public class UserController extends BaseApiController {

    private final UserService userService;
    private final UserEventLogService userEventLogService;
    private final AuthenticationService authenticationService;

    /** 分页查询用户（组合条件放请求体，故用 POST） */
    @PostMapping("/page/list")
    public RestResponse<PageInfo<UserResponseVM>> pageList(@RequestBody UserPageRequestVM model) {
        PageInfo<User> pageInfo = userService.userPage(model);
        PageInfo<UserResponseVM> page = PageInfoHelper.copyMap(pageInfo, UserResponseVM::from);
        return RestResponse.ok(page);
    }

    /** 用户行为日志分页 */
    @PostMapping("/event/page/list")
    public RestResponse<PageInfo<UserEventLogVM>> eventPageList(@RequestBody UserEventPageRequestVM model) {
        PageInfo<UserEventLog> pageInfo = userEventLogService.page(model);
        PageInfo<UserEventLogVM> page = PageInfoHelper.copyMap(pageInfo, d -> {
            UserEventLogVM vm = modelMapper.map(d, UserEventLogVM.class);
            vm.setCreateTime(DateTimeUtil.dateFormat(d.getCreateTime()));
            return vm;
        });
        return RestResponse.ok(page);
    }

    /** 当前登录用户信息 */
    @GetMapping("/current")
    public RestResponse<UserResponseVM> current() {
        return RestResponse.ok(UserResponseVM.from(getCurrentUser()));
    }

    /** 用户详情 */
    @GetMapping("/{id}")
    public RestResponse<UserResponseVM> detail(@PathVariable Integer id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return RestResponse.fail(SystemCode.InnerError.getCode(), "用户不存在");
        }
        return RestResponse.ok(UserResponseVM.from(user));
    }

    /** 新增用户 */
    @PostMapping
    public RestResponse<UserResponseVM> create(@RequestBody @Valid UserCreateVM model) {
        if (userService.getUserByUserName(model.getUserName()) != null) {
            return RestResponse.fail(2, "用户已存在");
        }
        if (StringUtils.isBlank(model.getPassword())) {
            return RestResponse.fail(3, "密码不能为空");
        }
        if (StringUtils.isBlank(model.getBirthDay())) {
            model.setBirthDay(null);
        }
        User user = modelMapper.map(model, User.class);
        user.setUserUuid(UUID.randomUUID().toString());
        user.setPassword(authenticationService.pwdEncode(model.getPassword()));
        user.setCreateTime(new Date());
        user.setLastActiveTime(new Date());
        user.setDeleted(false);
        userService.insertByFilter(user);
        return RestResponse.ok(UserResponseVM.from(userService.getUserById(user.getId())));
    }

    /** 修改指定用户（密码不传表示不修改） */
    @PutMapping("/{id}")
    public RestResponse<UserResponseVM> update(@PathVariable Integer id, @RequestBody @Valid UserCreateVM model) {
        User dbUser = userService.getUserById(id);
        if (dbUser == null) {
            return RestResponse.fail(SystemCode.InnerError.getCode(), "用户不存在");
        }
        if (StringUtils.isBlank(model.getBirthDay())) {
            model.setBirthDay(null);
        }
        User user = modelMapper.map(model, User.class);
        user.setId(id);
        if (StringUtils.isNotBlank(model.getPassword())) {
            user.setPassword(authenticationService.pwdEncode(model.getPassword()));
        } else {
            // 不传密码时置空，避免把已有密码覆盖掉
            user.setPassword(null);
        }
        user.setModifyTime(new Date());
        userService.updateByIdFilter(user);
        return RestResponse.ok(UserResponseVM.from(userService.getUserById(id)));
    }

    /** 修改本人信息（个人中心） */
    @PutMapping("/profile")
    public RestResponse<UserResponseVM> updateProfile(@RequestBody @Valid UserUpdateVM model) {
        Integer id = getCurrentUser().getId();
        User dbUser = userService.getUserById(id);
        if (dbUser == null) {
            return RestResponse.fail(SystemCode.InnerError.getCode(), "用户不存在");
        }
        modelMapper.map(model, dbUser);
        dbUser.setModifyTime(new Date());
        userService.updateByIdFilter(dbUser);
        return RestResponse.ok(UserResponseVM.from(dbUser));
    }

    /** 启用 / 禁用用户 */
    @PutMapping("/{id}/status")
    public RestResponse<Integer> changeStatus(@PathVariable Integer id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return RestResponse.fail(SystemCode.InnerError.getCode(), "用户不存在");
        }
        UserStatusEnum current = UserStatusEnum.fromCode(user.getStatus());
        Integer newStatus = current == UserStatusEnum.Enable
                ? UserStatusEnum.Disable.getCode() : UserStatusEnum.Enable.getCode();
        user.setStatus(newStatus);
        user.setModifyTime(new Date());
        userService.updateByIdFilter(user);
        return RestResponse.ok(newStatus);
    }

    /** 删除用户（逻辑删除，保留外键关联数据） */
    @DeleteMapping("/{id}")
    public RestResponse<Void> delete(@PathVariable Integer id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return RestResponse.fail(SystemCode.InnerError.getCode(), "用户不存在");
        }
        user.setDeleted(true);
        user.setModifyTime(new Date());
        userService.updateByIdFilter(user);
        return RestResponse.ok();
    }

    /** 用户名下拉（按用户名模糊查询） */
    @GetMapping("/names")
    public RestResponse<List<KeyValue>> selectByUserName(@RequestParam(required = false) String userName) {
        return RestResponse.ok(userService.selectByUserName(userName));
    }
}
