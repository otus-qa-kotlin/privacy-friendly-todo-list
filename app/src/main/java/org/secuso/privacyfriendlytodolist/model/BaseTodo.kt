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

import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler.ObjectStates

abstract class BaseTodo {
    var id = 0
    open var progress = 0
    open var name: String? = null
    var description: String? = null
    open var dBState: ObjectStates = ObjectStates.NO_DB_ACTION

    fun setCreated() {
        dBState = ObjectStates.INSERT_TO_DB
    }

    fun setChanged() {
        if (dBState == ObjectStates.NO_DB_ACTION) dBState = ObjectStates.UPDATE_DB
    }

    fun setChangedFromPomodoro() {
        dBState = ObjectStates.UPDATE_FROM_POMODORO
    }

    fun setUnchanged() {
        dBState = ObjectStates.NO_DB_ACTION
    }
}