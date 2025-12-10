package com.example.distributedtaskboard

data class Task(
    val id: Int,
    val description: String,
    var isCompleted: Boolean = false,
    val lastModifiedBy: String = ""
)
