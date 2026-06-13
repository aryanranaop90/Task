package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false,
    val points: Int = 10,
    val isPremium: Boolean = false, // Locked until watched ad
    val createdAt: Long = System.currentTimeMillis()
)
