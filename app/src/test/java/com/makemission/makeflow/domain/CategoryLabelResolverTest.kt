package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.CategoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryLabelResolverTest {

    private val housingParent = CategoryEntity(
        id = "housing_id", name = "Housing", type = "EXPENSE",
        parentCategoryId = null, iconName = "home", colorHex = "#42A5F5"
    )
    private val vehicleParent = CategoryEntity(
        id = "vehicle_id", name = "Vehicle", type = "EXPENSE",
        parentCategoryId = null, iconName = "directions_car", colorHex = "#D4E157"
    )
    private val maintenanceHousing = CategoryEntity(
        id = "maint_housing_id", name = "Maintenance", type = "EXPENSE",
        parentCategoryId = housingParent.id, iconName = "build", colorHex = "#0288D1"
    )
    private val maintenanceVehicle = CategoryEntity(
        id = "maint_vehicle_id", name = "Maintenance", type = "EXPENSE",
        parentCategoryId = vehicleParent.id, iconName = "build_circle", colorHex = "#F9A825"
    )

    private val giftsExpense = CategoryEntity(
        id = "gifts_exp_id", name = "Gifts", type = "EXPENSE",
        parentCategoryId = "generosity_id", iconName = "card_giftcard", colorHex = "#F06292"
    )
    private val giftsIncome = CategoryEntity(
        id = "gifts_inc_id", name = "Gifts", type = "INCOME",
        parentCategoryId = null, iconName = "card_giftcard", colorHex = "#EC407A"
    )

    private val otherExpense = CategoryEntity(
        id = "other_exp_id", name = "Other", type = "EXPENSE",
        parentCategoryId = null, iconName = "more_horiz", colorHex = "#9E9E9E"
    )
    private val otherIncome = CategoryEntity(
        id = "other_inc_id", name = "Other", type = "INCOME",
        parentCategoryId = null, iconName = "more_horiz", colorHex = "#9E9E9E"
    )

    private val restaurant = CategoryEntity(
        id = "restaurant_id", name = "Restaurant", type = "EXPENSE",
        parentCategoryId = "food_id", iconName = "restaurant", colorHex = "#FF8A65"
    )

    private val allCategories = listOf(
        housingParent, vehicleParent,
        maintenanceHousing, maintenanceVehicle,
        giftsExpense, giftsIncome,
        otherExpense, otherIncome,
        restaurant
    )

    @Test
    fun homeMaintenance_resolvesToHousingChild() {
        val result = CategoryLabelResolver.resolve("Home Maintenance", allCategories)
        assertNotNull(result)
        assertEquals("maint_housing_id", result!!.id)
        assertEquals(housingParent.id, result.parentCategoryId)
    }

    @Test
    fun vehicleMaintenance_resolvesToVehicleChild() {
        val result = CategoryLabelResolver.resolve("Vehicle Maintenance", allCategories)
        assertNotNull(result)
        assertEquals("maint_vehicle_id", result!!.id)
        assertEquals(vehicleParent.id, result.parentCategoryId)
    }

    @Test
    fun giftsExpenseLabel_resolvesToExpenseGifts() {
        val result = CategoryLabelResolver.resolve("Gifts (Expense)", allCategories)
        assertNotNull(result)
        assertEquals("gifts_exp_id", result!!.id)
        assertEquals("EXPENSE", result.type)
    }

    @Test
    fun giftsIncomeLabel_resolvesToIncomeGifts() {
        val result = CategoryLabelResolver.resolve("Gifts (Income)", allCategories)
        assertNotNull(result)
        assertEquals("gifts_inc_id", result!!.id)
        assertEquals("INCOME", result.type)
    }

    @Test
    fun otherExpenseLabel_resolvesToExpenseOther() {
        val result = CategoryLabelResolver.resolve("Other (Expense)", allCategories)
        assertNotNull(result)
        assertEquals("other_exp_id", result!!.id)
        assertEquals("EXPENSE", result.type)
    }

    @Test
    fun otherIncomeLabel_resolvesToIncomeOther() {
        val result = CategoryLabelResolver.resolve("Other (Income)", allCategories)
        assertNotNull(result)
        assertEquals("other_inc_id", result!!.id)
        assertEquals("INCOME", result.type)
    }

    @Test
    fun nonCollisionLabel_usesSimpleNameMatch() {
        val result = CategoryLabelResolver.resolve("Restaurant", allCategories)
        assertNotNull(result)
        assertEquals("restaurant_id", result!!.id)
    }

    @Test
    fun unknownLabel_returnsNull() {
        val result = CategoryLabelResolver.resolve("NonExistentCategory", allCategories)
        assertNull(result)
    }

    @Test
    fun homeMaintenanceWithoutParent_returnsNull() {
        val noHousing = allCategories.filter { it.id != housingParent.id && it.id != maintenanceHousing.id }
        val result = CategoryLabelResolver.resolve("Home Maintenance", noHousing)
        assertNull(result)
    }

    @Test
    fun giftsIncomeWithOnlyExpenseGifts_returnsNull() {
        val noIncomeGifts = allCategories.filter { it.id != giftsIncome.id }
        val result = CategoryLabelResolver.resolve("Gifts (Income)", noIncomeGifts)
        assertNull(result)
    }
}
