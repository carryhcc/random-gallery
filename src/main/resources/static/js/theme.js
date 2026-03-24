/**
 * 主题切换功能
 * 支持深色/浅色模式切换，自动检测系统偏好，保存用户选择
 */

(function () {
    'use strict';

    const THEME_STORAGE_KEY = 'random-gallery-theme';
    const THEME_ATTRIBUTE = 'data-theme';
    const RESUME_CLASS = 'page-resuming';
    const RESUME_EVENT = 'app:page-resumed';
    let resumeTimer = null;

    /**
     * 获取系统主题偏好
     * @returns {string} 'dark' 或 'light'
     */
    function getSystemTheme() {
        if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
            return 'dark';
        }
        return 'light'; // 默认浅色模式
    }

    /**
     * 获取当前主题
     * @returns {string} 'dark' 或 'light'
     */
    function getCurrentTheme() {
        // 优先使用用户保存的选择
        const savedTheme = localStorage.getItem(THEME_STORAGE_KEY);
        if (savedTheme === 'dark' || savedTheme === 'light') {
            return savedTheme;
        }
        // 如果没有保存的选择，使用系统偏好
        return getSystemTheme();
    }

    /**
     * 应用主题
     * @param {string} theme - 'dark' 或 'light'
     */
    function applyTheme(theme) {
        if (theme === 'dark' || theme === 'light') {
            document.documentElement.setAttribute(THEME_ATTRIBUTE, theme);
            localStorage.setItem(THEME_STORAGE_KEY, theme);
        }
    }

    /**
     * 切换主题
     */
    function toggleTheme() {
        const currentTheme = getCurrentTheme();
        const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
        applyTheme(newTheme);
        return newTheme;
    }

    /**
     * 初始化主题
     */
    function initTheme() {
        const theme = getCurrentTheme();
        applyTheme(theme);

        // 监听系统主题变化（仅在用户未手动设置时）
        if (window.matchMedia) {
            const mediaQuery = window.matchMedia('(prefers-color-scheme: light)');
            mediaQuery.addEventListener('change', (e) => {
                // 如果用户没有手动保存过主题，则跟随系统变化
                if (!localStorage.getItem(THEME_STORAGE_KEY)) {
                    const systemTheme = e.matches ? 'light' : 'dark';
                    applyTheme(systemTheme);
                }
            });
        }
    }

    function isMobileDevice() {
        const isMobileViewport = window.matchMedia && window.matchMedia('(max-width: 768px)').matches;
        const isTouchDevice = 'ontouchstart' in window || navigator.maxTouchPoints > 0;
        return isMobileViewport || isTouchDevice;
    }

    function stabilizePageOnResume() {
        if (!isMobileDevice() || !document.body) {
            return;
        }
        document.body.classList.add(RESUME_CLASS);
        window.clearTimeout(resumeTimer);
        resumeTimer = window.setTimeout(() => {
            document.body.classList.remove(RESUME_CLASS);
        }, 320);
        window.dispatchEvent(new CustomEvent(RESUME_EVENT));
    }

    function bindResumeStabilizer() {
        document.addEventListener('visibilitychange', () => {
            if (!document.hidden) {
                stabilizePageOnResume();
            }
        });

        window.addEventListener('pageshow', (event) => {
            if (event.persisted) {
                stabilizePageOnResume();
            }
        });

        window.addEventListener('focus', () => {
            if (document.visibilityState === 'visible') {
                stabilizePageOnResume();
            }
        });
    }

    // 立即应用主题（避免闪烁）
    // 使用立即执行，不等待DOM加载
    initTheme();
    bindResumeStabilizer();

    // 暴露到全局作用域
    window.themeManager = {
        toggle: toggleTheme,
        set: applyTheme,
        get: getCurrentTheme,
        init: initTheme,
        stabilizePageOnResume: stabilizePageOnResume,
        resumeEventName: RESUME_EVENT
    };
})();
