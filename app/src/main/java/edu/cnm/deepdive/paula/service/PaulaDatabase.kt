package edu.cnm.deepdive.paula.service

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import edu.cnm.deepdive.paula.model.dao.CategoryDao
import edu.cnm.deepdive.paula.model.dao.RoutineDao
import edu.cnm.deepdive.paula.model.dao.RoutineLogDao
import edu.cnm.deepdive.paula.model.dao.TaskDao
import edu.cnm.deepdive.paula.model.dao.UserDao
import edu.cnm.deepdive.paula.model.dao.VoicePromptDao
import edu.cnm.deepdive.paula.model.entity.Category
import edu.cnm.deepdive.paula.model.entity.Routine
import edu.cnm.deepdive.paula.model.entity.RoutineLog
import edu.cnm.deepdive.paula.model.entity.Task
import edu.cnm.deepdive.paula.model.entity.User
import edu.cnm.deepdive.paula.model.entity.VoicePrompt
import edu.cnm.deepdive.paula.service.PaulaDatabase.Companion.DATABASE_VERSION

@Database(
    entities = [
        User::class,
        Category::class,
        VoicePrompt::class,
        Task::class,
        Routine::class,
        RoutineLog::class
    ],
    version = DATABASE_VERSION
)
@TypeConverters(Converters::class)
abstract class PaulaDatabase : RoomDatabase() {

    abstract fun getUserDao(): UserDao

    abstract fun getCategoryDao(): CategoryDao

    abstract fun getVoicePromptDao(): VoicePromptDao

    abstract fun getTaskDao(): TaskDao

    abstract fun getRoutineDao(): RoutineDao

    abstract fun getRoutineLogDao(): RoutineLogDao

    companion object {
        const val DATABASE_NAME = "paula-db"
        const val DATABASE_VERSION = 1
    }

}
