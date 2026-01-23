package com.example.randomGallery.annotation;

import java.lang.annotation.*;

/**
 * 图片安全模式注解
 * 用于标记需要将返回数据中的图片URL替换为安全占位图的Controller方法
 * 
 * <p>
 * 使用示例：
 * 
 * <pre>
 * {@code
 * &#64;GetMapping("/list")
 * @SafeImage
 * public Result<XhsWorkPageVO> listWorks() {
 *     // ...
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SafeImage {
    /**
     * 是否启用安全图片模式
     * 
     * @return true表示启用，false表示不启用
     */
    boolean value() default true;
}
