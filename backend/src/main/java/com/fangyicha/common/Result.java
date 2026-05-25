package com.fangyicha.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一API响应结果封装
 * 所有接口均返回此格式：{code, message, data}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /** 状态码 */
    private int code;
    /** 提示信息 */
    private String message;
    /** 数据负载 */
    private T data;

    /** 分页数据（可选） */
    private Long total;
    private Long page;
    private Long pageSize;

    // ---------- 成功响应 ----------

    public static <T> Result<T> success() {
        return new Result<>(200, "success", null, null, null, null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data, null, null, null);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data, null, null, null);
    }

    public static <T> Result<T> success(T data, Long total, Long page, Long pageSize) {
        return new Result<>(200, "success", data, total, page, pageSize);
    }

    // ---------- 错误响应 ----------

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null, null, null, null);
    }

    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(401, message, null, null, null, null);
    }

    public static <T> Result<T> forbidden(String message) {
        return new Result<>(403, message, null, null, null, null);
    }

    public static <T> Result<T> notFound(String message) {
        return new Result<>(404, message, null, null, null, null);
    }

    public static <T> Result<T> badRequest(String message) {
        return new Result<>(400, message, null, null, null, null);
    }

    public static <T> Result<T> serverError(String message) {
        return new Result<>(500, message, null, null, null, null);
    }

}
