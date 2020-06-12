package com.raywenderlich.placebook.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.raywenderlich.placebook.model.Bookmark

@Database(entities = [Bookmark::class], version = 2)
// Identify a database class to Room
// entities is a required attribute defining an array of all entities.

abstract class PlaceBookDatabase: RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        private var instance: PlaceBookDatabase? = null

        fun getInstance(context: Context): PlaceBookDatabase {
            if (instance == null) {
                instance = Room.databaseBuilder(context.applicationContext,
                    PlaceBookDatabase::class.java, "PlaceBook")
                    .fallbackToDestructiveMigration()
                    // call the builder and tells Room to create new empty db if no migrations
                    .build()
            }
            return instance as PlaceBookDatabase
        }
    }
}