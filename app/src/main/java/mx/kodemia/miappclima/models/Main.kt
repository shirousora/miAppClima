package mx.kodemia.miappclima.models

data class Main(
    //mismos nombres que api
    val temp: Double,
    val feels_like: Double,
    val temp_min: Double,
    val temp_max: Double,
    val pressure: Int,
    val humidity: Int
)
