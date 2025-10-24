package com.example.randomGallery.exception;

import com.example.randomGallery.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(MethodArgumentNotValidException e) {
        log.error("参数验证失败: {}", e.getMessage());
        String message = e.getBindingResult().getFieldError() != null 
            ? e.getBindingResult().getFieldError().getDefaultMessage() 
            : "参数验证失败";
        return ResponseEntity.badRequest().body(Result.error(400, message));
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error("参数类型不匹配: {}", e.getMessage());
        return ResponseEntity.badRequest().body(Result.error(400, "参数类型不匹配"));
    }

    /**
     * 处理静态资源未找到异常（过滤掉浏览器开发者工具请求）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        // 过滤掉 Chrome DevTools 的请求，不记录日志
        if (e.getResourcePath() != null && 
            (e.getResourcePath().contains(".well-known") || 
             e.getResourcePath().contains("favicon.ico") ||
             e.getResourcePath().contains("robots.txt"))) {
            return ResponseEntity.notFound().build();
        }
        
        // 其他静态资源未找到的情况才记录日志
        log.warn("静态资源未找到: {}", e.getResourcePath());
        return ResponseEntity.notFound().build();
    }

    /**
     * 处理数据库异常
     */
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<Result<Void>> handleSQLException(SQLException e) {
        log.error("数据库操作异常: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Result.error(500, "数据库操作失败"));
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result<Void>> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Result.error(500, "系统内部错误"));
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("未知异常: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Result.error(500, "系统异常"));
    }
}
