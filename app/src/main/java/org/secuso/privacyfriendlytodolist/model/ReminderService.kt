/*
 This file is part of Privacy Friendly To-Do List.

 Privacy Friendly To-Do List is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.

 Privacy Friendly To-Do List is distributed in the hope
 that it will be useful, but WITHOUT ANY WARRANTY; without even
 the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Privacy Friendly To-Do List. If not, see <http://www.gnu.org/licenses/>.
 */
package org.secuso.privacyfriendlytodolist.model

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import org.secuso.privacyfriendlytodolist.R
import org.secuso.privacyfriendlytodolist.model.AlarmManagerHolder.getAlarmManager
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler
import org.secuso.privacyfriendlytodolist.model.database.DatabaseHelper
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Created by Sebastian Lutz on 12.03.2018.
 *
 * This service implements the following alarm policies:
 *
 * - On startup it sets alarms for all tasks fulfilling all of the subsequent conditions:
 * 1. The reminding time is in the past.
 * 2. The deadline is in the future.
 * 3. The task is not yet done.
 * - On startup the service sets an alarm for next due task in the future.
 *
 * - Whenever an alarm is triggered the service sets the alarm for the next due task in the future. It is possible
 * that this alarm is already set. In that case is just gets overwritten.
 */
class ReminderService : Service() {
    private var dbHelper: DatabaseHelper? = null
    private var alreadyRunning = false
    private val mBinder: IBinder = ReminderServiceBinder()
    private var mNotificationManager: NotificationManager? = null
    private var alarmManager: AlarmManager? = null
    private var helper: NotificationHelper? = null
    override fun onBind(intent: Intent): IBinder {
        val extras = intent.extras
        // Get messager from the Activity
        if (extras != null) {
            Log.i(TAG, "onBind() with extra")
        } else {
            Log.i(TAG, "onBind()")
        }
        return mBinder
    }

    override fun onDestroy() {
        dbHelper!!.close()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand()")
        dbHelper = DatabaseHelper.getInstance(this)
        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        alarmManager = getAlarmManager(this)
        helper = NotificationHelper(this)
        var alarmTriggered = false
        val extras = intent.extras
        if (extras != null) alarmTriggered = intent.extras!!.getBoolean(ALARM_TRIGGERED)
        if (alarmTriggered) {
            val task = intent.extras!!.getParcelable<TodoTask>(TodoTask.PARCELABLE_KEY)
            handleAlarm(task)

            // get next alarm
            val nextDueTask = DBQueryHandler.getNextDueTask(
                dbHelper!!.getReadableDatabase(),
                Helper.currentTimestamp
            )
            nextDueTask?.let { setAlarmForTask(it) }
        } else {

            //  service was started for the first time
            if (!alreadyRunning) {
                reloadAlarmsFromDB()
                alreadyRunning =
                    true // If this service gets killed, alreadyRunning will be false the next time. However, the service is only killed when the resources are scarce. So we deliberately set the alarms again after restarting the service.
                Log.i(TAG, "Service was started the first time.")
            } else {
                Log.i(TAG, "Service was already running.")
            }
        }
        return START_NOT_STICKY // do not recreate service if the phone runs out of memory
    }

    private fun handleAlarm(task: TodoTask?) {
        val title = task?.name
        val nb = helper!!.getNotification(
            title,
            resources.getString(
                R.string.deadline_approaching,
                Helper.getDateTime(task!!.deadline)
            ),
            task
        )
        helper!!.manager?.notify(task.id, nb.build())
    }

    fun reloadAlarmsFromDB() {
        mNotificationManager!!.cancelAll() // cancel all alarms
        val tasksToRemind = DBQueryHandler.getTasksToRemind(
            dbHelper!!.readableDatabase, Helper.currentTimestamp, null
        )

        // set alarms
        for (currentTask in tasksToRemind) {
            setAlarmForTask(currentTask)
        }
        if (tasksToRemind.size == 0) {
            Log.i(TAG, "No alarms set.")
        }
    }

    private fun setAlarmForTask(task: TodoTask) {
        val alarmIntent = Intent(this, ReminderService::class.java)
        alarmIntent.putExtra(TodoTask.PARCELABLE_KEY, task)
        alarmIntent.putExtra(ALARM_TRIGGERED, true)
        val alarmID = task.id // use database id as unique alarm id
        val pendingAlarmIntent = PendingIntent.getService(
            this,
            alarmID,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = Calendar.getInstance()
        val reminderTime = task.reminderTime
        if (reminderTime != -1L && reminderTime <= Helper.currentTimestamp) {
            val date = Date(TimeUnit.SECONDS.toMillis(Helper.currentTimestamp))
            calendar.time = date
            alarmManager!![AlarmManager.RTC_WAKEUP, calendar.timeInMillis] = pendingAlarmIntent
            Log.i(
                TAG,
                "Alarm set for " + task?.name + " at " + Helper.getDateTime(calendar.timeInMillis / 1000) + " (alarm id: " + alarmID + ")"
            )
        } else if (reminderTime != -1L) {
            val date = Date(TimeUnit.SECONDS.toMillis(reminderTime)) // convert to milliseconds
            calendar.time = date
            alarmManager!![AlarmManager.RTC_WAKEUP, calendar.timeInMillis] = pendingAlarmIntent
            Log.i(
                TAG,
                "Alarm set for " + task?.name + " at " + Helper.getDateTime(calendar.timeInMillis / 1000) + " (alarm id: " + alarmID + ")"
            )
        }
    }

    fun processTask(changedTask: TodoTask) {

        // TODO add more granularity: You don't need to change the alarm if the name or the description of the task were changed. You actually need this perform the following steps if the reminder time or the "done" status were modified.
        val alarmIntent = PendingIntent.getBroadcast(
            this, changedTask.id,
            Intent(this, ReminderService::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // check if alarm was set for this task
        if (alarmIntent != null) {

            // 1. cancel old alarm
            alarmManager!!.cancel(alarmIntent)
            Log.i(
                TAG,
                "Alarm of task " + changedTask.name + " cancelled. (id=" + changedTask.id + ")"
            )

            // 2. delete old notification if it exists
            mNotificationManager!!.cancel(changedTask.id)
            Log.i(
                TAG,
                "Notification of task " + changedTask.name + " deleted (if existed). (id=" + changedTask.id + ")"
            )
        } else {
            Log.i(
                TAG,
                "No alarm found for " + changedTask.name + " (alarm id: " + changedTask.id + ")"
            )
        }
        setAlarmForTask(changedTask)
    }

    inner class ReminderServiceBinder : Binder() {
        val service: ReminderService
            get() = this@ReminderService
    }

    companion object {
        private val TAG = ReminderService::class.java.simpleName
        const val ALARM_TRIGGERED = "ALARM_TRIGGERD"
    }
}