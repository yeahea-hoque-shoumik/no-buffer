package com.prime.nobuffer.shields

/** Builds optional privacy request headers (DNT / GPC) for main-frame navigations. */
object NavigationHeaders {

    fun build(doNotTrack: Boolean, globalPrivacyControl: Boolean): Map<String, String> {
        if (!doNotTrack && !globalPrivacyControl) return emptyMap()
        return buildMap {
            if (doNotTrack) put("DNT", "1")
            if (globalPrivacyControl) {
                put("Sec-GPC", "1")
                put("Accept-Language", "en-US")
            }
        }
    }
}
