package com.example.randomGallery.config;

import com.example.randomGallery.annotation.SafeImage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 图片安全模式切面
 * 拦截带有 @SafeImage 注解的方法，将返回值中的图片URL替换为安全占位图
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SafeImageAspect {

    private final SafeImageProperties safeImageProperties;

    /**
     * 需要替换URL的字段名关键词（包含这些关键词的URL字段会被替换）
     */
    private static final Set<String> URL_FIELD_KEYWORDS = Set.of(
            "image", "media", "pic", "cover", "avatar", "photo", "thumbnail");

    /**
     * 已处理对象缓存，防止循环引用导致的无限递归
     */
    private final ThreadLocal<Set<Integer>> processedObjects = ThreadLocal.withInitial(HashSet::new);

    /**
     * 拦截带有 @SafeImage 注解的方法
     */
    @Around("@annotation(com.example.randomGallery.annotation.SafeImage)")
    public Object processSafeImage(ProceedingJoinPoint joinPoint) throws Throwable {
        // 执行原方法
        Object result = joinPoint.proceed();

        // 检查是否启用安全模式
        if (!Boolean.TRUE.equals(safeImageProperties.getEnabled())) {
            log.debug("安全图片模式未启用，跳过URL替换");
            return result;
        }

        // 检查占位图URL是否配置
        if (!StringUtils.hasText(safeImageProperties.getPlaceholderUrl())) {
            log.warn("安全图片模式已启用，但未配置占位图URL，跳过URL替换");
            return result;
        }

        // 获取注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        SafeImage annotation = method.getAnnotation(SafeImage.class);

        // 检查注解是否启用
        if (!annotation.value()) {
            log.debug("@SafeImage注解已禁用，跳过URL替换");
            return result;
        }

        // 清空线程缓存
        processedObjects.get().clear();

        try {
            // 替换返回值中的图片URL
            replaceImageUrls(result);
            log.debug("成功替换方法 {} 返回值中的图片URL", method.getName());
        } catch (Exception e) {
            log.error("替换图片URL时发生错误", e);
        } finally {
            // 清理线程缓存
            processedObjects.remove();
        }

        return result;
    }

    /**
     * 递归替换对象中的图片URL
     */
    private void replaceImageUrls(Object obj) {
        if (obj == null) {
            return;
        }

        // 防止循环引用
        int objId = System.identityHashCode(obj);
        if (processedObjects.get().contains(objId)) {
            return;
        }
        processedObjects.get().add(objId);

        Class<?> clazz = obj.getClass();

        // 跳过基本类型和常见不可变类
        if (clazz.isPrimitive() || clazz.getName().startsWith("java.lang") ||
                clazz.getName().startsWith("java.time") || clazz.getName().startsWith("java.math")) {
            return;
        }

        // 处理集合类型
        if (obj instanceof Collection<?> collection) {
            for (Object item : collection) {
                replaceImageUrls(item);
            }
            return;
        }

        // 处理Map类型
        if (obj instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                replaceImageUrls(value);
            }
            return;
        }

        // 处理数组类型
        if (clazz.isArray()) {
            Object[] array = (Object[]) obj;
            for (Object item : array) {
                replaceImageUrls(item);
            }
            return;
        }

        // 处理普通对象，遍历所有字段
        Field[] fields = getAllFields(clazz);
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                Object fieldValue = field.get(obj);

                if (fieldValue == null) {
                    continue;
                }

                // 检查是否为URL字段
                if (isImageUrlField(field)) {
                    // 替换URL
                    if (fieldValue instanceof String url && StringUtils.hasText(url)) {
                        field.set(obj, safeImageProperties.getPlaceholderUrl());
                        log.trace("替换字段 {} 的URL: {} -> {}",
                                field.getName(), url, safeImageProperties.getPlaceholderUrl());
                    }
                } else {
                    // 递归处理嵌套对象
                    replaceImageUrls(fieldValue);
                }
            } catch (IllegalAccessException e) {
                log.warn("无法访问字段 {}.{}", clazz.getName(), field.getName(), e);
            }
        }
    }

    /**
     * 判断字段是否为图片URL字段
     */
    private boolean isImageUrlField(Field field) {
        String fieldName = field.getName().toLowerCase();

        // 必须包含 "url" 关键词
        if (!fieldName.contains("url")) {
            return false;
        }

        // 检查是否包含图片相关关键词
        for (String keyword : URL_FIELD_KEYWORDS) {
            if (fieldName.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取类的所有字段（包括父类）
     */
    private Field[] getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields.toArray(new Field[0]);
    }
}
