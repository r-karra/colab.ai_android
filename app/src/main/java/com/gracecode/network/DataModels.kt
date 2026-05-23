package com.gracecode.network

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val photoURL: String = "",
    val bio: String = "",
    val expertise: String = "", // e.g., Frontend, Backend, AI/ML, UI/UX
    val skills: List<String> = emptyList()
)

data class PrivateUserInfo(
    val email: String = "",
    val isVerified: Boolean = false
)

data class Project(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "wellbeing", // wellbeing, environment
    val architecture: String = "Next.js + Firebase + Gemini API",
    val status: String = "planned", // active, completed, planned
    val creatorId: String = "",
    val creatorName: String = "",
    val assignedDevelopers: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class Post(
    @DocumentId val id: String = "",
    val type: String = "social", // social, proposal
    val content: String = "",
    val problem: String = "", // for proposals
    val solution: String = "", // for proposals
    val prototypeUrl: String = "", // for proposals
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoURL: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0
)
