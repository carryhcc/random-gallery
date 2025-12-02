package com.example.randomGallery.exception;

/**
 * 重复提交异常类
 * 用于标识由于重复提交操作而产生的异常
 */
public class DuplicateSubmitException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private Integer code;

    /**
     * 构造方法
     * @param message 错误信息
     */
    public DuplicateSubmitException(String message) {
        super(message);
        this.code = 429; // 默认使用429状态码表示请求过多
    }

    /**
     * 构造方法
     * @param code 错误码
     * @param message 错误信息
     */
    public DuplicateSubmitException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造方法
     * @param message 错误信息
     * @param cause 异常原因
     */
    public DuplicateSubmitException(String message, Throwable cause) {
        super(message, cause);
        this.code = 429;
    }

    /**
     * 获取错误码
     * @return 错误码
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 设置错误码
     * @param code 错误码
     */
    public void setCode(Integer code) {
        this.code = code;
    }
}
