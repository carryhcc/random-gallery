package com.example.randomGallery.utils;

import cn.hutool.core.util.StrUtil;

public final class UserAgentUtils {

    private UserAgentUtils() {}

    public static boolean isSafari(String userAgent, String secChUa) {
        if (StrUtil.isBlank(userAgent)) {
            return false;
        }
        String ua = userAgent.toLowerCase();
        String chUa = StrUtil.blankToDefault(secChUa, "").toLowerCase();

        if (chUa.contains("google chrome") || chUa.contains("chromium")
                || chUa.contains("microsoft edge") || chUa.contains("opera")
                || chUa.contains("firefox")) {
            return false;
        }

        return ua.contains("safari")
                && !ua.contains("chrome")
                && !ua.contains("chromium")
                && !ua.contains("crios")
                && !ua.contains("edg")
                && !ua.contains("edga")
                && !ua.contains("edgios")
                && !ua.contains("opr")
                && !ua.contains("opios")
                && !ua.contains("fxios")
                && !ua.contains("firefox");
    }
}
