package com.alvis.media.configuration.spring.exception;

import com.alvis.media.base.RestResponse;
import com.alvis.media.base.SystemCode;
import com.alvis.media.exception.BusinessException;
import com.alvis.media.utility.ErrorUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理。
 *
 * 1.统一返回格式：所有异常都转成 RestResponse（code / message / response），前端只需判断 code；
 * 2.信息脱敏：兜底 handler 只在服务端日志打印异常堆栈，响应体不再回显 e.getMessage()，
 *   避免把 SQL 语句、表名、字段名、文件路径等内部信息泄露给前端；
 * 3.traceId：日志与响应头 X-Trace-Id 对应（见 TraceIdFilter），便于按一次请求排查全链路。
 */
@RestControllerAdvice
public class ExceptionHandle {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandle.class);

    /** 对外统一话术，避免泄露内部实现 */
    private static final String GENERIC_ERROR = "系统繁忙，请稍后重试";

    /**
     * 业务异常：由代码显式抛出，其 message 本身就是安全的用户提示，可直接下发
     */
    @ExceptionHandler(BusinessException.class)
    public RestResponse<Void> handler(BusinessException e, HttpServletRequest request) {
        logger.warn("业务异常 uri={} code={} message={}", request.getRequestURI(), e.getCode(), e.getMessage());
        int code = e.isUnknown() ? SystemCode.InnerError.getCode() : e.getCode();
        return RestResponse.fail(code, e.getMessage());
    }

    /**
     * 主动抛出的参数 / 业务前置校验失败（如"用户名已被注册"），
     * message 由代码显式书写、不含内部信息，可直接回显给前端，
     * 否则会落到兜底 handler 变成"系统繁忙"，前端无法给出正确提示。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public RestResponse<Void> handler(IllegalArgumentException e, HttpServletRequest request) {
        logger.warn("参数校验不通过 uri={} message={}", request.getRequestURI(), e.getMessage());
        return RestResponse.fail(SystemCode.ParameterValidError.getCode(), e.getMessage());
    }

    /**
     * 兜底：未预期异常。堆栈只进日志，响应体不暴露内部细节
     */
    @ExceptionHandler(Exception.class)
    public RestResponse<Void> handler(Exception e, HttpServletRequest request) {
        logger.error("系统异常 uri={} ", request.getRequestURI(), e);
        return RestResponse.fail(SystemCode.InnerError.getCode(), GENERIC_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public RestResponse<Void> handler(MethodArgumentNotValidException e) {
        String errorMsg = e.getBindingResult().getAllErrors().stream().map(file -> {
            FieldError fieldError = (FieldError) file;
            return ErrorUtil.parameterErrorFormat(fieldError.getField(), fieldError.getDefaultMessage());
        }).collect(Collectors.joining());
        return RestResponse.fail(SystemCode.ParameterValidError.getCode(), errorMsg);
    }

    @ExceptionHandler(BindException.class)
    public RestResponse<Void> handler(BindException e) {
        String errorMsg = e.getBindingResult().getAllErrors().stream().map(file -> {
            FieldError fieldError = (FieldError) file;
            return ErrorUtil.parameterErrorFormat(fieldError.getField(), fieldError.getDefaultMessage());
        }).collect(Collectors.joining());
        return RestResponse.fail(SystemCode.ParameterValidError.getCode(), errorMsg);
    }

    /**
     * 方法级参数校验失败（@RequestParam 上直接标注约束注解，配合 @Validated 生效）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public RestResponse<Void> handler(ConstraintViolationException e) {
        String errorMsg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining());
        return RestResponse.fail(SystemCode.ParameterValidError.getCode(), errorMsg);
    }

    /**
     * 请求体不可读：JSON 格式错误、字段类型不匹配
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public RestResponse<Void> handler(HttpMessageNotReadableException e) {
        logger.warn("请求体解析失败: {}", e.getMessage());
        return RestResponse.fail(SystemCode.ParameterValidError.getCode(), "请求参数格式错误");
    }

    /**
     * 缺少必填请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public RestResponse<Void> handler(MissingServletRequestParameterException e) {
        return RestResponse.fail(SystemCode.ParameterValidError.getCode(), "缺少必填参数：" + e.getParameterName());
    }

    /**
     * 参数类型不匹配（如 /video/{id} 传了非数字）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public RestResponse<Void> handler(MethodArgumentTypeMismatchException e) {
        return RestResponse.fail(SystemCode.ParameterValidError.getCode(), "参数类型不正确：" + e.getName());
    }

    /**
     * 请求方法不支持（把 POST 打到只允许 PUT/DELETE 的接口上）
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public RestResponse<Void> handler(HttpRequestMethodNotSupportedException e) {
        return RestResponse.fail(SystemCode.ParameterValidError.getCode(), "请求方法不支持：" + e.getMethod());
    }

    /**
     * 上传文件超过 multipart 限制
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public RestResponse<Void> handler(MaxUploadSizeExceededException e) {
        return RestResponse.fail(SystemCode.ParameterValidError.getCode(), "上传文件超过大小限制");
    }

    /**
     * 404：接口不存在
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public RestResponse<Void> handler(NoHandlerFoundException e) {
        return RestResponse.fail(404, "接口不存在：" + e.getRequestURL());
    }
}
