package com.gracecode.network

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gracecode.network.ui.theme.WeColabaiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeColabaiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppShell()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell() {
    // Collect Auth Flow to observe State transitions
    val currentUser by FirebaseManager.currentUserFlow.collectAsState()
    val context = LocalContext.current

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                Toast.makeText(context, "Welcome!", Toast.LENGTH_SHORT).show()
            }
        )
    } else {
        MainDashboard(user = currentUser!!)
    }
}

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var expertise by remember { mutableStateOf("") }
    var skillsInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isSignUp) "Register for WeColabai" else "Login to WeColabai",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        if (isSignUp) {
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = expertise,
                onValueChange = { expertise = it },
                label = { Text("Expertise (e.g., UI/UX, Android)") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = skillsInput,
                onValueChange = { skillsInput = it },
                label = { Text("Skills (comma-separated)") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else {
            Button(
                onClick = {
                    isLoading = true
                    if (isSignUp) {
                        val skillsList = skillsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        FirebaseManager.signUp(
                            email, password, displayName, expertise, skillsList,
                            onSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onFailure = {
                                isLoading = false
                            }
                        )
                    } else {
                        FirebaseManager.signIn(
                            email, password,
                            onSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onFailure = {
                                isLoading = false
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(if (isSignUp) "Register" else "Log In")
            }

            TextButton(
                onClick = { isSignUp = !isSignUp },
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(if (isSignUp) "Already have an account? Sign In" else "New to the platform? Create Account")
            }
        }
    }
}

@Composable
fun MainDashboard(user: User) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Projects", "Community", "Profile")

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabTitles.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(title) },
                        icon = {
                            Icon(
                                imageVector = when (index) {
                                    0 -> Icons.Default.Work
                                    1 -> Icons.Default.Forum
                                    else -> Icons.Default.Person
                                },
                                contentDescription = title
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> ProjectTab()
                1 -> CommunityTab()
                2 -> ProfileTab(user = user)
            }
        }
    }
}

@Composable
fun ProjectTab() {
    // Explicit type mapping prevents "Cannot infer type" error
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadProjects() {
        isLoading = true
        FirebaseManager.getProjects(
            onSuccess = { list ->
                projects = list
                isLoading = false
            },
            onFailure = {
                isLoading = false
            }
        )
    }

    LaunchedEffect(Unit) {
        loadProjects()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Collaborative Projects", style = MaterialTheme.typography.titleLarge)
                    Button(onClick = { showDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Project")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                }

                if (projects.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Text("No active projects. Start one now!", modifier = Modifier.align(Alignment.Center))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(projects) { project ->
                            ProjectCard(project)
                        }
                    }
                }
            }
        }

        // COMPOSE DIALOG (Ensures Composable context remains safe in State triggers)
        if (showDialog) {
            AddProjectDialog(
                onDismiss = { showDialog = false },
                onProjectCreated = {
                    showDialog = false
                    loadProjects()
                }
            )
        }
    }
}

@Composable
fun ProjectCard(project: Project) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(project.name, style = MaterialTheme.typography.titleMedium)
            Text("Focus: ${project.focus}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(project.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(12.dp))

            Text("Creator: ${project.creatorName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            if (project.requiredSkills.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Wanted: ${project.requiredSkills.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProjectDialog(onDismiss: () -> Unit, onProjectCreated: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var focus by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var skillsInput by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        title = { Text("New Collaboration Project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = focus,
                    onValueChange = { focus = it },
                    label = { Text("Focus Area / Tagline") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
                OutlinedTextField(
                    value = skillsInput,
                    onValueChange = { skillsInput = it },
                    label = { Text("Wanted Skills (comma-separated)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && description.isNotBlank() && !isSending,
                onClick = {
                    isSending = true
                    val skills = skillsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val newProject = Project(
                        name = name,
                        focus = focus,
                        description = description,
                        requiredSkills = skills
                    )
                    FirebaseManager.createProject(newProject,
                        onSuccess = {
                            isSending = false
                            onProjectCreated()
                        },
                        onFailure = {
                            isSending = false
                        }
                    )
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSending,
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CommunityTab() {
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var postContent by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    fun loadPosts() {
        isLoading = true
        FirebaseManager.getPosts(
            onSuccess = { list ->
                posts = list
                isLoading = false
            },
            onFailure = {
                isLoading = false
            }
        )
    }

    LaunchedEffect(Unit) {
        loadPosts()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Community Forum", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))

        // Shared composing card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = postContent,
                    onValueChange = { postContent = it },
                    placeholder = { Text("Ask or share ideas with WeColabai community...") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 5
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        isSubmitting = true
                        val newPost = Post(content = postContent)
                        FirebaseManager.createPost(newPost,
                            onSuccess = {
                                postContent = ""
                                isSubmitting = false
                                loadPosts()
                            },
                            onFailure = {
                                isSubmitting = false
                            }
                        )
                    },
                    enabled = postContent.isNotBlank() && !isSubmitting,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Share")
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(posts) { post ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(post.authorName, style = MaterialTheme.typography.titleSmall)
                                Text("Shared recently", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(post.content, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileTab(user: User) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Profile Pic",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(user.displayName, style = MaterialTheme.typography.headlineSmall)
        Text(user.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Role: ${user.expertise}", style = MaterialTheme.typography.titleSmall)
                if (user.bio.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(user.bio, style = MaterialTheme.typography.bodyMedium)
                }
                if (user.skills.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("My Skills: ${user.skills.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { FirebaseManager.signOut() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = "Log Out")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Out")
        }
    }
}