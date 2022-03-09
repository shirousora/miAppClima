package mx.kodemia.miappclima.net

import mx.kodemia.miappclima.models.Main
import mx.kodemia.miappclima.models.Sys
import mx.kodemia.miappclima.models.Weather
import mx.kodemia.miappclima.models.Wind

data class WeatherEntity(
    val base: String,
    val main: Main,
    val sys: Sys,
    val id: Int,
    val name: String,
    val wind: Wind,
    val weather: List<Weather>,
    val dt: Long
)
