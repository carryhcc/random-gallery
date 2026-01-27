package com.example.randomGallery.service.Impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.randomGallery.entity.DO.VisitLogDO;
import com.example.randomGallery.entity.DO.VisitUserDO;
import com.example.randomGallery.service.VisitService;
import com.example.randomGallery.service.mapper.VisitLogMapper;
import com.example.randomGallery.service.mapper.VisitUserMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitServiceImpl implements VisitService {

    private final VisitUserMapper visitUserMapper;
    private final VisitLogMapper visitLogMapper;

    private static final String VISITOR_COOKIE_NAME = "VISITOR_ID";

    // 不再使用 @Async 注解整个方法，改用内部异步处理，确保Request数据在主线程提取
    @Override
    public void recordVisit(HttpServletRequest request, Long duration, Integer status) {
        try {
            // 1. 在主线程提取所有需要的Request信息
            String ip = getIpAddress(request);
            String userAgentStr = request.getHeader("User-Agent");
            String uuid = getOrGenerateUuid(request);
            String uri = request.getRequestURI();
            String method = request.getMethod();
            String queryString = request.getQueryString();

            // 2. 异步执行数据库操作
            CompletableFuture.runAsync(() -> {
                doRecordVisit(ip, userAgentStr, uuid, uri, method, queryString, duration, status);
            });

        } catch (Exception e) {
            log.error("准备记录访问日志失败", e);
        }
    }

    private void doRecordVisit(String ip, String userAgentStr, String uuid, String uri, String method,
            String queryString, Long duration, Integer status) {
        try {
            // 解析UserAgent
            UserAgent userAgent = UserAgentUtil.parse(userAgentStr);

            // 1. 处理访客用户
            VisitUserDO visitUser = getOrCreateVisitUser(uuid, ip, userAgentStr, userAgent);

            // 2. 记录访问日志
            VisitLogDO visitLog = new VisitLogDO();
            visitLog.setVisitUserId(visitUser.getId());
            visitLog.setUri(uri);
            visitLog.setMethod(method);
            visitLog.setParams(limitString(queryString, 65535));
            visitLog.setStatus(status);
            visitLog.setDuration(duration);
            visitLog.setIp(ip);
            visitLog.setCreateTime(LocalDateTime.now());

            visitLogMapper.insert(visitLog);
        } catch (Exception e) {
            log.error("异步保存访问日志失败", e);
        }
    }

    private synchronized VisitUserDO getOrCreateVisitUser(String uuid, String ip, String userAgentStr,
            UserAgent userAgent) {
        // 加 synchronized 简单防止高并发下同一UUID瞬间创建多个用户

        // 先根据UUID查询
        VisitUserDO visitUser = visitUserMapper.selectOne(new LambdaQueryWrapper<VisitUserDO>()
                .eq(VisitUserDO::getUuid, uuid));

        LocalDateTime now = LocalDateTime.now();

        if (visitUser == null) {
            // 如果UUID不存在，尝试根据IP和UserAgent查找
            visitUser = visitUserMapper.selectOne(new LambdaQueryWrapper<VisitUserDO>()
                    .eq(VisitUserDO::getIp, ip)
                    .eq(VisitUserDO::getUserAgent, userAgentStr)
                    .last("LIMIT 1"));
        }

        if (visitUser == null) {
            // 创建新用户
            visitUser = new VisitUserDO();
            visitUser.setUuid(uuid);
            visitUser.setIp(ip);
            visitUser.setUserAgent(userAgentStr);
            visitUser.setBrowser(userAgent.getBrowser().getName());
            visitUser.setOs(userAgent.getOs().getName());
            visitUser.setDeviceType(userAgent.getPlatform().getName());
            visitUser.setFirstVisitTime(now);
            visitUser.setLastVisitTime(now);
            visitUser.setVisitCount(1);
            try {
                visitUserMapper.insert(visitUser);
            } catch (Exception e) {
                // 忽略唯一约束冲突，重新查询
                visitUser = visitUserMapper
                        .selectOne(new LambdaQueryWrapper<VisitUserDO>().eq(VisitUserDO::getUuid, uuid));
            }
        } else {
            // 更新老用户
            visitUser.setLastVisitTime(now);
            visitUser.setVisitCount(visitUser.getVisitCount() + 1);
            // 更新可能变动的信息
            boolean changed = false;
            if (!StrUtil.equals(visitUser.getIp(), ip)) {
                visitUser.setIp(ip);
                changed = true;
            }
            if (!StrUtil.equals(visitUser.getUserAgent(), userAgentStr)) {
                visitUser.setUserAgent(userAgentStr);
                // 重新解析UA更新设备信息
                visitUser.setBrowser(userAgent.getBrowser().getName());
                visitUser.setOs(userAgent.getOs().getName());
                visitUser.setDeviceType(userAgent.getPlatform().getName());
                changed = true;
            }
            if (changed || visitUser.getLastVisitTime().isBefore(now.minusSeconds(60))) {
                // 只有信息变动或超过1分钟才更新DB，减少热点写
                visitUserMapper.updateById(visitUser);
            } else {
                visitUserMapper.updateById(visitUser);
            }
        }
        return visitUser;
    }

    private String getOrGenerateUuid(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (VISITOR_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String limitString(String str, int maxLength) {
        if (str == null)
            return null;
        if (str.length() > maxLength) {
            return str.substring(0, maxLength);
        }
        return str;
    }
}
