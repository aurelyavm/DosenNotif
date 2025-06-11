package com.example.dosennotif.model

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Schedule(
    val id_dosen: String,
    val nama_dosen: String,
    val nidn_dosen: String,
    val id_periode: String,
    val id_program_studi: String,
    val nama_program_studi: String,
    val kode_mata_kuliah: String,
    val nama_mata_kuliah: String,
    val sks: String,
    val kelas: String,
    val hari: String,
    val jam_mulai: String,
    val jam_selesai: String,
    val ruang: String
) : Serializable {

    fun getFormattedDay(): String {
        return hari.trim()
    }

    fun getStartTime(): Date? {
        return try {
            val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            format.parse(jam_mulai)
        } catch (e: Exception) {
            null
        }
    }

    fun getEndTime(): Date? {
        return try {
            val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            format.parse(jam_selesai)
        } catch (e: Exception) {
            null
        }
    }

    fun getFormattedStartTime(): String {
        return try {
            val inputFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = inputFormat.parse(jam_mulai)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            jam_mulai
        }
    }

    fun getFormattedEndTime(): String {
        return try {
            val inputFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = inputFormat.parse(jam_selesai)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            jam_selesai
        }
    }

    fun getTimeRange(): String {
        return "${getFormattedStartTime()} - ${getFormattedEndTime()}"
    }

    fun getDayOfWeekNumber(): Int {
        return when (getFormattedDay().lowercase(Locale.getDefault())) {
            "senin" -> Calendar.MONDAY
            "selasa" -> Calendar.TUESDAY
            "rabu" -> Calendar.WEDNESDAY
            "kamis" -> Calendar.THURSDAY
            "jumat" -> Calendar.FRIDAY
            "sabtu" -> Calendar.SATURDAY
            "minggu" -> Calendar.SUNDAY
            else -> -1
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Schedule

        if (id_dosen != other.id_dosen) return false
        if (kode_mata_kuliah != other.kode_mata_kuliah) return false
        if (kelas != other.kelas) return false
        if (hari != other.hari) return false
        if (jam_mulai != other.jam_mulai) return false
        if (jam_selesai != other.jam_selesai) return false
        if (ruang != other.ruang) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id_dosen.hashCode()
        result = 31 * result + kode_mata_kuliah.hashCode()
        result = 31 * result + kelas.hashCode()
        result = 31 * result + hari.hashCode()
        result = 31 * result + jam_mulai.hashCode()
        result = 31 * result + jam_selesai.hashCode()
        result = 31 * result + ruang.hashCode()
        return result
    }
}