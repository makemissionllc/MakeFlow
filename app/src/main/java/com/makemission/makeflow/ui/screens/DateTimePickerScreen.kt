package com.makemission.makeflow.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerScreen(
    initialTimestamp: Long,
    onDateTimeSelected: (Long) -> Unit,
    onBack: () -> Unit
) {
    val initialCalendar = remember {
        Calendar.getInstance().apply { timeInMillis = initialTimestamp }
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialTimestamp)
    val timePickerState = rememberTimePickerState(
        initialHour = initialCalendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = initialCalendar.get(Calendar.MINUTE),
        is24Hour = false
    )

    val displayFormat = remember { SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()) }

    val combinedMillis = remember {
        val cal = Calendar.getInstance()
        cal.timeInMillis = datePickerState.selectedDateMillis ?: initialTimestamp
        cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
        cal.set(Calendar.MINUTE, timePickerState.minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121315))
    ) {
        Spacer(modifier = Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding()))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "Date & Time",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .background(Color(0xFF1B1C1F), RoundedCornerShape(16.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayFormat.format(Date(combinedMillis)),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DatePicker(
                    state = datePickerState,
                    colors = DatePickerDefaults.colors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        headlineContentColor = Color.White,
                        weekdayContentColor = Color.White.copy(alpha = 0.6f),
                        subheadContentColor = Color.White.copy(alpha = 0.7f),
                        yearContentColor = Color.White,
                        currentYearContentColor = Color.White,
                        selectedYearContentColor = Color(0xFF121315),
                        dayContentColor = Color.White,
                        selectedDayContentColor = Color(0xFF121315),
                        selectedDayContainerColor = Color.White,
                        todayContentColor = Color(0xFFFFA726),
                        todayDateBorderColor = Color(0xFFFFA726),
                        dayInSelectionRangeContentColor = Color.White,
                        dayInSelectionRangeContainerColor = Color(0xFF2C2C2E)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                TimePicker(
                    state = timePickerState,
                    colors = androidx.compose.material3.TimePickerDefaults.colors(
                        clockDialColor = Color(0xFF1B1C1F),
                        selectorColor = Color.White,
                        clockDialSelectedContentColor = Color(0xFF121315),
                        clockDialUnselectedContentColor = Color.White.copy(alpha = 0.7f),
                        timeSelectorSelectedContainerColor = Color.White,
                        timeSelectorSelectedContentColor = Color(0xFF121315),
                        timeSelectorUnselectedContainerColor = Color(0xFF1B1C1F),
                        timeSelectorUnselectedContentColor = Color.White.copy(alpha = 0.6f)
                    )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp)
            }
            Button(
                onClick = {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = datePickerState.selectedDateMillis ?: initialTimestamp
                    cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    cal.set(Calendar.MINUTE, timePickerState.minute)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    onDateTimeSelected(cal.timeInMillis)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF121315)
                )
            ) {
                Text("Save", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
