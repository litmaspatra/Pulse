// FILE: app/src/main/java/com/example/pulse/viewmodel/JournalEditorViewModel.kt
package com.example.pulse.viewmodel

import android.content.Context
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pulse.data.EntryMeta
import com.example.pulse.data.JournalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val bulletLineRegex = Regex("^(- \\[ \\] |- \\[[xX]\\] |- )(.*)$")
private val numberedLineRegex = Regex("^(\\d+)\\. (.*)$")

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

    private val _entryMeta = MutableStateFlow(EntryMeta("", ""))
    val entryMeta: StateFlow<EntryMeta> = _entryMeta.asStateFlow()

    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()
    private var lastSnapshot = ""
    private var snapshotJob: Job? = null
    private var hasUnsavedChanges = false

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val text = repository.readEntry(fileName)
            val meta = repository.entryMeta(fileName)
            _fieldValue.value = TextFieldValue(text, selection = TextRange(text.length))
            _entryMeta.value = meta
            lastSnapshot = text
        }
    }

    fun onTextChange(newValue: TextFieldValue) {
        val oldText = _fieldValue.value.text
        val newText = newValue.text

        // Detect "user just pressed Enter" (one '\n' inserted at the cursor,
        // nothing else changed) so we can auto-continue bullet/numbered lists.
        val isSingleNewlineInsert =
            newText.length == oldText.length + 1 &&
                    newValue.selection.start == newValue.selection.end &&
                    newValue.selection.start > 0 &&
                    newText.getOrNull(newValue.selection.start - 1) == '\n' &&
                    newText.removeRange(newValue.selection.start - 1, newValue.selection.start) == oldText

        if (isSingleNewlineInsert) {
            val cursor = newValue.selection.start
            val prevLineStart = newText.lastIndexOf('\n', cursor - 2).let { if (it == -1) 0 else it + 1 }
            val prevLine = newText.substring(prevLineStart, cursor - 1)

            val bulletMatch = bulletLineRegex.find(prevLine)
            val numberedMatch = numberedLineRegex.find(prevLine)

            if (bulletMatch != null) {
                val marker = bulletMatch.groupValues[1]
                val content = bulletMatch.groupValues[2]
                if (content.isBlank()) {
                    // Empty list item + Enter = stop the list.
                    val cleared = newText.substring(0, prevLineStart) + newText.substring(cursor)
                    commit(cleared, prevLineStart)
                    return
                }
                val continuation = if (marker.startsWith("- [")) "- [ ] " else "- "
                val updated = newText.substring(0, cursor) + continuation + newText.substring(cursor)
                commit(updated, cursor + continuation.length, trackUndo = oldText)
                return
            }

            if (numberedMatch != null) {
                val num = numberedMatch.groupValues[1].toIntOrNull() ?: 1
                val content = numberedMatch.groupValues[2]
                if (content.isBlank()) {
                    val cleared = newText.substring(0, prevLineStart) + newText.substring(cursor)
                    commit(cleared, prevLineStart)
                    return
                }
                val continuation = "${num + 1}. "
                val updated = newText.substring(0, cursor) + continuation + newText.substring(cursor)
                commit(updated, cursor + continuation.length, trackUndo = oldText)
                return
            }
        }

        _fieldValue.value = newValue
        hasUnsavedChanges = true
        if (newText != oldText) {
            snapshotJob?.cancel()
            snapshotJob = viewModelScope.launch {
                delay(500)
                pushUndoSnapshot(oldText)
            }
        }
    }

    private fun commit(text: String, cursor: Int, trackUndo: String? = null) {
        if (trackUndo != null) pushUndoSnapshot(trackUndo)
        _fieldValue.value = TextFieldValue(text, selection = TextRange(cursor))
        lastSnapshot = text
        hasUnsavedChanges = true
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
    fun toggleUnderline() = wrapSelection("++", "++")
    fun insertCheckbox() = insertAtLineStart("- [ ] ")
    fun insertHeading() = insertAtLineStart("## ")
    fun insertBulletList() = insertAtLineStart("- ")
    fun insertNumberedList() = insertAtLineStart("1. ")

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
        viewModelScope.launch {
            _isSaving.value = true
            withContext(Dispatchers.IO) {
                repository.saveEntry(fileName, _fieldValue.value.text)
            }
            _entryMeta.value = repository.entryMeta(fileName)
            hasUnsavedChanges = false
            _isSaving.value = false
            onSaved()
        }
    }

    /** Moves the entry to Trash (soft delete) rather than deleting it outright. */
    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.moveToTrash(fileName)
            }
            onDeleted()
        }
    }

    fun archive(onArchived: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.archiveEntry(fileName)
            }
            onArchived()
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