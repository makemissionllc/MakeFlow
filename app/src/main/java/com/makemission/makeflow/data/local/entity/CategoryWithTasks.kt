package com.makemission.makeflow.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class CategoryWithTasks(
    @Embedded val category: CategoryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "categoryId"
    )
    val tasks: List<TaskEntity>
)
