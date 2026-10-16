package com.alvis.media.controller;

import com.alvis.media.base.RestResponse;
import com.alvis.media.base.SystemCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 容器级错误出口（未匹配到任何 Controller 时由容器转发到 /error）。
 * 统一返回 RestResponse，保证前端无论拿到业务异常还是容器异常，
 * 响应结构都是 {code, message, response}。
 */
@RestController
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    @RequestMapping(value = "/error", produces = {MediaType.APPLICATION_JSON_VALUE})
    public RestResponse<Void> error(HttpServletRequest request) {
        Object status = request.getAttribute("jakarta.servlet.error.status_code");
        if (status != null && status.toString().startsWith("4")) {
            return RestResponse.fail(404, "接口或资源不存在");
        }
        return RestResponse.fail(SystemCode.InnerError.getCode(), SystemCode.InnerError.getMessage());
    }
}
