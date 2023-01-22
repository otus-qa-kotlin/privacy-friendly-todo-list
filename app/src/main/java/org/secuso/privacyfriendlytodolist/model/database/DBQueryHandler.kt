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
package org.secuso.privacyfriendlytodolist.model.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import org.secuso.privacyfriendlytodolist.model.TodoList
import org.secuso.privacyfriendlytodolist.model.TodoSubTask
import org.secuso.privacyfriendlytodolist.model.TodoTask
import org.secuso.privacyfriendlytodolist.model.TodoTask.Priority.Companion.fromInt
import org.secuso.privacyfriendlytodolist.model.database.tables.TTodoList
import org.secuso.privacyfriendlytodolist.model.database.tables.TTodoSubTask
import org.secuso.privacyfriendlytodolist.model.database.tables.TTodoTask

/**
 * Created by Sebastian Lutz on 13.3.2018.
 *
 * This class encapsulates sql statements and returns them.
 *
 */
class DBQueryHandler {

    enum class ObjectStates {
        INSERT_TO_DB, UPDATE_DB, UPDATE_FROM_POMODORO, NO_DB_ACTION
    }

    companion object {
        private val TAG = DBQueryHandler::class.java.simpleName
        const val NO_CHANGES = -2
        fun getNextDueTask(db: SQLiteDatabase, today: Long): TodoTask? {
            val rawQuery =
                "SELECT * FROM " + TTodoTask.TABLE_NAME + " WHERE " + TTodoTask.COLUMN_DONE + "=0 AND " + TTodoTask.COLUMN_DEADLINE_WARNING_TIME + " > 0 AND " + TTodoTask.COLUMN_TRASH + "=0 AND " + TTodoTask.COLUMN_DEADLINE_WARNING_TIME + "-? > 0 ORDER BY ABS(" + TTodoTask.COLUMN_DEADLINE_WARNING_TIME + " -?) LIMIT 1;"
            val selectionArgs = arrayOf(today.toString())
            var nextDueTask: TodoTask? = null
            try {
                val cursor = db.rawQuery(rawQuery, selectionArgs)
                cursor.use { cursor ->
                    if (cursor.moveToFirst()) {
                        nextDueTask = extractTodoTask(cursor)
                    }
                }
            } catch (ex: Exception) {
            }
            return nextDueTask
        }

        /**
         * returns a list of tasks
         *
         * -   which are not fulfilled and whose reminder time is prior to the current time
         * -   the task which is next due
         */
        fun getTasksToRemind(
            db: SQLiteDatabase,
            today: Long,
            lockedIds: HashSet<Int>?
        ): ArrayList<TodoTask> {
            val tasks = ArrayList<TodoTask>()

            // do not request tasks for which the user was just notified (these tasks are locked)
            val excludedIDs = StringBuilder()
            val and = " AND "
            if (lockedIds != null && lockedIds.size > 0) {
                excludedIDs.append(" AND ")
                for (lockedTaskID in lockedIds) {
                    excludedIDs.append(TTodoTask.COLUMN_ID + " <> " + lockedTaskID.toString())
                    excludedIDs.append(" AND ")
                }
                excludedIDs.setLength(excludedIDs.length - and.length)
            }
            excludedIDs.append(";")
            val rawQuery =
                "SELECT * FROM " + TTodoTask.TABLE_NAME + " WHERE " + TTodoTask.COLUMN_DONE + " = 0 AND " + TTodoTask.COLUMN_TRASH + "=0 AND " + TTodoTask.COLUMN_DEADLINE_WARNING_TIME + " > 0 AND " + TTodoTask.COLUMN_DEADLINE_WARNING_TIME + " <= ? " + excludedIDs.toString()
            val selectionArgs = arrayOf(today.toString())
            try {
                val cursor = db.rawQuery(rawQuery, selectionArgs)
                cursor.use { cursor ->
                    if (cursor.moveToFirst()) {
                        do {
                            val taskForNotification = extractTodoTask(cursor)
                            tasks.add(taskForNotification)
                        } while (cursor.moveToNext())
                    }
                }
            } catch (e: Exception) {
            }

            // get task that is next due
            val nextDueTask = getNextDueTask(db, today)
            if (nextDueTask != null) tasks.add(nextDueTask)
            return tasks
        }

        @SuppressLint("Range")
        private fun extractTodoTask(cursor: Cursor): TodoTask {
            val id = cursor.getInt(cursor.getColumnIndex(TTodoTask.COLUMN_ID))
            val listPosition = cursor.getInt(cursor.getColumnIndex(TTodoTask.COLUMN_LIST_POSITION))
            val title = cursor.getString(cursor.getColumnIndex(TTodoTask.COLUMN_NAME))
            val description = cursor.getString(cursor.getColumnIndex(TTodoTask.COLUMN_DESCRIPTION))
            val done = cursor.getInt(cursor.getColumnIndex(TTodoTask.COLUMN_DONE)) > 0
            val progress = cursor.getInt(cursor.getColumnIndex(TTodoTask.COLUMN_PROGRESS))
            val deadline = cursor.getInt(cursor.getColumnIndex(TTodoTask.COLUMN_DEADLINE))
            val reminderTime =
                cursor.getInt(cursor.getColumnIndex(TTodoTask.COLUMN_DEADLINE_WARNING_TIME))
            val priority = cursor.getInt(cursor.getColumnIndex(TTodoTask.COLUMN_PRIORITY))
            val listID = cursor.getInt(cursor.getColumnIndex(TTodoTask.COLUMN_TODO_LIST_ID))
            val inTrash = cursor.getInt(cursor.getColumnIndex(TTodoTask.COLUMN_TRASH)) > 0
            val task = TodoTask()
            task.name = title
            task.deadline = deadline.toLong()
            task.description = description
            task.priority = fromInt(priority)
            task.reminderTime = reminderTime.toLong()
            task.id = id
            task.setPositionInList(listPosition)
            task.progress = progress
            task.done = done
            task.listId = listID
            task.isInTrash = inTrash
            return task
        }

        @JvmStatic
        fun deleteTodoList(db: SQLiteDatabase, todoList: TodoList) {
            val id = todoList.id.toLong()
            var deletedTasks = 0
            for (task in todoList.tasks) deletedTasks += putTaskInTrash(db, task)
            Log.i(TAG, "$deletedTasks tasks put into trash")
            val where = TTodoList.COLUMN_ID + "=?"
            val whereArgs = arrayOf(id.toString())
            val deletedLists = db.delete(TTodoList.TABLE_NAME, where, whereArgs)
            Log.i(TAG, "$deletedLists lists removed from database")
        }

        @JvmStatic
        fun deleteTodoTask(db: SQLiteDatabase, todoTask: TodoTask): Int {
            val id = todoTask.id.toLong()
            var removedSubTask = 0
            for (subTask in todoTask.subTasks) removedSubTask += deleteTodoSubTask(db, subTask)
            Log.i(TAG, "$removedSubTask subtasks removed from database")
            val where = TTodoTask.COLUMN_ID + " = ?"
            val whereArgs = arrayOf(id.toString())
            return db.delete(TTodoTask.TABLE_NAME, where, whereArgs)
        }

        @SuppressLint("Range")
        @JvmStatic
        fun getAllToDoTasks(db: SQLiteDatabase): ArrayList<TodoTask> {
            val todo = ArrayList<TodoTask>()
            val where = TTodoTask.COLUMN_TRASH + " =0"
            try {
                val c = db.query(TTodoTask.TABLE_NAME, null, where, null, null, null, null)
                c.use { c ->
                    if (c.moveToFirst()) {
                        do {
                            val id = c.getInt(c.getColumnIndex(TTodoTask.COLUMN_ID))
                            val listId = c.getInt(c.getColumnIndex(TTodoTask.COLUMN_TODO_LIST_ID))
                            val taskName = c.getString(c.getColumnIndex(TTodoTask.COLUMN_NAME))
                            val taskDescription =
                                c.getString(c.getColumnIndex(TTodoTask.COLUMN_DESCRIPTION))
                            val progress = c.getInt(c.getColumnIndex(TTodoTask.COLUMN_PROGRESS))
                            val deadline = c.getLong(c.getColumnIndex(TTodoTask.COLUMN_DEADLINE))
                            val priority = c.getInt(c.getColumnIndex(TTodoTask.COLUMN_PRIORITY))
                            val done = c.getInt(c.getColumnIndex(TTodoTask.COLUMN_DONE)) > 0
                            val reminderTime =
                                c.getInt(c.getColumnIndex(TTodoTask.COLUMN_DEADLINE_WARNING_TIME))
                            val inTrash = c.getInt(c.getColumnIndex(TTodoTask.COLUMN_TRASH)) > 0
                            val currentTask = TodoTask()
                            currentTask.name = taskName
                            currentTask.description = taskDescription
                            currentTask.id = id
                            currentTask.subTasks = getSubTasksByTaskId(db, id.toLong(), 0)
                            currentTask.progress = progress
                            currentTask.deadline = deadline
                            currentTask.priority = fromInt(priority)
                            currentTask.done = done
                            currentTask.reminderTime = reminderTime.toLong()
                            currentTask.isInTrash = inTrash
                            currentTask.listId = listId
                            todo.add(currentTask)
                        } while (c.moveToNext())
                    }
                }
            } catch (ex: Exception) {
            }
            return todo
        }

        @SuppressLint("Range")
        @JvmStatic
        fun getBin(db: SQLiteDatabase): ArrayList<TodoTask> {
            val todo = ArrayList<TodoTask>()
            val where = TTodoTask.COLUMN_TRASH + " >0"
            try {
                val c = db.query(TTodoTask.TABLE_NAME, null, where, null, null, null, null)
                c.use { c ->
                    if (c.moveToFirst()) {
                        do {
                            val id = c.getInt(c.getColumnIndex(TTodoTask.COLUMN_ID))
                            val listId = c.getInt(c.getColumnIndex(TTodoTask.COLUMN_TODO_LIST_ID))
                            val taskName = c.getString(c.getColumnIndex(TTodoTask.COLUMN_NAME))
                            val taskDescription =
                                c.getString(c.getColumnIndex(TTodoTask.COLUMN_DESCRIPTION))
                            val progress = c.getInt(c.getColumnIndex(TTodoTask.COLUMN_PROGRESS))
                            val deadline = c.getLong(c.getColumnIndex(TTodoTask.COLUMN_DEADLINE))
                            val priority = c.getInt(c.getColumnIndex(TTodoTask.COLUMN_PRIORITY))
                            val done = c.getInt(c.getColumnIndex(TTodoTask.COLUMN_DONE)) > 0
                            val reminderTime =
                                c.getInt(c.getColumnIndex(TTodoTask.COLUMN_DEADLINE_WARNING_TIME))
                            val inTrash = c.getInt(c.getColumnIndex(TTodoTask.COLUMN_TRASH)) > 0
                            val currentTask = TodoTask()
                            currentTask.name = taskName
                            currentTask.description = taskDescription
                            currentTask.id = id
                            currentTask.subTasks = getSubTasksByTaskId(db, id.toLong(), 1)
                            currentTask.progress = progress
                            currentTask.deadline = deadline
                            currentTask.priority = fromInt(priority)
                            currentTask.done = done
                            currentTask.reminderTime = reminderTime.toLong()
                            currentTask.isInTrash = inTrash
                            currentTask.listId = listId
                            todo.add(currentTask)
                        } while (c.moveToNext())
                    }
                }
            } catch (ex: Exception) {
            }
            return todo
        }

        @SuppressLint("Range")
        @JvmStatic
        fun getAllToDoLists(db: SQLiteDatabase): ArrayList<TodoList> {
            val todoLists = ArrayList<TodoList>()
            try {
                val cursor = db.query(TTodoList.TABLE_NAME, null, null, null, null, null, null)
                cursor.use { cursor ->
                    if (cursor.moveToFirst()) {
                        do {
                            val id = cursor.getInt(cursor.getColumnIndex(TTodoList.COLUMN_ID))
                            val listName =
                                cursor.getString(cursor.getColumnIndex(TTodoList.COLUMN_NAME))
                            val currentList = TodoList()
                            currentList.name = listName
                            currentList.id = id
                            currentList.tasks = getTasksByListId(db, id, listName)
                            todoLists.add(currentList)
                        } while (cursor.moveToNext())
                    }
                }
            } catch (ex: Exception) {
            }
            return todoLists
        }

        private fun getTasksByListId(
            db: SQLiteDatabase,
            listId: Int,
            listName: String
        ): ArrayList<TodoTask> {
            val tasks = ArrayList<TodoTask>()
            val where =
                TTodoTask.COLUMN_TODO_LIST_ID + " = " + listId + " AND " + TTodoTask.COLUMN_TRASH + "=0"
            val cursor = db.query(TTodoTask.TABLE_NAME, null, where, null, null, null, null)
            cursor.use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        val currentTask = extractTodoTask(cursor)
                        currentTask.listName = listName
                        currentTask.subTasks = getSubTasksByTaskId(db, currentTask.id.toLong(), 0)
                        tasks.add(currentTask)
                    } while (cursor.moveToNext())
                }
            }
            return tasks
        }

        @SuppressLint("Range")
        private fun getSubTasksByTaskId(
            db: SQLiteDatabase,
            taskId: Long,
            alsoTrashedOnes: Int
        ): ArrayList<TodoSubTask?> {
            val subTasks = ArrayList<TodoSubTask?>()
            val where =
                TTodoSubTask.COLUMN_TASK_ID + " = " + taskId + " AND " + TTodoSubTask.COLUMN_TRASH + "<=" + alsoTrashedOnes
            val cursor = db.query(TTodoSubTask.TABLE_NAME, null, where, null, null, null, null)
            cursor.moveToFirst()
            cursor.use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        val id = cursor.getInt(cursor.getColumnIndex(TTodoSubTask.COLUMN_ID))
                        val title =
                            cursor.getString(cursor.getColumnIndex(TTodoSubTask.COLUMN_TITLE))
                        val done =
                            cursor.getInt(cursor.getColumnIndex(TTodoSubTask.COLUMN_DONE)) > 0
                        val trash =
                            cursor.getInt(cursor.getColumnIndex(TTodoSubTask.COLUMN_TRASH)) > 0
                        val currentSubTask = TodoSubTask()
                        currentSubTask.id = id
                        currentSubTask.name = title
                        currentSubTask.done = done
                        currentSubTask.taskId = taskId
                        currentSubTask.isInTrash = trash
                        subTasks.add(currentSubTask)
                    } while (cursor.moveToNext())
                }
            }
            return subTasks
        }

        @JvmStatic
        fun saveTodoSubTaskInDb(db: SQLiteDatabase, subTask: TodoSubTask): Int {
            val returnCode: Int
            if (subTask.dBState != ObjectStates.NO_DB_ACTION) {
                val values = ContentValues()
                values.put(TTodoSubTask.COLUMN_TITLE, subTask.name)
                values.put(TTodoSubTask.COLUMN_DONE, subTask.done)
                values.put(TTodoSubTask.COLUMN_TASK_ID, subTask.taskId)
                values.put(TTodoSubTask.COLUMN_TRASH, subTask.isInTrash)
                if (subTask.dBState == ObjectStates.INSERT_TO_DB) {
                    returnCode = db.insert(TTodoSubTask.TABLE_NAME, null, values).toInt()
                    Log.d(
                        TAG,
                        "Todo subtask " + subTask.name + " was inserted into the database (return code: " + returnCode + ")."
                    )
                } else if (subTask.dBState == ObjectStates.UPDATE_DB) {
                    val whereClause = TTodoSubTask.COLUMN_ID + "=?"
                    val whereArgs = arrayOf(subTask.id.toString())
                    db.update(TTodoSubTask.TABLE_NAME, values, whereClause, whereArgs)
                    returnCode = subTask.id
                    Log.d(
                        TAG,
                        "Todo subtask " + subTask.name + " was updated (return code: " + returnCode + ")."
                    )
                } else returnCode = NO_CHANGES
                subTask.setUnchanged()
                //subTask.setDbState(ObjectStates.NO_DB_ACTION);
            } else {
                returnCode = NO_CHANGES
            }
            return returnCode
        }

        @JvmStatic
        fun saveTodoTaskInDb(db: SQLiteDatabase, todoTask: TodoTask): Int {
            val returnCode: Int
            if (todoTask.dBState != ObjectStates.NO_DB_ACTION && todoTask.dBState != ObjectStates.UPDATE_FROM_POMODORO) {
                val values = ContentValues()
                values.put(TTodoTask.COLUMN_NAME, todoTask.name)
                values.put(TTodoTask.COLUMN_DESCRIPTION, todoTask.description)
                values.put(TTodoTask.COLUMN_PROGRESS, todoTask.progress)
                values.put(TTodoTask.COLUMN_DEADLINE, todoTask.deadline)
                values.put(TTodoTask.COLUMN_DEADLINE_WARNING_TIME, todoTask.reminderTime)
                values.put(TTodoTask.COLUMN_PRIORITY, todoTask.priority!!.value)
                values.put(TTodoTask.COLUMN_TODO_LIST_ID, todoTask.listId)
                values.put(TTodoTask.COLUMN_LIST_POSITION, todoTask.listPosition)
                values.put(TTodoTask.COLUMN_DONE, todoTask.done)
                values.put(TTodoTask.COLUMN_TRASH, todoTask.isInTrash)
                if (todoTask.dBState == ObjectStates.INSERT_TO_DB) {
                    returnCode = db.insert(TTodoTask.TABLE_NAME, null, values).toInt()
                    Log.d(
                        TAG,
                        "Todo task " + todoTask.name + " was inserted into the database (return code: " + returnCode + ")."
                    )
                } else if (todoTask.dBState == ObjectStates.UPDATE_DB) {
                    val whereClause = TTodoTask.COLUMN_ID + "=?"
                    val whereArgs = arrayOf(todoTask.id.toString())
                    db.update(TTodoTask.TABLE_NAME, values, whereClause, whereArgs)
                    returnCode = todoTask.id
                    Log.d(
                        TAG,
                        "Todo task " + todoTask.name + " was updated (return code: " + returnCode + ")."
                    )
                } else returnCode = NO_CHANGES
                todoTask.setUnchanged()
                //todoTask.setDbState(ObjectStates.NO_DB_ACTION);
            } else if (todoTask.dBState == ObjectStates.UPDATE_FROM_POMODORO) {
                //Only update values given by pomodoro
                val values = ContentValues()
                values.put(TTodoTask.COLUMN_NAME, todoTask.name)
                values.put(TTodoTask.COLUMN_PROGRESS, todoTask.progress)
                values.put(TTodoTask.COLUMN_DONE, todoTask.done)
                val whereClause = TTodoTask.COLUMN_ID + "=?"
                val whereArgs = arrayOf(todoTask.id.toString())
                db.update(TTodoTask.TABLE_NAME, values, whereClause, whereArgs)
                returnCode = todoTask.id
                Log.d(
                    TAG,
                    "Todo task " + todoTask.name + " was updated (return code: " + returnCode + ")."
                )
                todoTask.setUnchanged()
            } else {
                Log.d("DB", "5")
                returnCode = NO_CHANGES
            }
            Log.d("DB", "return code:$returnCode")
            return returnCode
        }

        // returns the id of the todolist
        @JvmStatic
        fun saveTodoListInDb(db: SQLiteDatabase, todoList: TodoList): Int {
            val returnCode: Int

            // Log.i(TAG, "Changes of list " + currentList.getName() + " were stored in the database.");
            if (todoList.dBState != ObjectStates.NO_DB_ACTION) {
                val values = ContentValues()
                values.put(TTodoList.COLUMN_NAME, todoList.name)
                if (todoList.dBState == ObjectStates.INSERT_TO_DB) {
                    returnCode = db.insert(TTodoList.TABLE_NAME, null, values).toInt()
                    Log.d(
                        TAG,
                        "Todo list " + todoList.name + " was inserted into the database (return code: " + returnCode + ")."
                    )
                } else if (todoList.dBState == ObjectStates.UPDATE_DB) {
                    val whereClause = TTodoList.COLUMN_ID + "=?"
                    val whereArgs = arrayOf(todoList.id.toString())
                    db.update(TTodoList.TABLE_NAME, values, whereClause, whereArgs)
                    returnCode = todoList.id
                    Log.d(
                        TAG,
                        "Todo list " + todoList.name + " was updated (return code: " + returnCode + ")."
                    )
                } else returnCode = NO_CHANGES
                todoList.setUnchanged()
                //todoList.setDbState(ObjectStates.NO_DB_ACTION);
            } else {
                returnCode = NO_CHANGES
            }
            return returnCode
        }

        @JvmStatic
        fun deleteTodoSubTask(db: SQLiteDatabase, subTask: TodoSubTask?): Int {
            val id = subTask!!.id.toLong()
            val where = TTodoSubTask.COLUMN_ID + " = ?"
            val whereArgs = arrayOf(id.toString())
            return db.delete(TTodoSubTask.TABLE_NAME, where, whereArgs)
        }

        @JvmStatic
        fun putTaskInTrash(db: SQLiteDatabase, todoTask: TodoTask?): Int {
            val id = todoTask!!.id.toLong()
            val args = ContentValues()
            args.put(TTodoTask.COLUMN_TRASH, 1)
            var removedSubTask = 0
            for (subTask in todoTask.subTasks) removedSubTask += putSubtaskInTrash(db, subTask)
            Log.i(TAG, "$removedSubTask subtasks put into bin")
            val where = TTodoTask.COLUMN_ID + " = ?"
            val whereArgs = arrayOf(id.toString())
            return db.update(TTodoTask.TABLE_NAME, args, where, whereArgs)
        }

        @JvmStatic
        fun putSubtaskInTrash(db: SQLiteDatabase, subTask: TodoSubTask?): Int {
            val id = subTask!!.id.toLong()
            val args = ContentValues()
            args.put(TTodoSubTask.COLUMN_TRASH, 1)
            val where = TTodoSubTask.COLUMN_ID + " = ?"
            val whereArgs = arrayOf(id.toString())
            return db.update(TTodoSubTask.TABLE_NAME, args, where, whereArgs)
        }

        @JvmStatic
        fun recoverTasks(db: SQLiteDatabase, todoTask: TodoTask): Int {
            val id = todoTask.id.toLong()
            val args = ContentValues()
            args.put(TTodoTask.COLUMN_TRASH, 0)
            var removedSubTask = 0
            for (subTask in todoTask.subTasks) removedSubTask += recoverSubtasks(db, subTask)
            Log.i(TAG, "$removedSubTask subtasks put into bin")
            val where = TTodoTask.COLUMN_ID + " = ?"
            val whereArgs = arrayOf(id.toString())
            return db.update(TTodoTask.TABLE_NAME, args, where, whereArgs)
        }

        @JvmStatic
        fun recoverSubtasks(db: SQLiteDatabase, subTask: TodoSubTask?): Int {
            val id = subTask!!.id.toLong()
            val args = ContentValues()
            args.put(TTodoSubTask.COLUMN_TRASH, 0)
            val where = TTodoSubTask.COLUMN_ID + " = ?"
            val whereArgs = arrayOf(id.toString())
            return db.update(TTodoSubTask.TABLE_NAME, args, where, whereArgs)
        }
    }
}