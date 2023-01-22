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
import android.widget.EditText
import android.widget.Toast
import org.secuso.privacyfriendlytodolist.R
import org.secuso.privacyfriendlytodolist.model.TodoList

/**
 * Created by Sebastian Lutz on 12.03.2018.
 *
 * Defines the dialog that lets the user create a list
 */
class ProcessTodoListDialog : FullScreenDialog {
    private val self = this
    private var buttonOkay: Button? = null
    private var buttonCancel: Button? = null
    private var etName: EditText? = null
    private val etDescription: EditText? = null
    private var todoList: TodoList

    constructor(context: Context?) : super(context, R.layout.add_todolist_dialog) {
        initGui()
        todoList = TodoList()
        todoList.setCreated()
        //todoList.setDbState(DBQueryHandler.ObjectStates.INSERT_TO_DB);
    }

    constructor(context: Context?, list2Change: TodoList) : super(
        context,
        R.layout.add_todolist_dialog
    ) {
        initGui()
        etName!!.setText(list2Change.name)
        todoList = list2Change
        todoList.setChanged()
        //todoList.setDbState(DBQueryHandler.ObjectStates.UPDATE_DB);
    }

    private fun initGui() {
        buttonOkay = findViewById<View>(R.id.bt_newtodolist_ok) as Button
        buttonOkay!!.setOnClickListener(OkayButtonListener())
        buttonCancel = findViewById<View>(R.id.bt_newtodolist_cancel) as Button
        buttonCancel!!.setOnClickListener { dismiss() }
        etName = findViewById<View>(R.id.et_todo_list_name) as EditText
    }

    private inner class OkayButtonListener : View.OnClickListener {
        override fun onClick(view: View) {

            // prepare list data
            val listName = etName!!.text.toString()
            if (listName == "" || listName == null) {
                Toast.makeText(
                    context,
                    context.getString(R.string.list_name_must_not_be_empty),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                if (changesMade(listName)) {
                    todoList.name = listName
                    callback!!.finish(todoList)
                }
                self.dismiss()
            }
        }
    }

    private fun changesMade(listName: String): Boolean {

        // check if real changes were made
        return if (listName == todoList.name) {
            false
        } else true
    }
}