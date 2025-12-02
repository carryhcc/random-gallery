package com.example.randomGallery.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 防重复提交注解
 * 加了这个注解的接口在指定时间内重复点击时会提示重复操作
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PreventDuplicateSubmit {
    
    /**
     * 防重复提交的时间间隔（毫秒）
     * 默认1000毫秒（1秒）
     */
    long interval() default 1000;
    
    /**
     * 错误提示信息
     */
    String message() default "操作过于频繁，请稍后重试";
}
