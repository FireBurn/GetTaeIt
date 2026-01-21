package uk.co.fireburn.gettaeit.shared.data

import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

class Converters {
    @TypeConverter
    fun fromUUIDList(value: List<UUID>?): String? {
        return value?.let { Gson().toJson(it) }
    }

    @TypeConverter
    fun toUUIDList(value: String?): List<UUID>? {
        val listType = object : TypeToken<List<UUID>>() {}.type
        return value?.let { Gson().fromJson(it, listType) }
    }
    
    @TypeConverter
    fun fromLatLng(latLng: LatLng?): String? {
        return latLng?.let { "${it.latitude},${it.longitude}" }
    }

    @TypeConverter
    fun toLatLng(value: String?): LatLng? {
        return value?.split(',')?.let {
            LatLng(it[0].toDouble(), it[1].toDouble())
        }
    }
    
    @TypeConverter
    fun fromIntList(value: List<Int>?): String? {
        return value?.let { Gson().toJson(it) }
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int>? {
        val listType = object : TypeToken<List<Int>>() {}.type
        return value?.let { Gson().fromJson(it, listType) }
    }
}
