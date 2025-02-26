package com.ufersa.myapplication.model

import com.google.firebase.Timestamp

data class Post(
    val userId: String? = null,
    val userName: String? = null,
    val imageUrl: String? = null,
    val description: String? = null,
    val timestamp: Timestamp? = null,
    val title: String? = null,
    val profileImageUrl: String? = null
)