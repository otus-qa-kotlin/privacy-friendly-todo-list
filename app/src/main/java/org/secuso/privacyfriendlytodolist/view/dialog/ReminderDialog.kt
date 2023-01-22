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
package org.secuso.privacyfriendlytodolist.view.dialog

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.DatePicker.OnDateChangedListener
import android.widget.LinearLayout
import android.widget.TimePicker
import org.secuso.privacyfriendlytodolist.R
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.concurrent.TimeUnit

class ReminderDialog constructor(context: Context?, reminderTime: Long, deadline: Long) :
    FullScreenDialog(context, R.layout.reminder_dialog) {
    var reminderCallback: ReminderCallback? = null

    init {
        val calendar: Calendar = GregorianCalendar.getInstance()
        if (reminderTime != -1L) calendar.setTimeInMillis(TimeUnit.SECONDS.toMillis(reminderTime)) else if (deadline != -1L) calendar.setTimeInMillis(
            TimeUnit.SECONDS.toMillis(deadline)
        ) //TODO subtract predefined reminder interval
        else calendar.setTime(Calendar.getInstance().getTime())
        val datePicker: DatePicker = findViewById<View>(R.id.dp_reminder) as DatePicker
        //datePicker.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(
            Calendar.DAY_OF_MONTH
        ), object : OnDateChangedListener {
            override fun onDateChanged(
                view: DatePicker,
                year: Int,
                monthOfYear: Int,
                dayOfMonth: Int
            ) {
                val layoutDate: LinearLayout =
                    findViewById<View>(R.id.ll_reminder_date) as LinearLayout
                layoutDate.setVisibility(View.GONE)
                val layoutTime: LinearLayout =
                    findViewById<View>(R.id.ll_reminder_time) as LinearLayout
                layoutTime.setVisibility(View.VISIBLE)
            }
        })
        val timePicker: TimePicker = findViewById<View>(R.id.tp_reminder) as TimePicker
        timePicker.setIs24HourView(true)
        timePicker.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY))
        timePicker.setCurrentMinute(calendar.get(Calendar.MINUTE) + 1)
        val buttonDate: Button = findViewById<View>(R.id.bt_reminder_date) as Button
        buttonDate.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                val layoutDate: LinearLayout =
                    findViewById<View>(R.id.ll_reminder_date) as LinearLayout
                layoutDate.setVisibility(View.VISIBLE)
                val layoutTime: LinearLayout =
                    findViewById<View>(R.id.ll_reminder_time) as LinearLayout
                layoutTime.setVisibility(View.GONE)
            }
        })
        val buttonTime: Button = findViewById<View>(R.id.bt_reminder_time) as Button
        buttonTime.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                val layoutDate: LinearLayout =
                    findViewById<View>(R.id.ll_reminder_date) as LinearLayout
                layoutDate.setVisibility(View.GONE)
                val layoutTime: LinearLayout =
                    findViewById<View>(R.id.ll_reminder_time) as LinearLayout
                layoutTime.setVisibility(View.VISIBLE)
            }
        })
        val buttonOkay: Button = findViewById<View>(R.id.bt_reminder_ok) as Button
        buttonOkay.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                val datePicker: DatePicker = findViewById<View>(R.id.dp_reminder) as DatePicker
                val timePicker: TimePicker = findViewById<View>(R.id.tp_reminder) as TimePicker
                val calendar: Calendar = GregorianCalendar(
                    datePicker.getYear(),
                    datePicker.getMonth(),
                    datePicker.getDayOfMonth(),
                    timePicker.getCurrentHour(),
                    timePicker.getCurrentMinute()
                )
                reminderCallback!!.setReminder(TimeUnit.MILLISECONDS.toSeconds(calendar.getTimeInMillis()))
                dismiss()
            }
        })
        val buttonNoReminder: Button = findViewById<View>(R.id.bt_reminder_noreminder) as Button
        buttonNoReminder.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                reminderCallback!!.removeReminder()
                dismiss()
            }
        })
    }

    interface ReminderCallback {
        fun setReminder(deadline: Long)
        fun removeReminder()
    }
}