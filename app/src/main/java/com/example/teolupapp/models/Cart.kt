package com.example.teolupapp.models

data class Cart(
    var key: String = "",       // Firebase Key
    val name: String = "",      // 상품명
    var checked: Boolean = false, // 체크 여부
    var userId: String = ""     // 사용자 계정 ID (추가됨)
)