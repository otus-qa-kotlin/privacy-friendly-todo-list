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

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.text.TextUtils
import android.util.Log
import org.secuso.privacyfriendlytodolist.model.database.tables.TTodoList
import org.secuso.privacyfriendlytodolist.model.database.tables.TTodoSubTask
import org.secuso.privacyfriendlytodolist.model.database.tables.TTodoTask
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale

/**
 *
 * Created by Sebastian Lutz on 13.3.2018.
 *
 * This class extends SQLiteOpenHelper and is responsible for fundamental things such as:
 *
 * - Create all tables mentioned above (#createAll)
 * - Delete all tables (#deleteAll)
 */
class DatabaseHelper private constructor(private val context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {
    @JvmOverloads
    fun deleteAll(db: SQLiteDatabase = this.writableDatabase) {
        db.execSQL("DROP TABLE " + TTodoList.TABLE_NAME)
        db.execSQL("DROP TABLE " + TTodoTask.TABLE_NAME)
        db.execSQL("DROP TABLE " + TTodoSubTask.TABLE_NAME)
    }

    @JvmOverloads
    fun createAll(db: SQLiteDatabase = this.writableDatabase) {
        db.execSQL(TTodoList.TABLE_CREATE)
        db.execSQL(TTodoTask.TABLE_CREATE)
        db.execSQL(TTodoSubTask.TABLE_CREATE)
    }

    override fun onCreate(db: SQLiteDatabase) {
        createAll(db)
        Log.i(TAG, "onCreate() finished")
    }

    /**
     * Taken from https://riggaroo.co.za/android-sqlite-database-use-onupgrade-correctly/ .
     * @param db the writeable database to update.
     * @param oldVersion the old version to update from
     * @param newVersion the new version to update to
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.e(TAG, "Updating table from $oldVersion to $newVersion")
        // You will not need to modify this unless you need to do some android specific things.
        // When upgrading the database, all you need to do is add a file to the assets folder and name it:
        // from_1_to_2.sql with the version that you are upgrading to as the last version.
        for (i in oldVersion until newVersion) {
            val migrationName = String.format(Locale.ENGLISH, "from_%d_to_%d.sql", i, i + 1)
            Log.d(TAG, "Looking for migration file: $migrationName")
            readAndExecuteSQLScript(db, context, migrationName)
        }
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    private fun readAndExecuteSQLScript(db: SQLiteDatabase, ctx: Context, fileName: String) {
        if (TextUtils.isEmpty(fileName)) {
            Log.d(TAG, "SQL script file name is empty")
            return
        }
        Log.d(TAG, "Script found. Executing...")
        val assetManager = ctx.assets
        var reader: BufferedReader? = null
        try {
            val `is` = assetManager.open(fileName)
            val isr = InputStreamReader(`is`)
            reader = BufferedReader(isr)
            executeSQLScript(db, reader)
        } catch (e: IOException) {
            Log.e(TAG, "IOException:", e)
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: IOException) {
                    Log.e(TAG, "IOException:", e)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun executeSQLScript(db: SQLiteDatabase, reader: BufferedReader) {
        var line: String
        var statement = StringBuilder()
        while (reader.readLine().also { line = it } != null) {
            statement.append(line)
            statement.append("\n")
            if (line.endsWith(";")) {
                db.execSQL(statement.toString())
                statement = StringBuilder()
            }
        }
    }

    companion object {
        private val TAG = DatabaseHelper::class.java.simpleName
        private var mInstance: DatabaseHelper? = null
        const val DATABASE_VERSION = 2
        const val DATABASE_NAME = "TodoDatabase.db"
        @JvmStatic
        fun getInstance(context: Context): DatabaseHelper? {
            if (mInstance == null) {
                mInstance = DatabaseHelper(context.applicationContext)
            }
            return mInstance
        }
    }
}