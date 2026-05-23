package com.gracecode.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProjectsScreen() {
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        FirebaseManager.getProjects(
            onSuccess = {
                projects = it
                isLoading = false
            },
            onFailure = { isLoading = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "GraceCode Projects",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(projects) { project ->
                    ProjectItem(project)
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create Project")
        }

        if (showCreateDialog) {
            CreateProjectDialog(
                onDismiss = { showCreateDialog = false },
                onProjectCreated = {
                    showCreateDialog = false
                    isLoading = true
                    FirebaseManager.getProjects(
                        onSuccess = {
                            projects = it
                            isLoading = false
                        },
                        onFailure = { isLoading = false }
                    )
                }
            )
        }
    }
}

@Composable
fun ProjectItem(project: Project) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (project.category == "wellbeing") Icons.Default.CheckCircle else Icons.Default.Public,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = project.category.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    color = when(project.status) {
                        "active" -> Color(0xFFD1FAE5)
                        "completed" -> Color(0xFFDBEAFE)
                        else -> Color(0xFFF3F4F6)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = project.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        color = when(project.status) {
                            "active" -> Color(0xFF065F46)
                            "completed" -> Color(0xFF1E40AF)
                            else -> Color(0xFF374151)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = project.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = project.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Architecture Goal:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text(text = project.architecture, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Proposed by ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text(text = project.creatorName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CreateProjectDialog(onDismiss: () -> Unit, onProjectCreated: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("wellbeing") }
    var architecture by remember { mutableStateOf("Next.js + Firebase + Gemini API") }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Proposal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Project Title") })
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Impact Description") },
                    modifier = Modifier.height(80.dp)
                )
                
                Text("Category", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = category == "wellbeing",
                        onClick = { category = "wellbeing" },
                        label = { Text("Wellbeing") }
                    )
                    FilterChip(
                        selected = category == "environment",
                        onClick = { category = "environment" },
                        label = { Text("Environment") }
                    )
                }

                OutlinedTextField(
                    value = architecture,
                    onValueChange = { architecture = it },
                    label = { Text("Architecture Goal") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSubmitting = true
                    val project = Project(
                        name = name,
                        description = description,
                        category = category,
                        architecture = architecture
                    )
                    FirebaseManager.createProject(project,
                        onSuccess = {
                            isSubmitting = false
                            onProjectCreated()
                        },
                        onFailure = { isSubmitting = false }
                    )
                },
                enabled = name.isNotEmpty() && description.isNotEmpty() && !isSubmitting
            ) {
                if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                else Text("Propose")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
