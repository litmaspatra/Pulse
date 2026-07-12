package com.example.pulse.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SecurityQuestionSetupScreen(
    onCompleted: (String, String) -> Unit
) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Security Question") }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Set a security question to recover your PIN if you forget it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("Security question") },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it },
                label = { Text("Your answer") },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                singleLine = true
            )
            Button(
                onClick = { onCompleted(question.trim(), answer.trim()) },
                enabled = question.isNotBlank() && answer.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
            ) { Text("Continue") }
        }
    }
}