/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.*

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application) {

        // Define viewModelJob and assign it an instance of Job
        private var viewModelJob = Job()

        // Override onCleared() for canceling all coroutines
        override fun onCleared() {
                super.onCleared()
                viewModelJob.cancel()
        }

        // Define a uiScope for the coroutines to run in (main thread)
        private val uiScope = CoroutineScope(Dispatchers.Main +  viewModelJob)

        // Create tonight MutableLiveData var to hold the current night
        private var tonight = MutableLiveData<SleepNight?>()

        // Define a variable, nights. Then getAllNights() from the database and assign to
        // the nights variable
        private val nights = database.getAllNights()

        /** Converted nights to Spanned for displaying **/
        // obs.: application resources gives us access to all string resources
        val nightsString = Transformations.map(nights) { nights ->
                formatNights(nights, application.resources)
        }

        /** If tonight has not been set, then the START button should be visible. **/
        val startButtonVisible = Transformations.map(tonight) {
                null == it
        }

        /** If tonight has been set, then the STOP button should be visible. **/
        val stopButtonVisible = Transformations.map(tonight) {
                null != it
        }

        /** If there are any nights in the database, show the CLEAR button. **/
        val clearButtonVisible = Transformations.map(nights) {
                it?.isNotEmpty()
        }

        /**
         * Request a toast by setting this value to true.
         *
         * This is private because we don't want to expose setting this value to the Fragment.
         */
        private var _showSnackbarEvent = MutableLiveData<Boolean>()

        /** If this is true, immediately `show()` a toast and call `doneShowingSnackbar()`. **/
        val showSnackBarEvent: LiveData<Boolean>
                get() = _showSnackbarEvent

        /**
         * Call this immediately after calling `show()` on a toast.
         *
         * It will clear the toast request, so if the user rotates their phone it won't show a duplicate
         * toast.
         */

        fun doneShowingSnackbar() {
                _showSnackbarEvent.value = false
        }

        /**
         * Variable that tells the Fragment to navigate to a specific [SleepQualityFragment]
         *
         * This is private because we don't want to expose setting this value to the Fragment.
         */
        private val _navigateToSleepQuality = MutableLiveData<SleepNight>()


        /** If this is non-null, immediately navigate to [SleepQualityFragment]
         * and call [doneNavigating] **/
        val navigateToSleepQuality: LiveData<SleepNight>
                get() = _navigateToSleepQuality

        /**
         * Call this immediately after navigating to [SleepQualityFragment]
         *
         * It will clear the navigation request, so if the user rotates their phone it won't
         * navigate twice.
         */
        fun doneNavigating() {
                _navigateToSleepQuality.value = null
        }

        /** Initialize the tonight variable (function initializeTonight()) **/
        init {
                initializeTonight()
        }

        // Implement initializeTonight()
        private fun initializeTonight() {
                // In the uiScope, launch a coroutine to avoid blocking the UI while waiting result
                // obs.: unless specified, the coroutine is schedule to execute immediately
                uiScope.launch {
                        tonight.value = getTonightFromDatabase()
                }
        }

        /**
         *  Handling the case of the stopped app or forgotten recording,
         *  the start and end times will be the same.
         *
         *  If the start time and end time are not the same, then we do not have an unfinished
         *  recording.
         */
        private suspend fun getTonightFromDatabase(): SleepNight? {
                // Creates a coroutine that runs in the Dispatchers.IO context and returns
                // its result
                return withContext(Dispatchers.IO) {
                        // Let the coroutine get tonight from the database.If the start
                        //  and end times are the not the same, meaning, the night has already been
                        //  completed, return null. Otherwise, return night
                        var night = database.getTonight()

                        if (night?.endTimeMilli != night?.startTimeMilli) {
                                night = null
                        }
                        night
                }
        }

        /** Executes when the START button is clicked. **/
        // to do the database work
        fun onStartTracking() {
                // Launch a coroutine in uiScope (results continue and update the UI)
                uiScope.launch {
                        // Create a new night, which captures the current time,
                        // and insert it into the database.
                        val newNight = SleepNight()
                        // Call insert() to insert it into the database
                        insert(newNight)
                        // Set tonight to the new night
                        tonight.value = getTonightFromDatabase()
                }
        }

        // Define insert() as a private suspend function that takes a SleepNight as
        // its argument
        private suspend fun insert(night: SleepNight) {
                // launch a coroutine in the IO context and insert the night into the database
                withContext(Dispatchers.IO) {
                        database.insert(night)
                }
        }

        /** Executes when the STOP button is clicked. **/
        fun onStopTracking() {
//                // Set a LiveData that changes when you want to navigate
//                private val _navigateToSleepQuality = MutableLiveData<SleepNight>()
//
//                val navigateToSleepQuality: LiveData<SleepNight>
//                get() = _navigateToSleepQuality

                // Launch a coroutine in uiScope (results continue and update the UI)
                uiScope.launch {
                        // In Kotlin, the return@label syntax is used for specifying which
                        // function among several nested ones this statement returns from.
                        // In this case, we are specifying to return from launch(), not the lambda.
                        val oldNight = tonight.value ?: return@launch

                        // Update the night in the database to add the end time.
                        oldNight.endTimeMilli = System.currentTimeMillis()
                        // Call update() to update the database
                        update(oldNight)

                        // Set state to navigate to the SleepQualityFragment.
                        _navigateToSleepQuality.value = oldNight
                }
        }

        // Define update() as a private suspend function that takes a SleepNight as its argument
        private suspend fun update(night: SleepNight) {
                // launch a coroutine in the IO context and insert the night into the database
                withContext(Dispatchers.IO) {
                        database.update(night)
                }
        }

        /** Executes when the CLEAR button is clicked. **/
        fun onClear() {
                // Launch a coroutine in uiScope (results continue and update the UI)
                uiScope.launch {
                        // Clear the database table.
                        clear()

                        // And clear tonight since it's no longer in the database
                        tonight.value = null
                }
                // Show a snackbar message, because it's friendly.
                _showSnackbarEvent.value = true
        }

        // Define clear() as a suspend function
        suspend fun clear() {
                // launch a coroutine in the IO context and insert the night into the database
                withContext(Dispatchers.IO) {
                        database.clear()
                }
        }
}

