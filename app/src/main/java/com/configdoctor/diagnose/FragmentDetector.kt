package com.configdoctor.diagnose

object FragmentDetector {
    /**
     * تشخیص می‌دهد آیا این کانفیگ نیاز به Fragment دارد یا خیر.
     * اگر TCP و TLS اوکی باشند ولی Real Delay شکست بخورد، یعنی احتمالاً DPI ترافیک را بلاک می‌کند.
     */
    fun needsFragment(tcpOk: Boolean, tlsOk: Boolean?, realDelayOk: Boolean): Boolean {
        return tcpOk && (tlsOk == true) && !realDelayOk
    }
}