package com.makemission.makeflow.ui.state

import com.makemission.makeflow.data.local.entity.TaskEntity

data class TaskUiState(
    val isLoading: Boolean = true,
    val tasks: List<TaskEntity> = emptyList(),
    val completedCount: Int = 0,
    val errorMessage: String? = null
)
