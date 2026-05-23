package com.gracecode.network

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _currentUserFlow = MutableStateFlow<User?>(null)
    val currentUserFlow: StateFlow<User?> = _currentUserFlow

    init {
        // Sync the current user's profile automatically whenever authentication state changes
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                fetchUserProfile(firebaseUser.uid)
            } else {
                _currentUserFlow.value = null
            }
        }
    }

    private fun fetchUserProfile(uid: String) {
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val user = document.toObject(User::class.java)
                    _currentUserFlow.value = user
                } else {
                    val newUser = User(
                        id = uid,
                        uid = uid,
                        displayName = auth.currentUser?.displayName ?: "User",
                        email = auth.currentUser?.email ?: "",
                        phoneNumber = auth.currentUser?.phoneNumber ?: "",
                        photoURL = auth.currentUser?.photoUrl?.toString() ?: "",
                        bio = "Hi there! I am using GraceCode.",
                        expertise = "Contributor",
                        skills = emptyList()
                    )
                    db.collection("users").document(uid).set(newUser)
                    _currentUserFlow.value = newUser
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching user profile, using fallback", exception)
                // Fallback to basic info from Auth if Firestore fails
                val fallbackUser = User(
                    id = uid,
                    uid = uid,
                    displayName = auth.currentUser?.displayName ?: "User",
                    email = auth.currentUser?.email ?: "",
                    phoneNumber = auth.currentUser?.phoneNumber ?: "",
                    photoURL = auth.currentUser?.photoUrl?.toString() ?: ""
                )
                _currentUserFlow.value = fallbackUser
            }
    }

    fun getUserProfile(uid: String, onSuccess: (User) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                val user = document?.toObject(User::class.java)
                if (user != null) {
                    onSuccess(user)
                } else {
                    onFailure(Exception("Profile data not found"))
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun verifyPhoneNumber(
        phoneNumber: String,
        activity: Activity,
        onCodeSent: (String, PhoneAuthProvider.ForceResendingToken) -> Unit,
        onVerificationCompleted: (PhoneAuthCredential) -> Unit,
        onVerificationFailed: (FirebaseException) -> Unit
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    onVerificationCompleted(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    onVerificationFailed(e)
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    onCodeSent(verificationId, token)
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun signInWithPhoneCredential(credential: PhoneAuthCredential, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onFailure(it)
            }
    }

    fun signUp(
        email: String,
        password: String,
        displayName: String,
        expertise: String,
        skills: List<String>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: ""
                val newUser = User(
                    id = uid,
                    uid = uid,
                    displayName = displayName,
                    email = email,
                    photoURL = "", // Can be updated later
                    expertise = expertise,
                    skills = skills
                )
                db.collection("users").document(uid).set(newUser)
                    .addOnSuccessListener {
                        _currentUserFlow.value = newUser
                        onSuccess()
                    }
                    .addOnFailureListener { exception ->
                        onFailure(exception)
                    }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun signOut() {
        auth.signOut()
        _currentUserFlow.value = null
    }

    fun createProject(project: Project, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val currentUid = auth.currentUser?.uid ?: return onFailure(Exception("User not authenticated"))
        val currentUserName = _currentUserFlow.value?.displayName ?: "Collaborator"

        val finalProject = project.copy(creatorId = currentUid, creatorName = currentUserName)
        db.collection("projects")
            .add(finalProject)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onFailure(it)
            }
    }

    fun getProjects(onSuccess: (List<Project>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("projects")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val projects = querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Project::class.java)
                }
                onSuccess(projects)
            }
            .addOnFailureListener {
                onFailure(it)
            }
    }

    fun createPost(post: Post, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val currentUid = auth.currentUser?.uid ?: return onFailure(Exception("User not authenticated"))
        val currentUserName = _currentUserFlow.value?.displayName ?: "User"

        val finalPost = post.copy(authorId = currentUid, authorName = currentUserName)
        db.collection("posts")
            .add(finalPost)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onFailure(it)
            }
    }

    fun getPosts(onSuccess: (List<Post>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val posts = querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Post::class.java)
                }
                onSuccess(posts)
            }
            .addOnFailureListener {
                onFailure(it)
            }
    }
}