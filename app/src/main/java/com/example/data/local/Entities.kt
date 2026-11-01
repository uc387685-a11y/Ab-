package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"])]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val sender: String, // "user", "assistant", "system"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "sent", // "sent", "loading", "error"
    val actionType: String? = null, // "app_launched", "contact_call", "search", "timer", "photo_share", "offline_tip"
    val actionData: String? = null // metadata e.g. package name, phone number, query
)
