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
package org.secuso.privacyfriendlytodolist.view.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.AdapterView
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import org.secuso.privacyfriendlytodolist.R
import org.secuso.privacyfriendlytodolist.model.TodoList
import org.secuso.privacyfriendlytodolist.model.TodoTask
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler.Companion.getAllToDoLists
import org.secuso.privacyfriendlytodolist.model.database.DatabaseHelper.Companion.getInstance

/**
 * Created by Sebastian Lutz on 15.02.2018.
 *
 * This class sets to-do tasks to show up in the widget
 *
 */
class WidgetViewsFactory(private val mContext: Context, intent: Intent?) : RemoteViewsFactory {
    private var lists: ArrayList<TodoList>
    private var listTasks: ArrayList<TodoTask>
    private var listChosen: String? = null

    init {
        lists = ArrayList()
        listTasks = ArrayList()
    }

    override fun onCreate() {
        listChosen = getListName(c, id)
        lists = getAllToDoLists(getInstance(mContext)!!.readableDatabase)
        for (i in lists.indices) {
            if (lists[i].name == listChosen) listTasks = lists[i].tasks
        }
    }

    override fun getCount(): Int {
        return listTasks.size
    }

    override fun onDataSetChanged() {
        listChosen = getListName(c, id)
        lists = getAllToDoLists(getInstance(mContext)!!.readableDatabase)
        for (i in lists.indices) {
            if (lists[i].name == listChosen) listTasks = lists[i].tasks
        }
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getViewAt(position: Int): RemoteViews? {
        if (position == AdapterView.INVALID_POSITION) {
            return null
        }
        val todo = listTasks[position]
        val itemView = RemoteViews(mContext.packageName, R.layout.widget_tasks)
        if (todo!!.done) {
            itemView.setViewVisibility(R.id.widget_done, View.VISIBLE)
            itemView.setViewVisibility(R.id.widget_undone, View.INVISIBLE)
        } else if (!todo.done) {
            itemView.setViewVisibility(R.id.widget_done, View.INVISIBLE)
            itemView.setViewVisibility(R.id.widget_undone, View.VISIBLE)
        }
        itemView.setTextViewText(R.id.tv_widget_task_name, todo.name)
        itemView.setEmptyView(R.id.tv_empty_widget, R.string.empty_todo_list)
        val fillInIntent = Intent()
        itemView.setOnClickFillInIntent(R.id.tv_widget_task_name, fillInIntent)
        itemView.setOnClickFillInIntent(R.id.widget_undone, fillInIntent)
        itemView.setOnClickFillInIntent(R.id.widget_done, fillInIntent)
        return itemView
    }

    override fun getItemId(position: Int): Long {
        return (ID_CONSTANT + position).toLong()
    }

    override fun onDestroy() {
        lists.clear()
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    companion object {
        private const val ID_CONSTANT = 0x0101010
        private var c: Context? = null
        private var id = 0
        fun getListName(context: Context?, AppWidgetId: Int): String {
            c = context
            id = AppWidgetId
            return TodoListWidgetConfigureActivity.Companion.loadTitlePref(context, AppWidgetId)
        }
    }
}