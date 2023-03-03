package com.ckds.movieapp.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stored_series", primaryKeys = ["id","category"])
data class StoredSeries(
    val id: Int,
    val category: String
)