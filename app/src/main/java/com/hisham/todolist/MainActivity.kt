package com.hisham.todolist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisham.todolist.core.designsystem.TodoListTheme
import com.hisham.todolist.presentation.AppStateViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appStateViewModel: AppStateViewModel = hiltViewModel()
            val themeMode by appStateViewModel.themeMode.collectAsStateWithLifecycle()

            TodoListTheme(themeMode = themeMode) {
                TodoListRoot(appStateViewModel = appStateViewModel)
            }
        }
    }
}
