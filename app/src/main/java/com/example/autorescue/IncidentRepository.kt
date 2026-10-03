package com.example.autorescue

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class IncidentRepository(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            "autorescue_history",
            Context.MODE_PRIVATE
        )

    fun load(): List<IncidentHistoryEntry> {

        return runCatching {

            val raw =
                preferences.getString(
                    "items",
                    "[]"
                ) ?: "[]"

            val array =
                JSONArray(raw)

            buildList {

                for (
                index in 0 until array.length()
                ) {

                    val objectItem =
                        array.getJSONObject(index)

                    add(
                        IncidentHistoryEntry(

                            timestamp =
                                objectItem.getLong(
                                    "timestamp"
                                ),

                            priority =
                                objectItem.getString(
                                    "priority"
                                ),

                            score =
                                objectItem.getInt(
                                    "score"
                                ),

                            impactMagnitude =
                                objectItem
                                    .getDouble(
                                        "impactMagnitude"
                                    )
                                    .toFloat(),

                            rotationMagnitude =
                                objectItem
                                    .getDouble(
                                        "rotationMagnitude"
                                    )
                                    .toFloat(),

                            driverResponse =
                                objectItem.getString(
                                    "driverResponse"
                                ),

                            gpsAvailable =
                                objectItem.getBoolean(
                                    "gpsAvailable"
                                ),

                            latitude =
                                if (
                                    objectItem.isNull(
                                        "latitude"
                                    )
                                ) {
                                    null
                                } else {
                                    objectItem.getDouble(
                                        "latitude"
                                    )
                                },

                            longitude =
                                if (
                                    objectItem.isNull(
                                        "longitude"
                                    )
                                ) {
                                    null
                                } else {
                                    objectItem.getDouble(
                                        "longitude"
                                    )
                                }
                        )
                    )
                }
            }

        }.getOrDefault(
            emptyList()
        )
    }

    fun save(
        entry: IncidentHistoryEntry
    ) {

        val entries =
            (
                    listOf(entry) +
                            load()
                    ).take(20)

        val array =
            JSONArray()

        entries.forEach { item ->

            array.put(
                JSONObject().apply {

                    put(
                        "timestamp",
                        item.timestamp
                    )

                    put(
                        "priority",
                        item.priority
                    )

                    put(
                        "score",
                        item.score
                    )

                    put(
                        "impactMagnitude",
                        item.impactMagnitude
                    )

                    put(
                        "rotationMagnitude",
                        item.rotationMagnitude
                    )

                    put(
                        "driverResponse",
                        item.driverResponse
                    )

                    put(
                        "gpsAvailable",
                        item.gpsAvailable
                    )

                    put(
                        "latitude",
                        item.latitude
                    )

                    put(
                        "longitude",
                        item.longitude
                    )
                }
            )
        }

        preferences
            .edit()
            .putString(
                "items",
                array.toString()
            )
            .apply()
    }

    fun clear() {

        preferences
            .edit()
            .remove("items")
            .apply()
    }
}