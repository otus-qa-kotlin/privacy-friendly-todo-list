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
import org.secuso.privacyfriendlytodolist.R
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.concurrent.TimeUnit

class DeadlineDialog(context: Context?, deadline: Long) :
    FullScreenDialog(context, R.layout.deadline_dialog) {
    var deadlineCallback: DeadlineCallback? = null

    init {
        val calendar = GregorianCalendar.getInstance()
        if (deadline != -1L) calendar.timeInMillis =
            TimeUnit.SECONDS.toMillis(deadline) else calendar.time = Calendar.getInstance().time
        val datePicker = findViewById<View>(R.id.dp_deadline) as DatePicker
        datePicker.updateDate(
            calendar[Calendar.YEAR],
            calendar[Calendar.MONTH],
            calendar[Calendar.DAY_OF_MONTH]
        )
        val buttonOkay = findViewById<View>(R.id.bt_deadline_ok) as Button
        buttonOkay.setOnClickListener {
            val datePicker = findViewById<View>(R.id.dp_deadline) as DatePicker
            val calendar: Calendar =
                GregorianCalendar(datePicker.year, datePicker.month, datePicker.dayOfMonth)
            deadlineCallback!!.setDeadline(TimeUnit.MILLISECONDS.toSeconds(calendar.timeInMillis))
            dismiss()
        }
        val buttonNoDeadline = findViewById<View>(R.id.bt_deadline_nodeadline) as Button
        buttonNoDeadline.setOnClickListener {
            deadlineCallback!!.removeDeadline()
            dismiss()
        }
    }

    interface DeadlineCallback {
        fun setDeadline(deadline: Long)
        fun removeDeadline()
    }
}