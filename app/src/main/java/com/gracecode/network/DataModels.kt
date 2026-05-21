package com.gracecode.network

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val bio: String = "",
    val expertise: String = "",
    val skills: List<String> = emptyList()
)

data class Project(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val focus: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val requiredSkills: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class Post(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)