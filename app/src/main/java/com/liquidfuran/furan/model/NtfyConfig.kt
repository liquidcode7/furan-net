package com.liquidfuran.furan.model

data class NtfyConfig(
    val serverUrl: String = "",
    val requestTopic: String = "furan-unlock",
    val approvalTopic: String = "furan-approved",
    val sharedSecret: String = ""
) {
    val isConfigured: Boolean
        get() = serverUrl.isNotBlank() && sharedSecret.isNotBlank()
}
