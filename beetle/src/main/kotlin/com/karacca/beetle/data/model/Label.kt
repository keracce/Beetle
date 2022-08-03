package com.karacca.beetle.data.model
import com.google.gson.annotations.SerializedName


/**
 * @author karacca
 * @date 25.07.2022
 */
 
data class Label(
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("node_id")
    val nodeId: String? = null,
    @SerializedName("url")
    val url: String? = null,
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("color")
    val color: String? = null,
    @SerializedName("default")
    val default: Boolean? = null
) {

    var selected = false
}