package com.qmz.canteen.common;

import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 全局异常处理：所有接口异常统一转成 { code, message, timestamp, traceId } 结构，
 * HTTP 状态码保持 200，业务状态由 code 表达（前端 axios 拦截器按 code 分支）。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 参数错误最多返回几条，避免超长提示 */
    private static final int MAX_PARAM_ERRORS = 3;
    private static final String DEFAULT_PARAM_MESSAGE = ResultCode.PARAM_ERROR.getMessage();

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 {} {} -> {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return build(e.getCode(), e.getMessage(), request);
    }

    /** @RequestBody @Valid 失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest request) {
        return paramError(fieldMessages(e.getBindingResult().getFieldErrors()), e, request);
    }

    /** @ModelAttribute 绑定校验失败（分页/查询参数） */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e, HttpServletRequest request) {
        return paramError(fieldMessages(e.getBindingResult().getFieldErrors()), e, request);
    }

    /** Spring 6.1+ 方法级校验失败（@PathVariable/@RequestParam 上的约束） */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public Result<Void> handleHandlerMethodValidation(HandlerMethodValidationException e,
                                                       HttpServletRequest request) {
        List<String> messages = e.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(error -> error.getDefaultMessage())
                .distinct()
                .collect(Collectors.toList());
        return paramError(messages, e, request);
    }

    /** 类级别 @Validated 触发的 Bean Validation 违规 */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e,
                                                            HttpServletRequest request) {
        List<String> messages = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());
        return paramError(messages, e, request);
    }

    /** 缺参、类型不匹配、请求体不可解析 */
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingRequestValueException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public Result<Void> handleParamException(Exception e, HttpServletRequest request) {
        String message = DEFAULT_PARAM_MESSAGE;
        if (e instanceof MethodArgumentTypeMismatchException mismatch) {
            message = "参数 " + mismatch.getName() + " 类型不正确";
        } else if (e instanceof MissingServletRequestParameterException missing) {
            message = "缺少必填参数 " + missing.getParameterName();
        } else if (e instanceof HttpMessageNotReadableException notReadable) {
            message = describeUnreadable(notReadable);
        }
        return paramError(List.of(message), e, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                                  HttpServletRequest request) {
        log.warn("请求方式不支持 {} {}", request.getMethod(), request.getRequestURI());
        return build(405, "请求方式不支持，请使用 " + e.getSupportedHttpMethods(), request);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public Result<Void> handleNotFound(Exception e, HttpServletRequest request) {
        log.warn("资源不存在 {} {}", request.getMethod(), request.getRequestURI());
        return build(ResultCode.NOT_FOUND.getCode(), "请求的资源不存在：" + request.getRequestURI(), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<Void> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e,
                                                     HttpServletRequest request) {
        return build(415, "请求格式不支持，请使用 application/json", request);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKey(DuplicateKeyException e, HttpServletRequest request) {
        log.warn("唯一约束冲突 {} {}", request.getMethod(), request.getRequestURI());
        return build(ResultCode.DATA_EXISTS.getCode(), "数据已存在，请勿重复提交", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Void> handleDataIntegrity(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("数据完整性冲突 {} {} -> {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return build(ResultCode.PARAM_ERROR.getCode(), "数据关联校验失败，请检查引用数据是否存在", request);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.error("系统异常 {} {} traceId={}", request.getMethod(), request.getRequestURI(), traceId, e);
        Result<Void> result = build(ResultCode.ERROR.getCode(), "系统繁忙，请稍后再试（追踪号：" + traceId + "）", request);
        result.setTraceId(traceId);
        return result;
    }

    private String describeUnreadable(HttpMessageNotReadableException e) {
        if (e.getMostSpecificCause() instanceof InvalidFormatException format) {
            if (format.getTargetType() != null && LocalDateTime.class.isAssignableFrom(format.getTargetType())) {
                return "时间格式应为 yyyy-MM-dd HH:mm:ss";
            }
            if (format.getTargetType() != null && Number.class.isAssignableFrom(format.getTargetType())) {
                return "参数 " + format.getPath().stream().map(Reference::getFieldName).reduce((a, b) -> b)
                        .orElse("数值") + " 必须是数字";
            }
        }
        return "请求参数格式不正确，请检查后重试";
    }

    private List<String> fieldMessages(List<FieldError> errors) {
        return errors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());
    }

    private Result<Void> paramError(List<String> messages, Exception e, HttpServletRequest request) {
        String message = messages == null || messages.isEmpty()
                ? DEFAULT_PARAM_MESSAGE
                : messages.stream().limit(MAX_PARAM_ERRORS).collect(Collectors.joining("；"));
        log.warn("参数校验失败 {} {} -> {}", request.getMethod(), request.getRequestURI(), message);
        return build(ResultCode.PARAM_ERROR.getCode(), message, request);
    }

    private Result<Void> build(Integer code, String message, HttpServletRequest request) {
        Result<Void> result = new Result<>();
        result.setCode(code == null ? ResultCode.ERROR.getCode() : code);
        result.setMessage(message);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }
}
