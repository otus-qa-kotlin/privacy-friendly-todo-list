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
import android.content.Context

object AlarmManagerHolder {
    private var alarmManager: AlarmManager? = null
    @JvmStatic
    fun getAlarmManager(context: Context): AlarmManager? {
        if (alarmManager == null) {
            alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        }
        return alarmManager
    }
}