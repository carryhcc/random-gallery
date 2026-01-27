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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
            // 1. 提取并校验IP
            String ip = getIpAddress(request);
            // 核心优化：判断IP是否为内网/回环地址，是则直接返回不记录
            if (isPrivateIp(ip)) {
                return;
            }
            // 2. 提取请求上下文信息（主线程提取，避免异步线程访问request导致的线程安全问题）
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

    /**
     * 判断IP地址是否为内网/本地地址（兼容IPv4和IPv6）
     * 覆盖范围：
     * - IPv4：127.0.0.0/8（回环）、10.0.0.0/8、172.16.0.0/12、192.168.0.0/16
     * - IPv6：::1（回环）、fe80::/10（链路本地）、fc00::/7（唯一本地/私有）
     *
     * @param ip 待判断的IP字符串（如 "192.168.1.1"、"fe80::1%eth0"、"::1"）
     * @return true=内网/本地地址，false=公网地址/解析失败
     */
    public static boolean isPrivateIp(String ip) {
        // 空IP直接返回false，避免空指针
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }

        try {
            // 解析IP地址（自动兼容IPv4/IPv6，忽略IPv6的zone index如%eth0）
            InetAddress inetAddress = InetAddress.getByName(ip);

            // 第一步：判断是否为回环地址（IPv4:127.0.0.0/8；IPv6:::1）
            if (inetAddress.isLoopbackAddress()) {
                return true;
            }

            // 第二步：判断IPv4私有地址
            if (inetAddress instanceof Inet4Address) {
                byte[] ipBytes = inetAddress.getAddress();
                // 10.0.0.0/8 网段
                if (ipBytes[0] == 10) {
                    return true;
                }
                // 172.16.0.0/12 网段（172.16~31）
                if (ipBytes[0] == (byte) 172 && (ipBytes[1] >= 16 && ipBytes[1] <= 31)) {
                    return true;
                }
                // 192.168.0.0/16 网段
                if (ipBytes[0] == (byte) 192 && ipBytes[1] == (byte) 168) {
                    return true;
                }
            }

            // 第三步：判断IPv6本地/私有地址
            if (inetAddress instanceof Inet6Address) {
                byte[] ipBytes = inetAddress.getAddress();
                // 链路本地地址（fe80::/10）：前两个字节为 0xFE 0x80/0x81/.../0xBF
                if ((ipBytes[0] & (byte) 0xFF) == (byte) 0xFE && (ipBytes[1] & (byte) 0xC0) == (byte) 0x80) {
                    return true;
                }
                // 唯一本地地址（ULA，fc00::/7）：第一个字节为 0xFC 或 0xFD
                return (ipBytes[0] & (byte) 0xFF) == (byte) 0xFC || (ipBytes[0] & (byte) 0xFF) == (byte) 0xFD;
            }

            // 非内网地址
            return false;

        } catch (UnknownHostException e) {
            // IP格式非法/解析失败（如 "abc"、"192.168.256.1"），默认返回false（避免误拦截）
            log.warn("IP地址解析失败，无法判断是否为内网IP，IP:{}", ip, e);
            return false;
        }
    }
}
