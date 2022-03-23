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

package com.example.android.trackmysleepquality.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

// TODO Create annotated interface SleepDatabaseDao
@Dao
interface SleepDatabaseDao {

    //    TODO Add annotated insert() method for inserting a single SleepNight
    @Insert
    fun insert(night: SleepNight)

    /** When updating a row with a value already set in a column, replaces the old value
     * with the new one
     *
     * @param night new value to write
     **/
    //    TODO Add annotated update() method for updating a SleepNight
    @Update
    fun update(night: SleepNight)

    /** Select and returns the row that matches the supplied start time, which is our key
     *
     * @param key startTimeMilli to match
     **/
    //    TODO Add annotated get() method that gets the SleepNight by key
    @Query("SELECT * FROM daily_sleep_quality_table WHERE nightId = :key")
    fun get(key: Long): SleepNight?

    /** Delete all the values from the table, not the table **/
    //    TODO Add annotated clear() method and query
    @Query("DELETE FROM daily_sleep_quality_table")
    fun clear()

    /** Select and returns all rows, sorted by start time in DESC order **/
    //    TODO Add annotated getAllNights() method and query
    @Query("SELECT * FROM daily_sleep_quality_table ORDER BY nightId DESC")
    fun getAllNights(): LiveData<List<SleepNight>>

    /** Select and returns the latest night **/
    //    TODO Add annotated getTonight() method and query
    @Query("SELECT * FROM daily_sleep_quality_table ORDER BY nightId DESC LIMIT 1")
    fun getTonight(): SleepNight?


}
