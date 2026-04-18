package com.liquidfuran.furan.model

enum class SigilState {
    IDLE,        // Default resting state
    REQUESTING,  // Sending unlock request to ntfy
    WAITING,     // Request sent, awaiting wife's approval (slow rotation)
    APPROVED,    // Approval received — pulse + full cyan glow
    DENIED       // Invalid/expired response — magenta tint + shake
}
