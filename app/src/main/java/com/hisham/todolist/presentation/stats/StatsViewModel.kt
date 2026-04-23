package com.hisham.todolist.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisham.todolist.domain.usecase.CalculateDailyAverageUseCase
import com.hisham.todolist.domain.usecase.CalculateWeeklyEffectivenessUseCase
import com.hisham.todolist.domain.usecase.GetMonthlyCompletedTasksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatsUiState(
    val weeklyEffectiveness: Int = 0,
    val monthlyCompletedTasks: Int = 0,
    val dailyAverage: Double = 0.0,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    calculateWeeklyEffectivenessUseCase: CalculateWeeklyEffectivenessUseCase,
    getMonthlyCompletedTasksUseCase: GetMonthlyCompletedTasksUseCase,
    calculateDailyAverageUseCase: CalculateDailyAverageUseCase,
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        calculateWeeklyEffectivenessUseCase(),
        getMonthlyCompletedTasksUseCase(),
        calculateDailyAverageUseCase(),
    ) { weeklyEffectiveness, monthlyCompletedTasks, dailyAverage ->
        StatsUiState(
            weeklyEffectiveness = weeklyEffectiveness,
            monthlyCompletedTasks = monthlyCompletedTasks,
            dailyAverage = dailyAverage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = StatsUiState(),
    )
}
