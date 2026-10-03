package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.CategoryEntity

object CategoryLabelResolver {

    private sealed interface Resolution {
        data class ParentLookup(val categoryName: String, val parentCategoryName: String) : Resolution
        data class TypeLookup(val categoryName: String, val type: String) : Resolution
    }

    private val collisionMap: Map<String, Resolution> = mapOf(
        "Home Maintenance" to Resolution.ParentLookup(categoryName = "Maintenance", parentCategoryName = "Housing"),
        "Vehicle Maintenance" to Resolution.ParentLookup(categoryName = "Maintenance", parentCategoryName = "Vehicle"),
        "Gifts (Expense)" to Resolution.TypeLookup(categoryName = "Gifts", type = "EXPENSE"),
        "Gifts (Income)" to Resolution.TypeLookup(categoryName = "Gifts", type = "INCOME"),
        "Other (Expense)" to Resolution.TypeLookup(categoryName = "Other", type = "EXPENSE"),
        "Other (Income)" to Resolution.TypeLookup(categoryName = "Other", type = "INCOME"),
    )

    fun resolve(label: String, categories: List<CategoryEntity>): CategoryEntity? {
        val resolution = collisionMap[label]
        return when (resolution) {
            is Resolution.ParentLookup -> {
                val parentId = categories.find { it.name == resolution.parentCategoryName && it.parentCategoryId == null }?.id
                    ?: return null
                categories.find { it.name == resolution.categoryName && it.parentCategoryId == parentId }
            }
            is Resolution.TypeLookup -> {
                categories.find { it.name == resolution.categoryName && it.type == resolution.type }
            }
            null -> categories.find { it.name == label }
        }
    }
}
