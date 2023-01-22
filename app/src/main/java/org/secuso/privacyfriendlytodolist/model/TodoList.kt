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

import android.graphics.Color
import android.os.Parcel
import android.os.Parcelable
import org.secuso.privacyfriendlytodolist.model.TodoTask.DeadlineColors
import java.util.Locale

/**
 * Created by Sebastian Lutz on 12.03.2018.
 *
 * Class to set up a To-Do List and its parameters.
 */
class TodoList : BaseTodo, Parcelable {
    var tasks = ArrayList<TodoTask>()

    constructor() {}

    constructor(parcel: Parcel) {
        id = parcel.readInt()
        name = parcel.readString()
        parcel.readList(tasks, TodoTask::class.java.classLoader)
    }

    val isDummyList: Boolean
        get() = id == DUMMY_LIST_ID

    fun setDummyList() {
        id = DUMMY_LIST_ID
    }

    val size: Int
        get() = tasks.size
    val color: Int
        get() = Color.BLACK
    val doneTodos: Int
        get() {
            var counter = 0
            for (task in tasks) counter += if (task.done) 1 else 0
            return counter
        }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeString(name)
        dest.writeList(tasks)
    }

    val nextDeadline: Long
        get() {
            var minDeadLine: Long = -1
            for (i in tasks.indices) {
                val currentTask = tasks[i]
                if (currentTask.done ==false) {
                    if (minDeadLine == -1L && currentTask.deadline > 0) minDeadLine =
                        currentTask.deadline else {
                        val possNewDeadline = currentTask.deadline
                        if (possNewDeadline > 0 && possNewDeadline < minDeadLine) {
                            minDeadLine = possNewDeadline
                        }
                    }
                }
            }
            return minDeadLine
        }

    fun getDeadlineColor(defaultReminderTime: Long): DeadlineColors {
        var orangeCounter = 0
        for (currentTask in tasks) {
            when (currentTask.getDeadlineColor(defaultReminderTime)) {
                DeadlineColors.RED -> return DeadlineColors.RED
                DeadlineColors.ORANGE -> orangeCounter++
                else -> {}
            }
        }
        return if (orangeCounter > 0) DeadlineColors.ORANGE else DeadlineColors.BLUE
    }

    @JvmOverloads
    fun checkQueryMatch(query: String?, recursive: Boolean = true): Boolean {
        // no query? always match!
        if (query == null || query.isEmpty()) return true
        val queryLowerCase = query.lowercase(Locale.getDefault())
        if (name!!.lowercase(Locale.getDefault()).contains(queryLowerCase)) return true
        if (recursive) for (i in tasks.indices) if (tasks[i]
                .checkQueryMatch(queryLowerCase, recursive)
        ) return true
        return false
    }

    companion object {
        const val PARCELABLE_KEY = "PARCELABLE_KEY_FOR_TODO_LIST"
        const val UNIQUE_DATABASE_ID = "CURRENT_TODO_LIST_ID"
        const val DUMMY_LIST_ID = -3 // -1 is often used for error codes
        @JvmField
        val CREATOR: Parcelable.Creator<TodoList> = object : Parcelable.Creator<TodoList> {
            override fun createFromParcel(source: Parcel): TodoList? {
                return TodoList(source)
            }

            override fun newArray(size: Int): Array<TodoList?> {
                return arrayOfNulls(size)
            }
        }
    }
}