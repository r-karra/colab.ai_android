package com.gracecode.network

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var showProposalDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTab) {
        isLoading = true
        FirebaseManager.getPosts(
            onSuccess = { allPosts ->
                posts = if (selectedTab == 1) {
                    allPosts.filter { it.type == "proposal" }
                } else {
                    allPosts
                }
                isLoading = false
            },
            onFailure = { isLoading = false }
        )
    }

    Scaffold(
        topBar = {
            SecondaryTabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("For You", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("Proposals", modifier = Modifier.padding(12.dp))
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showProposalDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Take a Stand")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(posts) { post ->
                        PostItem(post)
                    }
                }
            }
        }

        if (showProposalDialog) {
            ProposalDialog(
                onDismiss = { showProposalDialog = false },
                onProposalCreated = {
                    showProposalDialog = false
                    selectedTab = 1 // Switch to proposals tab
                }
            )
        }
    }
}

@Composable
fun PostItem(post: Post) {
    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
    val date = Date(post.timestamp)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = post.authorName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = post.authorName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = sdf.format(date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (post.type == "proposal") {
                    SuggestionChip(
                        onClick = { },
                        label = { Text("Stand", fontSize = 10.sp) },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (post.type == "proposal") {
                Text(
                    text = "World Problem:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = post.problem, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Proposed Solution:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = post.solution, style = MaterialTheme.typography.bodyMedium)
                if (post.prototypeUrl.isNotEmpty()) {
                    Text(
                        text = "Prototype: ${post.prototypeUrl}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                Text(text = post.content, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun ProposalDialog(onDismiss: () -> Unit, onProposalCreated: () -> Unit) {
    var problem by remember { mutableStateOf("") }
    var solution by remember { mutableStateOf("") }
    var prototypeUrl by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Take a Stand") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("What world problem do you want to solve?", style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(
                    value = problem,
                    onValueChange = { problem = it },
                    placeholder = { Text("Describe the problem...") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("What is your proposed solution?", style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(
                    value = solution,
                    onValueChange = { solution = it },
                    placeholder = { Text("Describe your solution...") },
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                )
                OutlinedTextField(
                    value = prototypeUrl,
                    onValueChange = { prototypeUrl = it },
                    label = { Text("Prototype URL (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSubmitting = true
                    val post = Post(
                        type = "proposal",
                        problem = problem,
                        solution = solution,
                        prototypeUrl = prototypeUrl
                    )
                    FirebaseManager.createPost(post,
                        onSuccess = {
                            isSubmitting = false
                            onProposalCreated()
                        },
                        onFailure = { isSubmitting = false }
                    )
                },
                enabled = problem.isNotEmpty() && solution.isNotEmpty() && !isSubmitting
            ) {
                if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                else Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
