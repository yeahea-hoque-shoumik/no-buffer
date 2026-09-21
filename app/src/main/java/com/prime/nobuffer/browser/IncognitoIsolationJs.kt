package com.prime.nobuffer.browser

/**
 * Best-effort private-mode storage on system WebView (no per-profile cookie jar).
 * localStorage / sessionStorage stay in memory for this WebView; IndexedDB is disabled.
 * Destroying the WebView discards it. HTTP cookies are purged separately when the last
 * private tab closes, for hosts that aren't also open in a regular tab.
 */
object IncognitoIsolationJs {
    const val SCRIPT: String = """
        (function() {
            try {
                var mem = Object.create(null);
                var keys = [];
                function syncKeys() { keys = Object.keys(mem); }
                var store = {
                    getItem: function(k) {
                        k = String(k);
                        return Object.prototype.hasOwnProperty.call(mem, k) ? mem[k] : null;
                    },
                    setItem: function(k, v) { mem[String(k)] = String(v); syncKeys(); },
                    removeItem: function(k) { delete mem[String(k)]; syncKeys(); },
                    clear: function() { mem = Object.create(null); keys = []; },
                    key: function(i) { return keys[i] || null; }
                };
                Object.defineProperty(store, 'length', { get: function() { return keys.length; } });
                try { Object.defineProperty(window, 'localStorage', { configurable: true, get: function() { return store; } }); } catch (e) {}
                try { Object.defineProperty(window, 'sessionStorage', { configurable: true, get: function() { return store; } }); } catch (e) {}
            } catch (e) {}
            try {
                Object.defineProperty(window, 'indexedDB', { configurable: true, get: function() { return undefined; } });
            } catch (e) {}
        })();
    """
}
