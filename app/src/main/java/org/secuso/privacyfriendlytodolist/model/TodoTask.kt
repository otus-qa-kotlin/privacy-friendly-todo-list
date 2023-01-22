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

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler.ObjectStates
import java.util.Locale

/**
 *
 * Created by Sebastian Lutz on 12.03.2018.
 *
 * Class to set up To-Do Tasks and its parameters.
 */
class TodoTask : BaseTodo, Parcelable {
    enum class Priority( // Priority steps must be sorted in the same way like they will be displayed
        val value: Int
    ) {
        HIGH(0), MEDIUM(1), LOW(2);

        companion object {
            @JvmStatic
            fun fromInt(i: Int): Priority {
                for (p in values()) {
                    if (p.value == i) {
                        return p
                    }
                }
                throw IllegalArgumentException("No such priority defined.")
            }
        }
    }

    enum class DeadlineColors {
        BLUE, ORANGE, RED
    }

    var isInTrash: Boolean
    var done: Boolean
    override var progress: Int = 0
    var priority: Priority? = null
    var reminderTime: Long = -1
        set(reminderTime) {
        if (deadline in 1 until reminderTime) {
            Log.i(TAG, "Reminder time must not be greater than the deadline.")
        } else {
            field = reminderTime
        }

        // check if reminder time was already set and now changed -> important for reminder service
        if (reminderTimeWasInitialized) reminderTimeChanged = true
        reminderTimeWasInitialized = true

    }
    var listPosition // indicates at what position inside the list this task it placed
            = 0
        private set
    var listId = 0
    var deadline // absolute timestamp
            : Long = 0
    private var reminderTimeChanged = false // important for the reminder service
    private var reminderTimeWasInitialized = false
    var listName: String? = null
    var subTasks = ArrayList<TodoSubTask?>()

    constructor() : super() {
        done = false
        isInTrash = false
    }

    fun hasDeadline(): Boolean {
        return deadline > 0
    }

    constructor(parcel: Parcel) {
        id = parcel.readInt()
        listId = parcel.readInt()
        name = parcel.readString()
        description = parcel.readString()
        done = parcel.readByte().toInt() != 0
        isInTrash = parcel.readByte().toInt() != 0
        progress = parcel.readInt()
        deadline = parcel.readLong()
        reminderTime = parcel.readLong()
        listPosition = parcel.readInt()
        priority = Priority.fromInt(parcel.readInt())
        parcel.readList(subTasks, TodoSubTask::class.java.classLoader)
    }

    fun setPositionInList(pos: Int) {
        listPosition = pos
    }

    // This method expects the deadline to be greater than the reminder time.
    fun getDeadlineColor(defaultReminderTime: Long): DeadlineColors {

        // The default reminder time is a relative value in seconds (e.g. 86400s == 1 day)
        // The user specified reminder time is an absolute timestamp
        if (!done && deadline > 0) {
            val currentTimeStamp = Helper.currentTimestamp
            val remTimeToCalc =
                if (reminderTime > 0) deadline - reminderTime else defaultReminderTime
            if (currentTimeStamp >= deadline - remTimeToCalc && deadline > currentTimeStamp) return DeadlineColors.ORANGE
            if (deadline in 1 until currentTimeStamp) return DeadlineColors.RED
        }
        return DeadlineColors.BLUE
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeInt(listId)
        dest.writeString(name)
        dest.writeString(description)
        dest.writeByte((if (done) 1 else 0).toByte())
        dest.writeByte((if (isInTrash) 1 else 0).toByte())
        dest.writeInt(progress)
        dest.writeLong(deadline)
        dest.writeLong(reminderTime)
        dest.writeInt(listPosition)
        dest.writeInt(priority!!.value)
        dest.writeList(subTasks)
    }

    fun setAllSubTasksDone(doneSubTask: Boolean) {
        for (subTask in subTasks) {
            subTask?.done = doneSubTask
        }
    }

    // A task is done if the user manually sets it done or when all subtaks are done.
    // If a subtask is selected "done", the entire task might be "done" if by now all subtasks are done.
    fun doneStatusChanged() {
        var doneSubTasks = true
        var i = 0
        while (doneSubTasks && i < subTasks.size) {
            doneSubTasks = doneSubTasks and subTasks[i]!!.done
            i++
        }
        if (doneSubTasks != done) {
            dBState = ObjectStates.UPDATE_DB
        }
        done = doneSubTasks
    }

    @JvmOverloads
    fun checkQueryMatch(query: String?, recursive: Boolean = true): Boolean {
        // no query? always match!
        if (query.isNullOrEmpty()) return true
        val queryLowerCase = query.lowercase(Locale.getDefault())
        if (name!!.lowercase(Locale.getDefault()).contains(queryLowerCase)) return true
        if (description!!.lowercase(Locale.getDefault()).contains(queryLowerCase)) return true
        if (recursive) for (i in subTasks.indices) if (subTasks[i]!!.checkQueryMatch(queryLowerCase)) return true
        return false
    }

    companion object {
        private val TAG = TodoTask::class.java.simpleName
        const val PARCELABLE_KEY = "key_for_parcels"
        @JvmField
        val CREATOR: Parcelable.Creator<TodoTask> = object : Parcelable.Creator<TodoTask> {
            override fun createFromParcel(source: Parcel): TodoTask {
                return TodoTask(source)
            }

            override fun newArray(size: Int): Array<TodoTask?> {
                return arrayOfNulls(size)
            }
        }
    }
}