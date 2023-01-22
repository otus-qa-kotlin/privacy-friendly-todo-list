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
package org.secuso.privacyfriendlytodolist.view

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.ExpandableListView
import android.widget.ExpandableListView.ExpandableListContextMenuInfo
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.MenuItemCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import org.secuso.privacyfriendlytodolist.R
import org.secuso.privacyfriendlytodolist.model.BaseTodo
import org.secuso.privacyfriendlytodolist.model.Helper.getMenuHeader
import org.secuso.privacyfriendlytodolist.model.ReminderService
import org.secuso.privacyfriendlytodolist.model.ReminderService.ReminderServiceBinder
import org.secuso.privacyfriendlytodolist.model.TodoList
import org.secuso.privacyfriendlytodolist.model.TodoSubTask
import org.secuso.privacyfriendlytodolist.model.TodoTask
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler.Companion.deleteTodoList
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler.Companion.deleteTodoSubTask
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler.Companion.getAllToDoLists
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler.Companion.getAllToDoTasks
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler.Companion.putSubtaskInTrash
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler.Companion.putTaskInTrash
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler.Companion.recoverSubtasks
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler.Companion.recoverTasks
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler.Companion.saveTodoListInDb
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler.Companion.saveTodoSubTaskInDb
import org.secuso.privacyfriendlytodolist.model.database.DBQueryHandler.Companion.saveTodoTaskInDb
import org.secuso.privacyfriendlytodolist.model.database.DatabaseHelper
import org.secuso.privacyfriendlytodolist.model.database.DatabaseHelper.Companion.getInstance
import org.secuso.privacyfriendlytodolist.tutorial.PrefManager
import org.secuso.privacyfriendlytodolist.tutorial.TutorialActivity
import org.secuso.privacyfriendlytodolist.util.PinUtil.hasPin
import org.secuso.privacyfriendlytodolist.view.ExpandableTodoTaskAdapter.SortTypes
import org.secuso.privacyfriendlytodolist.view.calendar.CalendarActivity
import org.secuso.privacyfriendlytodolist.view.dialog.PinDialog
import org.secuso.privacyfriendlytodolist.view.dialog.PinDialog.PinCallback
import org.secuso.privacyfriendlytodolist.view.dialog.ProcessTodoListDialog
import org.secuso.privacyfriendlytodolist.view.dialog.ProcessTodoSubTaskDialog
import org.secuso.privacyfriendlytodolist.view.dialog.ProcessTodoTaskDialog

/**
 * Created by Sebastian Lutz on 12.03.2018.
 *
 * This Activity handles the navigation and operation on lists and tasks.
 *
 */
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    // Fragment administration
    private var currentFragment: Fragment? = null
    private val fragmentManager = supportFragmentManager

    //TodoTask administration
    private var rl: RelativeLayout? = null
    private var exLv: ExpandableListView? = null
    private var tv: TextView? = null
    private var expandableTodoTaskAdapter: ExpandableTodoTaskAdapter? = null
    private var initialAlert: TextView? = null
    private var secondAlert: TextView? = null
    private var optionFab: FloatingActionButton? = null

    // Database administration
    var dbHelper: DatabaseHelper? = null
        private set
    private var mPref: SharedPreferences? = null

    // TodoList administration
    private var todoLists: ArrayList<TodoList>? = ArrayList()
    var dummyList // use this list if you need a container for tasks that does not exist in the database (e.g. to show all tasks, tasks of today etc.)
            : TodoList? = null
    var clickedList // reference of last clicked list for fragment
            : TodoList? = null
    private val mRecyclerView: TodoRecyclerView? = null
    private var adapter: TodoListAdapter? = null
    private val containerActivity: MainActivity? = null

    // Service that triggers notifications for upcoming tasks
    private var reminderService: ReminderService? = null

    // GUI
    private var navigationView: NavigationView? = null
    private var navigationBottomView: NavigationView? = null
    private var toolbar: Toolbar? = null
    private var drawer: DrawerLayout? = null

    // Others
    private var inList = false
    var isInitialized = false
    var isUnlocked = false
    var unlockUntil: Long = -1
    var affectedRows = 0
    var notificationDone = 0
    private var activeList = -1

    //Pomodoro
    private var pomodoroInstalled = false
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        menuInflater.inflate(R.menu.search, menu)
        menuInflater.inflate(R.menu.add_list, menu)
        val searchItem = menu.findItem(R.id.ac_search)
        val searchView = MenuItemCompat.getActionView(searchItem) as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                collapseAll()
                expandableTodoTaskAdapter!!.setQueryString(query)
                expandableTodoTaskAdapter!!.notifyDataSetChanged()
                return false
            }

            override fun onQueryTextChange(query: String): Boolean {
                collapseAll()
                expandableTodoTaskAdapter!!.setQueryString(query)
                expandableTodoTaskAdapter!!.notifyDataSetChanged()
                return false
            }
        })
        val priotiryGroup = menu.findItem(R.id.ac_group_by_prio)
        priotiryGroup.isChecked = mPref!!.getBoolean("PRIORITY", false)
        val deadlineGroup = menu.findItem(R.id.ac_sort_by_deadline)
        deadlineGroup.isChecked = mPref!!.getBoolean("DEADLINE", false)
        return super.onCreateOptionsMenu(menu)
    }

    private fun collapseAll() {
        // collapse all elements on view change.
        // the expandable list view keeps the expanded indices, so other items
        // get expanded, when they get the old expanded index
        val groupCount = expandableTodoTaskAdapter!!.groupCount
        for (i in 0 until groupCount) exLv!!.collapseGroup(i)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var checked = false
        var sortType: SortTypes
        sortType = SortTypes.DEADLINE
        collapseAll()
        when (item.itemId) {
            R.id.ac_add -> {
                startListDialog()
                addListToNav()
            }

            R.id.ac_show_all_tasks -> {
                expandableTodoTaskAdapter!!.setFilter(ExpandableTodoTaskAdapter.Filter.ALL_TASKS)
                expandableTodoTaskAdapter!!.notifyDataSetChanged()
                mPref!!.edit().putString("FILTER", "ALL_TASKS").commit()
                return true
            }

            R.id.ac_show_open_tasks -> {
                expandableTodoTaskAdapter!!.setFilter(ExpandableTodoTaskAdapter.Filter.OPEN_TASKS)
                expandableTodoTaskAdapter!!.notifyDataSetChanged()
                mPref!!.edit().putString("FILTER", "OPEN_TASKS").commit()
                return true
            }

            R.id.ac_show_completed_tasks -> {
                expandableTodoTaskAdapter!!.setFilter(ExpandableTodoTaskAdapter.Filter.COMPLETED_TASKS)
                expandableTodoTaskAdapter!!.notifyDataSetChanged()
                mPref!!.edit().putString("FILTER", "COMPLETED_TASKS").commit()
                return true
            }

            R.id.ac_group_by_prio -> {
                checked = !item.isChecked
                item.isChecked = checked
                sortType = SortTypes.PRIORITY
                mPref!!.edit().putBoolean("PRIORITY", checked).commit()
            }

            R.id.ac_sort_by_deadline -> {
                checked = !item.isChecked
                item.isChecked = checked
                sortType = SortTypes.DEADLINE
                mPref!!.edit().putBoolean("DEADLINE", checked).commit()
            }

            else -> return super.onOptionsItemSelected(item)
        }
        if (checked) {
            expandableTodoTaskAdapter!!.addSortCondition(sortType)
        } else {
            expandableTodoTaskAdapter!!.removeSortCondition(sortType)
        }
        expandableTodoTaskAdapter!!.notifyDataSetChanged()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0) {
            finish()
            return
        }
        val prefManager = PrefManager(this)
        if (prefManager.isFirstTimeLaunch) {
            prefManager.setFirstTimeValues(this)
            startTut()
            finish()
        }
        if (savedInstanceState != null) {
            restore(savedInstanceState)
        } else {
            isUnlocked = false
            unlockUntil = -1
        }
        setContentView(R.layout.activity_main)
        rl = findViewById<View>(R.id.relative_task) as RelativeLayout
        exLv = findViewById<View>(R.id.exlv_tasks) as ExpandableListView
        tv = findViewById<View>(R.id.tv_empty_view_no_tasks) as TextView
        optionFab = findViewById<View>(R.id.fab_new_task) as FloatingActionButton
        initialAlert = findViewById<View>(R.id.initial_alert) as TextView
        secondAlert = findViewById<View>(R.id.second_alert) as TextView
        hints()
        dbHelper = getInstance(this)
        mPref = PreferenceManager.getDefaultSharedPreferences(this)

        //Try to snooze the task by notification
        /*if (savedInstanceState == null) {
            Bundle b = getIntent().getExtras();
            if (b != null){
                notificationDone = b.getInt("snooze");
                int taskID = b.getInt("taskId");
                TodoList tasks = getTodoTasks();
                TodoTask currentTask = tasks.getTasks().get(taskID);
                currentTask.setReminderTime(System.currentTimeMillis() + notificationDone);
                sendToDatabase(currentTask);

            }
        } */if (intent.getIntExtra(COMMAND, -1) == COMMAND_UPDATE) {
            updateTodoFromPomodoro()
        }
        authAndGuiInit(savedInstanceState)
        val defaultList = TodoList()
        defaultList.setDummyList()
        saveTodoListInDb(dbHelper!!.writableDatabase, defaultList)
        if (activeList != -1) {
            showTasksOfList(activeList)
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(KEY_TODO_LISTS, todoLists)
        outState.putParcelable(KEY_CLICKED_LIST, clickedList)
        outState.putParcelable(KEY_DUMMY_LIST, dummyList)
        outState.putBoolean(KEY_IS_UNLOCKED, isUnlocked)
        outState.putLong(KEY_UNLOCK_UNTIL, unlockUntil)
        outState.putInt(KEY_ACTIVE_LIST, activeList)
    }

    private fun authAndGuiInit(savedInstanceState: Bundle?) {
        if (hasPin(this) && !isUnlocked && (unlockUntil == -1L || System.currentTimeMillis() > unlockUntil)) {
            val dialog = PinDialog(this)
            dialog.pinCallback = object : PinCallback {
                override fun accepted() {
                    initActivity(savedInstanceState)
                }

                override fun declined() {
                    finishAffinity()
                }

                override fun resetApp() {
                    PreferenceManager.getDefaultSharedPreferences(this@MainActivity).edit().clear()
                        .commit()
                    dbHelper!!.deleteAll()
                    dbHelper!!.createAll()
                    val intent = Intent(this@MainActivity, MainActivity::class.java)
                    dialog.dismiss()
                    startActivity(intent)
                }
            }
            dialog.show()
        } else {
            initActivity(savedInstanceState)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        restore(savedInstanceState)
    }

    private fun restore(savedInstanceState: Bundle) {
        todoLists = savedInstanceState.getParcelableArrayList(KEY_TODO_LISTS)
        clickedList = savedInstanceState.getParcelable(KEY_CLICKED_LIST)
        dummyList = savedInstanceState.getParcelable(KEY_DUMMY_LIST)
        isUnlocked = savedInstanceState.getBoolean(KEY_IS_UNLOCKED)
        unlockUntil = savedInstanceState.getLong(KEY_UNLOCK_UNTIL)
        activeList = savedInstanceState.getInt(KEY_ACTIVE_LIST)
    }

    fun initActivity(savedInstanceState: Bundle?) {
        isUnlocked = true
        getTodoLists(true)
        val extras = intent.extras
        val preferences = PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
        showAllTasks()
        //currentFragment = fragmentManager.findFragmentByTag(KEY_FRAGMENT_CONFIG_CHANGE_SAVE);

        // check if app was started by clicking on a reminding notification
        if (extras != null && TodoTasksFragment.KEY == extras.getString(
                KEY_SELECTED_FRAGMENT_BY_NOTIFICATION
            )
        ) {
            val dueTask = extras.getParcelable<TodoTask>(TodoTask.PARCELABLE_KEY)
            val bundle = Bundle()
            bundle.putInt(TodoList.UNIQUE_DATABASE_ID, dueTask!!.listId)
            bundle.putBoolean(TodoTasksFragment.SHOW_FLOATING_BUTTON, true)
            currentFragment = TodoTasksFragment()
            currentFragment?.arguments = bundle
        } else {
            if (currentFragment == null) {
                showAllTasks()
                Log.i(TAG, "Activity was not retained.")
            } else {

                // restore state before configuration change
                if (savedInstanceState != null) {
                    todoLists = savedInstanceState.getParcelableArrayList(KEY_TODO_LISTS)
                    clickedList = savedInstanceState[KEY_CLICKED_LIST] as TodoList?
                    dummyList = savedInstanceState[KEY_DUMMY_LIST] as TodoList?
                } else {
                    Log.i(TAG, "Could not restore old state because savedInstanceState is null.")
                }
                Log.i(TAG, "Activity was retained.")
            }
        }
        guiSetup()
        this.isInitialized = true
        inList = false
    }

    public override fun onStart() {
        super.onStart()
        uncheckNavigationEntries()
        if (navigationView != null) {
            navigationView!!.menu.getItem(0).isChecked = true
        }
    }

    private fun guiSetup() {

        // toolbar setup
        toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        // side menu setup
        drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        val toggle = ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer!!.setDrawerListener(toggle)
        toggle.syncState()
        addListToNav()

        //LinearLayout l = (LinearLayout) findViewById(R.id.footer);
        navigationView = findViewById<View>(R.id.nav_view) as NavigationView
        navigationBottomView = findViewById<View>(R.id.nav_view_bottom) as NavigationView
        navigationView!!.setNavigationItemSelectedListener(this)
        navigationBottomView!!.setNavigationItemSelectedListener(this)
        navigationView!!.menu.getItem(0).isChecked = true
    }

    fun uncheckNavigationEntries() {
        // uncheck all navigtion entries
        if (navigationView != null) {
            val size = navigationView!!.menu.size()
            for (i in 0 until size) {
                navigationView!!.menu.getItem(i).isChecked = false
            }
            Log.i(TAG, "Navigation entries unchecked.")
        }
        if (navigationBottomView != null) {
            val size = navigationBottomView!!.menu.size()
            for (i in 0 until size) {
                navigationBottomView!!.menu.getItem(i).isChecked = false
            }
            Log.i(TAG, "Navigation-Bottom entries unchecked.")
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId
        if (id == R.id.nav_settings) {
            uncheckNavigationEntries()
            val intent = Intent(this, Settings::class.java)
            unlockUntil = System.currentTimeMillis() + UnlockPeriod
            startActivity(intent)
        } else if (id == R.id.nav_tutorial) {
            uncheckNavigationEntries()
            val intent = Intent(this, TutorialActivity::class.java)
            unlockUntil = System.currentTimeMillis() + UnlockPeriod
            startActivity(intent)
        } else if (id == R.id.menu_calendar_view) {
            uncheckNavigationEntries()
            val intent = Intent(this, CalendarActivity::class.java)
            unlockUntil = System.currentTimeMillis() + UnlockPeriod
            startActivity(intent)
        } else if (id == R.id.nav_trash) {
            uncheckNavigationEntries()
            val intent = Intent(this, RecyclerActivity::class.java)
            unlockUntil = System.currentTimeMillis() + UnlockPeriod
            startActivity(intent)
        } else if (id == R.id.nav_about) {
            uncheckNavigationEntries()
            val intent = Intent(this, AboutActivity::class.java)
            unlockUntil = System.currentTimeMillis() + UnlockPeriod
            startActivity(intent)
        } else if (id == R.id.nav_help) {
            uncheckNavigationEntries()
            val intent = Intent(this, HelpActivity::class.java)
            unlockUntil = System.currentTimeMillis() + UnlockPeriod
            startActivity(intent)
        } else if (id == R.id.menu_home) {
            uncheckNavigationEntries()
            inList = false
            showAllTasks()
            toolbar!!.setTitle(R.string.home)
            item.isCheckable = true
            item.isChecked = true
        } else if (id == R.id.nav_dummy1 || id == R.id.nav_dummy2 || id == R.id.nav_dummy3) {
            if (!inList) {
                uncheckNavigationEntries()
                navigationView!!.menu.getItem(0).isChecked = true
                return false
            }
            if (inList) return false
        } else {
            showTasksOfList(id)
            toolbar!!.title = item.title
            item.isChecked = true
        }
        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onStop() {
        isUnlocked = false
        super.onStop()
    }

    override fun onRestart() {
        super.onRestart()
    }

    override fun onResume() {
        super.onResume()
        Log.d("RESUME", "resume")
        // Check if Pomodoro is installed
        pomodoroInstalled = checkIfPomodoroInstalled()
        if (this.isInitialized && !isUnlocked && (unlockUntil == -1L || System.currentTimeMillis() > unlockUntil)) {
            // restart activity to show pin dialog again
            //Intent intent = new Intent(this, MainActivity.class);
            //finish();
            //startActivity(intent);
            if (reminderService == null) bindToReminderService()
            guiSetup()
            if (activeList == -1) {
                showAllTasks()
            } else {
                showTasksOfList(activeList)
            }
            return
        }

        // isUnlocked might be false when returning from another activity. set to true if the unlock period was not expired:
        isUnlocked = isUnlocked || unlockUntil != -1L && System.currentTimeMillis() <= unlockUntil
        unlockUntil = -1
        if (reminderService == null) {
            bindToReminderService()
        }
        Log.i(TAG, "onResume()")
    }

    override fun onDestroy() {
        if (reminderService != null) {
            unbindService(reminderServiceConnection)
            reminderService = null
            Log.i(TAG, "service is now null")
        }
        super.onDestroy()
    }

    override fun onUserLeaveHint() {
        // prevents unlocking the app by rotating while the app is inactive and then returning
        isUnlocked = false
    }

    private fun bindToReminderService() {
        Log.i(TAG, "bindToReminderService()")
        val intent = Intent(this, ReminderService::class.java)
        bindService(
            intent,
            reminderServiceConnection,
            0
        ) // no Context.BIND_AUTO_CREATE, because service will be started by startService and thus live longer than this activity
        startService(intent)
    }

    override fun onBackPressed() {
        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else if (inList) {
            showAllTasks()
            toolbar!!.setTitle(R.string.home)
            inList = false
            uncheckNavigationEntries()
            navigationView!!.menu.getItem(0).isChecked = true
        } else {
//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setMessage(R.string.exit_app);
//            builder.setCancelable(true);
//            builder.setPositiveButton(R.string.exit_positive, new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    finish();
//                }
//            });
//            builder.setNegativeButton(R.string.exit_negative, new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    dialog.cancel();
//                }
//            });
//            AlertDialog alert = builder.create();
//            alert.show();
            super.onBackPressed()
        }
    }

    fun getTodoLists(reload: Boolean): ArrayList<TodoList>? {
        if (reload) {
            if (dbHelper != null) todoLists = getAllToDoLists(dbHelper!!.readableDatabase)
        }
        return todoLists
    }

    val todoTasks: TodoList?
        get() {
            var tasks = ArrayList<TodoTask>()
            if (dbHelper != null) {
                tasks = getAllToDoTasks(dbHelper!!.readableDatabase)
                for (i in tasks.indices) {
                    dummyList!!.setDummyList()
                    dummyList!!.name = "All tasks"
                    dummyList!!.tasks = tasks
                }
            }
            return dummyList
        }
    private val reminderServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            Log.d("ServiceConnection", "connected")
            reminderService = (binder as ReminderServiceBinder).service
        }

        //binder comes from server to communicate with method's of
        override fun onServiceDisconnected(className: ComponentName) {
            Log.d("ServiceConnection", "disconnected")
            reminderService = null
        }
    }

    fun notifyReminderService(currentTask: TodoTask) {

        // TODO This method is called from other fragments as well (e.g. after opening MainActivity by reminder). In such cases the service is null and alarms cannot be updated. Fix this!
        if (reminderService != null) {

            // Report changes to the reminder task if the reminder time is prior to the deadline or if no deadline is set at all. The reminder time must always be after the the current time. The task must not be completed.
            if ((currentTask.reminderTime < currentTask.deadline || !currentTask.hasDeadline()) && !currentTask.done) {
                reminderService!!.processTask(currentTask)
                Log.i(TAG, "Reminder is set!")
            } else {
                Log.i(TAG, "Reminder service was not informed about the task " + currentTask.name)
            }
        } else {
            Log.i(TAG, "Service is null. Cannot update alarms")
        }
    }

    // returns true if object was created in the database
    fun sendToDatabase(todo: BaseTodo): Boolean {
        var databaseID: Int
        var errorMessage = ""

        // call appropriate method depending on type
        if (todo is TodoList) {
            databaseID = saveTodoListInDb(dbHelper!!.writableDatabase, todo)
            errorMessage = getString(R.string.list_to_db_error)
        } else if (todo is TodoTask) {
            databaseID = saveTodoTaskInDb(dbHelper!!.writableDatabase, todo)
            notifyReminderService(todo)
            errorMessage = getString(R.string.task_to_db_error)
        } else if (todo is TodoSubTask) {
            databaseID = saveTodoSubTaskInDb(dbHelper!!.writableDatabase, todo)
            errorMessage = getString(R.string.subtask_to_db_error)
        } else {
            throw IllegalArgumentException("Cannot save unknown descendant of BaseTodo in the database.")
        }

        // set unique database id (primary key) to the current object
        if (databaseID == -1) {
            Log.e(TAG, errorMessage)
            return false
        } else if (databaseID != DBQueryHandler.NO_CHANGES) {
            todo.id = databaseID
            return true
        }
        return false
    }

    fun getListByID(id: Int): TodoList? {
        for (currentList in todoLists!!) {
            if (currentList.id == id) return currentList
        }
        return null
    }

    //Adds To do-Lists to the navigation-drawer
    private fun addListToNav() {
        val nv = findViewById<View>(R.id.nav_view) as NavigationView
        val navMenu = nv.menu
        navMenu.clear()
        val mf = MenuInflater(applicationContext)
        mf.inflate(R.menu.nav_content, navMenu)
        val help = ArrayList<TodoList>()
        help.addAll(todoLists!!)
        for (i in help.indices) {
            val name = help[i].name
            val id = help[i].id
            val item = navMenu.add(R.id.drawer_group2, id, 1, name)
            item.isCheckable = true
            item.setIcon(R.drawable.ic_label_black_24dp)
            val v = ImageButton(this, null, R.style.BorderlessButtonStyle)
            v.setImageResource(R.drawable.ic_delete_black_24dp)
            v.setOnClickListener(OnCustomMenuItemClickListener(help[i].id, this@MainActivity))
            item.actionView = v
        }
    }

    // Method to add a new To do-List
    private fun startListDialog() {
        dbHelper = getInstance(this)
        todoLists = getAllToDoLists(dbHelper!!.readableDatabase)
        adapter = TodoListAdapter(this, todoLists)
        val pl = ProcessTodoListDialog(this)
        pl.setDialogResult(object: TodoCallback {
            override fun finish(b: BaseTodo?) {
                if (b is TodoList) {
                    todoLists!!.add(b)
                    adapter!!.updateList(todoLists) // run filter again
                    adapter!!.notifyDataSetChanged()
                    sendToDatabase(b)
                    hints()
                    addListToNav()
                    Log.i(TAG, "list added")
                }
            }})
        pl.show()
    }

    // Method starting tutorial
    private fun startTut() {
        val intent = Intent(this@MainActivity, TutorialActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    //ClickListener to delete a Todo-List
    inner class OnCustomMenuItemClickListener internal constructor(
        private val id: Int,
        private val context: Context
    ) : View.OnClickListener {
        override fun onClick(view: View) {
            val builder1 = AlertDialog.Builder(
                context
            )
            builder1.setMessage(R.string.alert_listdelete)
            builder1.setCancelable(true)
            builder1.setPositiveButton(
                R.string.alert_delete_yes
            ) { dialog, setId ->
                val todoLists = getAllToDoLists(
                    getInstance(
                        context
                    )!!.readableDatabase
                )
                for (t in todoLists) {
                    if (t.id == id) {
                        deleteTodoList(getInstance(context)!!.writableDatabase, t)
                    }
                }
                dialog.cancel()
                val intent = Intent(context, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            }
            builder1.setNegativeButton(
                R.string.alert_delete_no
            ) { dialog, id -> dialog.cancel() }
            val alert11 = builder1.create()
            alert11.show()
            return
        }
    }

    private fun showAllTasks() {
        dbHelper = getInstance(this)
        val tasks: ArrayList<TodoTask>
        tasks = getAllToDoTasks(dbHelper!!.readableDatabase)
        expandableTodoTaskAdapter = ExpandableTodoTaskAdapter(this, tasks)
        exLv!!.onItemLongClickListener = OnItemLongClickListener { parent, view, position, id ->
            val groupPosition = ExpandableListView.getPackedPositionGroup(id)
            if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                val childPosition = ExpandableListView.getPackedPositionChild(id)
                expandableTodoTaskAdapter!!.setLongClickedSubTaskByPos(groupPosition, childPosition)
            } else {
                expandableTodoTaskAdapter!!.setLongClickedTaskByPos(groupPosition)
            }
            registerForContextMenu(exLv)
            false
        }
        exLv!!.setAdapter(expandableTodoTaskAdapter)
        exLv!!.emptyView = tv
        optionFab!!.visibility = View.VISIBLE
        initFab(0, false)
        hints()
    }

    private fun showTasksOfList(id: Int) {
        uncheckNavigationEntries()
        inList = true
        if (navigationView != null) {
            for (i in 0 until navigationView!!.menu.size()) {
                val item = navigationView!!.menu.getItem(i)
                item.isChecked = item.itemId == id
                if (item.itemId == id) {
                    toolbar!!.title = item.title
                }
            }
            Log.i(TAG, "Navigation entries unchecked.")
        }
        dbHelper = getInstance(this)
        val help = ArrayList<TodoTask>()
        val lists: ArrayList<TodoList> = getAllToDoLists(dbHelper!!.readableDatabase)
        activeList = id
        for (i in lists.indices) {
            if (id == lists[i].id) {
                help.addAll(lists[i].tasks)
            }
        }
        expandableTodoTaskAdapter = ExpandableTodoTaskAdapter(this, help)
        exLv!!.setAdapter(expandableTodoTaskAdapter)
        exLv!!.emptyView = tv
        optionFab!!.visibility = View.VISIBLE
        initFab(id, true)
    }

    //idExists describes if id is given from list (true) or new task is created in all-tasks (false)
    private fun initFab(id: Int, idExists: Boolean) {
        dbHelper = getInstance(this)
        val tasks: ArrayList<TodoTask> = getAllToDoTasks(dbHelper!!.readableDatabase)
        //final ExpandableTodoTaskAdapter taskAdapter = new ExpandableTodoTaskAdapter(this, tasks);
        optionFab!!.setOnClickListener {
            val pt = ProcessTodoTaskDialog(this@MainActivity)
            pt.setListSelector(id, idExists)
            pt.setDialogResult(object : TodoCallback {
                override fun finish(b: BaseTodo?) {
                    if (b is TodoTask) {
                        //((TodoTask) b).setListId(helpId);
                        sendToDatabase(b)
                        hints()
                        //show List if created in certain list, else show all tasks
                        if (idExists) {
                            showTasksOfList(id)
                        } else showAllTasks()
                    }
                }
            })
            pt.show()
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu, v: View,
        menuInfo: ContextMenuInfo
    ) {
        val info = menuInfo as ExpandableListContextMenuInfo
        val type = ExpandableListView.getPackedPositionType(info.packedPosition)
        val inflater = this.menuInflater
        menu.setHeaderView(
            getMenuHeader(
                baseContext,
                baseContext.getString(R.string.select_option)
            )
        )
        val workItemId: Int
        // context menu for child items
        workItemId = if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            inflater.inflate(R.menu.todo_subtask_long_click, menu)
            R.id.work_subtask
        } else { // context menu for group items
            inflater.inflate(R.menu.todo_task_long_click, menu)
            R.id.work_task
        }
        if (pomodoroInstalled) {
            menu.findItem(workItemId).isVisible = true
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val longClickedTodo = expandableTodoTaskAdapter!!.longClickedTodo
        when (item.itemId) {
            R.id.change_subtask -> {
                val dialog = ProcessTodoSubTaskDialog(this, longClickedTodo!!.right!!)
                dialog.titleEdit()
                dialog.setDialogResult(object: TodoCallback {
                    override fun finish(b: BaseTodo?) {
                        if (b is TodoSubTask) {
                            sendToDatabase(b)
                            expandableTodoTaskAdapter!!.notifyDataSetChanged()
                            Log.i(TAG, "subtask altered")
                        }
                    }
                })
                dialog.show()
            }

            R.id.delete_subtask -> {
                affectedRows =
                    deleteTodoSubTask(dbHelper!!.writableDatabase, longClickedTodo!!.right)
                longClickedTodo.left!!.subTasks.remove(longClickedTodo.right)
                if (affectedRows == 1) Toast.makeText(
                    baseContext,
                    getString(R.string.subtask_removed),
                    Toast.LENGTH_SHORT
                ).show() else Log.d(
                    TAG,
                    "Subtask was not removed from the database. Maybe it was not added beforehand (then this is no error)?"
                )
                expandableTodoTaskAdapter!!.notifyDataSetChanged()
            }

            R.id.change_task -> {
                val listIDold = longClickedTodo!!.left!!.listId
                val editTaskDialog = ProcessTodoTaskDialog(this, longClickedTodo.left!!)
                editTaskDialog.titleEdit()
                editTaskDialog.setListSelector(longClickedTodo.left.listId, true)
                editTaskDialog.setDialogResult(object : TodoCallback {
                    override fun finish(alteredTask: BaseTodo?) {
                        if (alteredTask is TodoTask) {
                            sendToDatabase(alteredTask)
                            expandableTodoTaskAdapter!!.notifyDataSetChanged()
                            if (inList && listIDold != -3) {
                                showTasksOfList(listIDold)
                            } else {
                                showAllTasks()
                            }
                        }
                    }
                })
                editTaskDialog.show()
            }

            R.id.delete_task -> {
                val snackbar =
                    Snackbar.make(optionFab!!, R.string.task_removed, Snackbar.LENGTH_LONG)
                val subTasks = longClickedTodo!!.left!!.subTasks
                for (ts in subTasks) {
                    putSubtaskInTrash(dbHelper!!.writableDatabase, ts)
                }
                affectedRows = putTaskInTrash(dbHelper!!.writableDatabase, longClickedTodo.left)
                if (affectedRows == 1) {
                    hints()
                } else Log.d(
                    TAG,
                    "Task was not removed from the database. Maybe it was not added beforehand (then this is no error)?"
                )

                // Dependent on the current View, update All-tasks or a certain List
                if (inList && longClickedTodo.left!!.listId != -3) {
                    showTasksOfList(longClickedTodo.left.listId)
                } else {
                    showAllTasks()
                }
                snackbar.setAction(R.string.snack_undo) {
                    val subTasks = longClickedTodo.left!!.subTasks
                    recoverTasks(dbHelper!!.writableDatabase, longClickedTodo.left)
                    for (ts in subTasks) {
                        recoverSubtasks(dbHelper!!.writableDatabase, ts)
                    }
                    if (inList && longClickedTodo.left.listId != -3) {
                        showTasksOfList(longClickedTodo.left.listId)
                    } else {
                        showAllTasks()
                    }
                    hints()
                }
                snackbar.show()
            }

            R.id.work_task -> {
                Log.i(MainActivity::class.java.simpleName, "START TASK")
                sendToPomodoro(longClickedTodo!!.left)
            }

            R.id.work_subtask -> {
                Log.i(MainActivity::class.java.simpleName, "START SUBTASK")
                sendToPomodoro(longClickedTodo!!.right)
            }

            else -> throw IllegalArgumentException("Invalid menu item selected.")
        }
        return super.onContextItemSelected(item)
    }

    private fun sendToPomodoro(todo: BaseTodo?) {
        val pomodoro = Intent(POMODORO_ACTION)
        val todoId = todo!!.id
        val todoDescription: String?
        todoDescription = if (todo.description == null) {
            ""
        } else {
            todo.description
        }
        val todoName = todo.name
        pomodoro.putExtra("todo_id", todoId)
            .putExtra("todo_name", todoName)
            .putExtra("todo_description", todoDescription)
            .putExtra("todo_progress", todo.progress)
            .setPackage("org.secuso.privacyfriendlyproductivitytimer").flags =
            Intent.FLAG_INCLUDE_STOPPED_PACKAGES
        sendBroadcast(pomodoro, "org.secuso.privacyfriendlytodolist.TODO_PERMISSION")
        finish()
    }

    private fun updateTodoFromPomodoro() {
        val todoRe = TodoTask()
        todoRe.setChangedFromPomodoro() //Change the dbState to UPDATE_FROM_POMODORO
        //todoRe.setPriority(TodoTask.Priority.HIGH);
        todoRe.name = intent.getStringExtra("todo_name")
        todoRe.id = intent.getIntExtra("todo_id", -1)
        todoRe.progress = intent.getIntExtra("todo_progress", -1)
        if (todoRe.progress == 100) {
            // Set task as done
            todoRe.done = true
            //todoRe.doneStatusChanged();
        }
        if (todoRe.progress != -1) {
            sendToDatabase(todoRe) //Update the existing entry, if no subtask
        }

        //super.onResume();
    }

    fun hints() {
        val anim: Animation = AlphaAnimation(0.0f, 1.0f)
        dbHelper = getInstance(this)
        if (getAllToDoTasks(dbHelper!!.readableDatabase).size == 0 &&
            getAllToDoLists(dbHelper!!.readableDatabase).size == 0
        ) {
            initialAlert!!.visibility = View.VISIBLE
            anim.duration = 1500
            anim.startOffset = 20
            anim.repeatMode = Animation.REVERSE
            anim.repeatCount = Animation.INFINITE
            initialAlert!!.startAnimation(anim)
        } else  /*if (DBQueryHandler.getAllToDoTasks(dbHelper.getReadableDatabase()).size() > 0 ||
                DBQueryHandler.getAllToDoLists(dbHelper.getReadableDatabase()).size() > 0) */ {
            initialAlert!!.visibility = View.GONE
            initialAlert!!.clearAnimation()
        }
        if (getAllToDoTasks(dbHelper!!.readableDatabase).size == 0) {
            secondAlert!!.visibility = View.VISIBLE
            anim.duration = 1500
            anim.startOffset = 20
            anim.repeatMode = Animation.REVERSE
            anim.repeatCount = Animation.INFINITE
            secondAlert!!.startAnimation(anim)
        } else {
            secondAlert!!.visibility = View.GONE
            secondAlert!!.clearAnimation()
        }
    }

    private fun checkIfPomodoroInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo("org.secuso.privacyfriendlyproductivitytimer", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    companion object {
        const val COMMAND = "command"
        const val COMMAND_UPDATE = 3
        private val TAG = MainActivity::class.java.simpleName

        // Keys
        private const val KEY_TODO_LISTS = "restore_todo_list_key_with_savedinstancestate"
        private const val KEY_CLICKED_LIST = "restore_clicked_list_with_savedinstancestate"
        private const val KEY_DUMMY_LIST = "restore_dummy_list_with_savedinstancestate"
        private const val KEY_IS_UNLOCKED = "restore_is_unlocked_key_with_savedinstancestate"
        private const val KEY_UNLOCK_UNTIL = "restore_unlock_until_key_with_savedinstancestate"
        const val KEY_SELECTED_FRAGMENT_BY_NOTIFICATION = "fragment_choice"
        private const val KEY_ACTIVE_LIST = "KEY_ACTIVE_LIST"
        private const val POMODORO_ACTION = "org.secuso.privacyfriendlytodolist.TODO_ACTION"
        private const val UnlockPeriod: Long =
            30000 // keep the app unlocked for 30 seconds after switching to another activity (settings/help/about)
    }
}