package com.hisham.todolist

import android.app.Application
import com.hisham.todolist.core.lifecycle.CurrentActivityTracker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TodoListApp : Application() {

    @Inject
    lateinit var currentActivityTracker: CurrentActivityTracker

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(currentActivityTracker)
    }
}
