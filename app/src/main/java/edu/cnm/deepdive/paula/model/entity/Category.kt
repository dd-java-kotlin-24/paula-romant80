package edu.cnm.deepdive.paula.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "category",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class Category(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "category_id")
    val id: Long = 0,

    val name: String,

    @ColumnInfo(name = "color_hex")
    val colorHex: String,

    @ColumnInfo(name = "icon_name")
    val iconName: String? = null
)