/**
 * 동의 기반 분석 스크립트 로더.
 *
 * GA4·Clarity는 서버 설정이 켜지고 ID가 있을 때만 window.ARKIVE_ANALYTICS에 값이 들어온다.
 * requireConsent=true이면 사용자가 동의하기 전까지 외부 스크립트를 요청하지 않는다.
 */
(function () {
    'use strict';

    var cfg = window.ARKIVE_ANALYTICS || {};
    if (!cfg.enabled) {
        return;
    }

    var storageKey = 'arkive.analytics.consent.' + (cfg.consentVersion || '1');
    var loaded = false;

    function loadGa4() {
        if (!cfg.ga4Id) {
            return;
        }
        var script = document.createElement('script');
        script.async = true;
        script.src = 'https://www.googletagmanager.com/gtag/js?id=' + encodeURIComponent(cfg.ga4Id);
        document.head.appendChild(script);
        window.dataLayer = window.dataLayer || [];
        window.gtag = window.gtag || function () { window.dataLayer.push(arguments); };
        window.gtag('js', new Date());
        window.gtag('config', cfg.ga4Id, { anonymize_ip: true });
    }

    function loadClarity() {
        if (!cfg.clarityId) {
            return;
        }
        (function (c, l, a, r, i, t, y) {
            c[a] = c[a] || function () { (c[a].q = c[a].q || []).push(arguments); };
            t = l.createElement(r);
            t.async = 1;
            t.src = 'https://www.clarity.ms/tag/' + encodeURIComponent(i);
            y = l.getElementsByTagName(r)[0];
            y.parentNode.insertBefore(t, y);
        })(window, document, 'clarity', 'script', cfg.clarityId);
    }

    function load() {
        if (loaded) {
            return;
        }
        loaded = true;
        loadGa4();
        loadClarity();
    }

    function save(value) {
        try {
            localStorage.setItem(storageKey, value);
        } catch (e) {
            // 저장소를 쓸 수 없으면 현재 방문에만 적용한다.
        }
    }

    function read() {
        try {
            return localStorage.getItem(storageKey);
        } catch (e) {
            return null;
        }
    }

    function showBanner() {
        var wrap = document.createElement('div');
        wrap.setAttribute('role', 'dialog');
        wrap.setAttribute('aria-label', '분석 쿠키 동의');
        wrap.style.cssText = 'position:fixed;left:16px;right:16px;bottom:16px;z-index:3000;'
            + 'background:#fff;border:1px solid #ccc;border-radius:8px;padding:16px;'
            + 'box-shadow:0 4px 16px rgba(0,0,0,.12);font-size:14px;line-height:1.5;';
        wrap.innerHTML = '<p style="margin:0 0 10px;">서비스 개선을 위한 분석 도구 사용에 동의하시겠습니까? '
            + '동의하지 않아도 서비스 이용에는 영향이 없습니다.</p>'
            + '<button type="button" id="arkiveAnalyticsAccept" class="krds-btn small primary">동의</button> '
            + '<button type="button" id="arkiveAnalyticsDeny" class="krds-btn small secondary">거부</button>';
        document.body.appendChild(wrap);
        document.getElementById('arkiveAnalyticsAccept').addEventListener('click', function () {
            save('granted');
            wrap.remove();
            load();
        });
        document.getElementById('arkiveAnalyticsDeny').addEventListener('click', function () {
            save('denied');
            wrap.remove();
        });
    }

    if (!cfg.requireConsent) {
        load();
        return;
    }
    var consent = read();
    if (consent === 'granted') {
        load();
    } else if (consent !== 'denied') {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', showBanner);
        } else {
            showBanner();
        }
    }
})();
