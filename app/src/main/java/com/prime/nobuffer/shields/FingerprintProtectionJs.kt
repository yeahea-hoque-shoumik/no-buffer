package com.prime.nobuffer.shields

/**
 * Phase 18 — "farbling-lite": JS-injection anti-fingerprinting, weaker than Brave's per-session
 * engine-level farbling (detectable via `Function.prototype.toString` on the patched functions),
 * but raises the bar over doing nothing. Injected at document-start via
 * `WebViewCompat.addDocumentStartJavaScript` so it runs before the page's own scripts.
 */
object FingerprintProtectionJs {

    /** [seed] should be a per-WebView-instance random int so noise differs across app sessions/tabs. */
    fun script(seed: Int): String = """
        (function() {
            try {
                Object.defineProperty(navigator, 'globalPrivacyControl', { value: true, configurable: true });
            } catch (e) {}
            try {
                Object.defineProperty(navigator, 'language', { get: function() { return 'en-US'; }, configurable: true });
                Object.defineProperty(navigator, 'languages', { get: function() { return ['en-US']; }, configurable: true });
            } catch (e) {}
            try {
                var __nbSeed = $seed;
                function __nbNoise(i) {
                    __nbSeed = (__nbSeed * 1103515245 + 12345) & 0x7fffffff;
                    return (__nbSeed % 5) - 2;
                }
                var origToDataURL = HTMLCanvasElement.prototype.toDataURL;
                HTMLCanvasElement.prototype.toDataURL = function() {
                    try {
                        var ctx = this.getContext('2d');
                        if (ctx) {
                            var imgData = ctx.getImageData(0, 0, this.width, this.height);
                            for (var i = 0; i < imgData.data.length; i += 4) {
                                imgData.data[i] = Math.min(255, Math.max(0, imgData.data[i] + __nbNoise(i)));
                            }
                            ctx.putImageData(imgData, 0, 0);
                        }
                    } catch (e) {}
                    return origToDataURL.apply(this, arguments);
                };
                var origGetImageData = CanvasRenderingContext2D.prototype.getImageData;
                CanvasRenderingContext2D.prototype.getImageData = function() {
                    var result = origGetImageData.apply(this, arguments);
                    for (var i = 0; i < result.data.length; i += 4) {
                        result.data[i] = Math.min(255, Math.max(0, result.data[i] + __nbNoise(i)));
                    }
                    return result;
                };
            } catch (e) {}
            try {
                var origGetChannelData = AudioBuffer.prototype.getChannelData;
                AudioBuffer.prototype.getChannelData = function() {
                    var data = origGetChannelData.apply(this, arguments);
                    for (var i = 0; i < data.length; i += 100) {
                        data[i] = data[i] + (__nbNoise(i) * 0.0000001);
                    }
                    return data;
                };
            } catch (e) {}
            try {
                var origGetParameter = WebGLRenderingContext.prototype.getParameter;
                WebGLRenderingContext.prototype.getParameter = function(param) {
                    var value = origGetParameter.apply(this, arguments);
                    if (typeof value === 'string') { return value + ''; }
                    return value;
                };
            } catch (e) {}
            try {
                function __nbInsertReferrerMeta() {
                    if (document.head && !document.querySelector('meta[name="referrer"]')) {
                        var meta = document.createElement('meta');
                        meta.name = 'referrer';
                        meta.content = 'strict-origin-when-cross-origin';
                        document.head.insertBefore(meta, document.head.firstChild);
                    }
                }
                if (document.head) {
                    __nbInsertReferrerMeta();
                } else {
                    var __nbMo = new MutationObserver(function() {
                        if (document.head) { __nbInsertReferrerMeta(); __nbMo.disconnect(); }
                    });
                    __nbMo.observe(document.documentElement, { childList: true, subtree: true });
                }
            } catch (e) {}
        })();
    """.trimIndent()
}
