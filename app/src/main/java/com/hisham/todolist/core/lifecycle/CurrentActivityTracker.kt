package com.hisham.todolist.core.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrentActivityTracker @Inject constructor() : Application.ActivityLifecycleCallbacks {

    @Volatile
    private var currentActivityReference: WeakReference<ComponentActivity>? = null

    fun currentActivity(): ComponentActivity? = currentActivityReference?.get()

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityResumed(activity: Activity) {
        if (activity is ComponentActivity) {
            currentActivityReference = WeakReference(activity)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        val currentActivity = currentActivityReference?.get()
        if (currentActivity === activity) {
            currentActivityReference?.clear()
            currentActivityReference = null
        }
    }

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) {
        val currentActivity = currentActivityReference?.get()
        if (currentActivity === activity) {
            currentActivityReference?.clear()
            currentActivityReference = null
        }
    }
}
