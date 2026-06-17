package com.makemission.makeflow.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.makemission.makeflow.data.local.AppDatabase
import com.makemission.makeflow.data.local.entity.TaskEntity
import com.makemission.makeflow.data.repository.TaskRepository
import com.makemission.makeflow.data.repository.TaskRepositoryImpl
import com.makemission.makeflow.ui.state.TaskUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = TaskRepositoryImpl(db.taskDao())
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            repository.getAllTasks()
                .map { tasks ->
                    TaskUiState(
                        isLoading = false,
                        tasks = tasks,
                        completedCount = tasks.count { it.isCompleted }
                    )
                }
                .catch { e ->
                    _uiState.value = TaskUiState(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load tasks"
                    )
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun onAddTask(title: String, description: String) {
        viewModelScope.launch {
            val task = TaskEntity(
                title = title.trim(),
                description = description.trim()
            )
            repository.insert(task)
        }
    }

    fun onToggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            repository.update(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun onDeleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.delete(task)
        }
    }
}
