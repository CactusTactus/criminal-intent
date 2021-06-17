package com.example.criminalintent.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class Crime(
    @PrimaryKey
    var id: UUID = UUID.randomUUID(),
    var title: String = "",
    var date: Date = Date(),
    var suspect: String = "",
    var phoneNumber: String = "",
    var isSolved: Boolean = false,
) {
    val photoFileName: String
        get() = "img_$id.jpg"
}