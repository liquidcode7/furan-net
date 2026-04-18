package com.liquidfuran.furan.model

enum class FuranMode {
    DUMB,   // Locked — allowlist only, sigil for unlock requests
    SMART   // Unlocked — full app drawer, search, browser widget
}
