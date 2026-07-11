// FILE: app/src/main/java/com/example/pulse/screens/JournalEditorScreen.kt
package com.example.pulse.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pulse.data.JournalRepository
import com.example.pulse.util.MarkdownVisualTransformation
import com.example.pulse.viewmodel.JournalEditorViewModel
import com.example.pulse.viewmodel.JournalEditorViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEditorScreen(
    fileName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: JournalEditorViewModel = viewModel(
        factory = JournalEditorViewModelFactory(context, fileName)
    )

    val fieldValue by viewModel.fieldValue.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val entryMeta by viewModel.entryMeta.collectAsStateWithLifecycle()

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val markdownTransform = remember { MarkdownVisualTransformation() }

    val wordCount = remember(fieldValue.text) {
        fieldValue.text.trim().let { if (it.isEmpty()) 0 else it.split(Regex("\\s+")).size }
    }
    val charCount = fieldValue.text.length

    BackHandler { viewModel.save { onBack() } }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Move to Trash?") },
            text = { Text("This entry will be moved to Trash. You can restore it later.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete { onBack() }
                }) { Text("Move to Trash") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(JournalRepository(context).formatFileNameToLabel(fileName)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.save { onBack() } }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.padding(12.dp).size(20.dp))
                    } else {
                        IconButton(onClick = { viewModel.save { onBack() } }) {
                            Icon(Icons.Default.Check, contentDescription = "Save and close")
                        }
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Archive") },
                            leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                viewModel.archive { onBack() }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move to Trash") },
                            onClick = {
                                showMenu = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = fieldValue,
                onValueChange = { viewModel.onTextChange(it) },
                visualTransformation = markdownTransform,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Write in markdown...") },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                )
            )

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
                Text(
                    "$wordCount words · $charCount characters",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
                Text(
                    "Created ${entryMeta.createdLabel} · Edited ${entryMeta.editedLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1
                )
            }

            Divider(modifier = Modifier.padding(top = 8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp)
            ) {
                IconButton(onClick = { viewModel.undo() }, enabled = canUndo) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                }
                IconButton(onClick = { viewModel.redo() }, enabled = canRedo) {
                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                }
                IconButton(onClick = { viewModel.toggleBold() }) {
                    Icon(Icons.Default.FormatBold, contentDescription = "Bold")
                }
                IconButton(onClick = { viewModel.toggleItalic() }) {
                    Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
                }
                IconButton(onClick = { viewModel.toggleUnderline() }) {
                    Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline")
                }
                IconButton(onClick = { viewModel.insertHeading() }) {
                    Icon(Icons.Default.Title, contentDescription = "Heading")
                }
                IconButton(onClick = { viewModel.insertBulletList() }) {
                    Icon(Icons.Default.FormatListBulleted, contentDescription = "Bullet list")
                }
                IconButton(onClick = { viewModel.insertNumberedList() }) {
                    Icon(Icons.Default.FormatListNumbered, contentDescription = "Numbered list")
                }
                IconButton(onClick = { viewModel.insertCheckbox() }) {
                    Icon(Icons.Default.CheckBox, contentDescription = "Checklist item")
                }
            }
        }
    }
}