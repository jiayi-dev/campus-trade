package com.jiayi.campustrade.exception;

/**
 * 业务异常
 *
 * 用于表达"业务规则不允许"的失败，例如：
 * 商品不存在、无权修改商品、订单状态不允许变更、商品已售出、
 * 用户不存在、卖家申请状态错误、参数非法等。
 *
 * 设计约束（重要）：
 * 继承 RuntimeException 而不是 Exception ——
 * 现有 Controller / Service 中大量 catch (RuntimeException e) 的代码
 * 无需任何修改即可继续捕获本异常，保证既有接口返回行为完全不变；
 * 只有未被捕获、逃逸到 Spring 层的异常，才会由
 * GlobalExceptionHandler 统一转换为 {code, message, data} JSON 响应。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
