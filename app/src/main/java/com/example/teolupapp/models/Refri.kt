package com.example.teolupapp.models

data class Refri(
    var key: String = "",       // Firebase Key (수정/삭제 시 필요)
    val name: String = "",      // 재료명
    val category: String = "",  // 카테고리
    val expiryDate: String = "",// 유통기한 (yyyy-MM-dd 형식의 String)
    val memo: String = "",      // 메모
    var userId: String = ""     // 사용자 계정 ID (추가됨)
)