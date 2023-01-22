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
import java.util.Locale

/**
 * Created by Sebastian Lutz on 12.03.2018.
 *
 * Class to set up To-Do subtasks and its parameters.
 */
class TodoSubTask : BaseTodo, Parcelable {
    override var name: String? = null
    var done: Boolean
    var isInTrash: Boolean
    var taskId: Long = 0
    override var progress: Int = 0

    constructor() : super() {
        done = false
        isInTrash = false
    }

    constructor(parcel: Parcel) {
        id = parcel.readInt()
        name = parcel.readString()
        done = parcel.readByte().toInt() != 0
        isInTrash = parcel.readByte().toInt() != 0
        taskId = parcel.readLong()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeString(name)
        dest.writeByte((if (done) 1 else 0).toByte())
        dest.writeByte((if (isInTrash) 1 else 0).toByte())
        dest.writeLong(taskId)
    }

    fun checkQueryMatch(query: String?): Boolean {
        // no query? always match!
        if (query.isNullOrEmpty()) return true
        val queryLowerCase = query.lowercase(Locale.getDefault())
        return this.name!!.lowercase(Locale.getDefault())
                .contains(queryLowerCase)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<TodoSubTask> = object : Parcelable.Creator<TodoSubTask> {
            override fun createFromParcel(source: Parcel): TodoSubTask {
                return TodoSubTask(source)
            }

            override fun newArray(size: Int): Array<TodoSubTask?> {
                return arrayOfNulls(size)
            }
        }
    }
}