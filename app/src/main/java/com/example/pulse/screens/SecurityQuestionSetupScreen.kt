package com.example.pulse.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val presetQuestions = listOf(
    "What was your childhood nickname?",
    "What is the name of your first school?",
    "What is your favorite book?",
    "What city were you born in?",
    "What was the name of your first pet?"
)

@Composable
fun SecurityQuestionSetupScreen(
    onCompleted: (question: String, answer: String) -> Unit
) {
    var selectedQuestion by remember { mutableStateOf(presetQuestions.first()) }
    var answer by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Set up a security question", style = MaterialTheme.typography.titleMedium)
        Text(
            "Used only if you forget your PIN.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Column(modifier = Modifier.padding(top = 16.dp)) {
            presetQuestions.forEach { question ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = question == selectedQuestion,
                            onClick = { selectedQuestion = question }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = question == selectedQuestion, onClick = { selectedQuestion = question })
                    Text(question, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it },
            label = { Text("Your answer") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )

        Button(
            onClick = { onCompleted(selectedQuestion, answer.trim()) },
            enabled = answer.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
        ) { Text("Continue") }
    }
}