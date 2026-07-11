package com.example.pulse.viewmodel

import android.content.Context
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pulse.data.JournalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JournalEditorViewModel(
    private val repository: JournalRepository,
    val fileName: String
) : ViewModel() {

    private val _fieldValue = MutableStateFlow(TextFieldValue(""))
    val fieldValue: StateFlow<TextFieldValue> = _fieldValue.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()
    private var lastSnapshot = ""
    private var snapshotJob: Job? = null
    private var hasUnsavedChanges = false

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val text = repository.readEntry(fileName)
            _fieldValue.value = TextFieldValue(text, selection = TextRange(text.length))
            lastSnapshot = text
        }
    }

    fun onTextChange(newValue: TextFieldValue) {
        val oldText = _fieldValue.value.text
        _fieldValue.value = newValue
        hasUnsavedChanges = true

        if (newValue.text != oldText) {
            snapshotJob?.cancel()
            snapshotJob = viewModelScope.launch {
                delay(500)
                pushUndoSnapshot(oldText)
            }
        }
    }

    private fun pushUndoSnapshot(text: String) {
        if (text == lastSnapshot) return
        undoStack.addLast(lastSnapshot)
        if (undoStack.size > 100) undoStack.removeFirst()
        lastSnapshot = text
        redoStack.clear()
        _canUndo.value = true
        _canRedo.value = false
    }

    fun undo() {
        snapshotJob?.cancel()
        if (undoStack.isEmpty()) return
        val current = _fieldValue.value.text
        val previous = undoStack.removeLast()
        redoStack.addLast(current)
        lastSnapshot = previous
        _fieldValue.value = TextFieldValue(previous, selection = TextRange(previous.length))
        hasUnsavedChanges = true
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = true
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = _fieldValue.value.text
        val next = redoStack.removeLast()
        undoStack.addLast(current)
        lastSnapshot = next
        _fieldValue.value = TextFieldValue(next, selection = TextRange(next.length))
        hasUnsavedChanges = true
        _canUndo.value = true
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun toggleBold() = wrapSelection("**", "**")
    fun toggleItalic() = wrapSelection("_", "_")
    fun insertCheckbox() = insertAtLineStart("- [ ] ")
    fun insertHeading() = insertAtLineStart("## ")

    private fun wrapSelection(prefix: String, suffix: String) {
        val current = _fieldValue.value
        val text = current.text
        val start = current.selection.start
        val end = current.selection.end

        pushUndoSnapshot(text)

        val newText = text.substring(0, start) + prefix + text.substring(start, end) + suffix + text.substring(end)
        val newCursor = if (start == end) start + prefix.length else end + prefix.length + suffix.length

        _fieldValue.value = TextFieldValue(newText, selection = TextRange(newCursor))
        lastSnapshot = newText
        hasUnsavedChanges = true
    }

    private fun insertAtLineStart(prefix: String) {
        val current = _fieldValue.value
        val text = current.text
        val cursor = current.selection.start

        pushUndoSnapshot(text)

        val newlineIndex = if (cursor == 0) -1 else text.lastIndexOf('\n', cursor - 1)
        val lineStart = if (newlineIndex == -1) 0 else newlineIndex + 1

        val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
        val newCursor = cursor + prefix.length

        _fieldValue.value = TextFieldValue(newText, selection = TextRange(newCursor))
        lastSnapshot = newText
        hasUnsavedChanges = true
    }

    fun save(onSaved: () -> Unit = {}) {
        if (!hasUnsavedChanges) {
            onSaved()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isSaving.value = true
            repository.saveEntry(fileName, _fieldValue.value.text)
            hasUnsavedChanges = false
            _isSaving.value = false
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteEntry(fileName)
            onDeleted()
        }
    }
}

class JournalEditorViewModelFactory(
    private val context: Context,
    private val fileName: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return JournalEditorViewModel(JournalRepository(context), fileName) as T
    }
}