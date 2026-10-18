package com.jiayi.campustrade.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常处理
 *
 * 职责边界（重要，与现有接口契约的关系）：
 * 1. 现有 Controller 大量使用 try-catch (RuntimeException) 把业务消息直接作为
 *    响应体返回（HTTP 200 + 文本），这是已上线、前端已依赖的稳定契约，
 *    本处理器【不改变】这些路径的行为（BusinessException 继承 RuntimeException，
 *    仍会被 Controller 内的 catch 优先捕获）。
 * 2. 本处理器只兜底"逃逸出 Controller"的异常：
 *    - 业务异常（BusinessException，未来新代码可不再手写 try-catch）
 *    - 认证类异常（ResponseStatusException，如登录 401）
 *    - 参数/请求体格式错误（400）
 *    - 未预期异常（500，日志记录完整堆栈，对外只返回友好提示）
 * 3. 统一响应结构：{ "code": 状态码, "message": 提示, "data": null }
 *    前端 request.js 的 pickServerMessage 已兼容该 JSON（取 data.message）。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 统一响应体结构：{ code, message, data } */
    private Map<String, Object> body(int code, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("data", null);
        return result;
    }

    /** 业务异常：HTTP 400 + 后端业务消息 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
    }

    /**
     * 认证/带状态码异常：保持原 HTTP 状态码（如登录失败 401），
     * message 取 reason，不再输出 Spring 默认的错误 JSON
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException e) {
        int status = e.getStatus().value();
        String reason = e.getReason() != null ? e.getReason() : e.getStatus().getReasonPhrase();
        return ResponseEntity.status(status).body(body(status, reason));
    }

    /** @Valid / @Validated 校验失败（当前项目暂未启用，兜底备用） */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBind(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "请求参数不合法";
        return ResponseEntity.badRequest().body(body(400, message));
    }

    /** 请求体 JSON 解析失败（如非法 JSON / 缺少请求体） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(body(400, "请求体格式不正确"));
    }

    /** URL 参数类型不匹配（如 /goods/abc） */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(body(400, "参数类型不正确"));
    }

    /** 缺少必填的 URL 参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest()
                .body(body(400, "缺少必填参数：" + e.getParameterName()));
    }

    /** 上传文件超过 5MB 限制（与 application-local.yml 的 max-file-size 一致） */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUpload(MaxUploadSizeExceededException e) {
        return ResponseEntity.badRequest().body(body(400, "上传文件大小超出限制（最大 5MB）"));
    }

    /** HTTP 方法不支持（如对只读接口发 DELETE） */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(body(405, "请求方式不支持"));
    }

    /** 未预期异常：记录完整堆栈便于排查，对外只返回友好提示，绝不泄漏内部细节 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception e) {
        log.error("未处理异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(500, "服务器繁忙，请稍后再试"));
    }
}
