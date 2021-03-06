package mx.kodemia.miappclima.ui.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.kodemia.miappclima.BuildConfig.APPLICATION_ID
import mx.kodemia.miappclima.R
import mx.kodemia.miappclima.databinding.ActivityMainBinding
import mx.kodemia.miappclima.net.WeatherEntity
import mx.kodemia.miappclima.net.WeatherService
import mx.kodemia.miappclima.utils.checkForInternet
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivityError"
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 34

    private var latitude = ""
    private var longitude = ""

    private lateinit var binding: ActivityMainBinding
    /**
     * Punto de entrada para el API Fused Location Provider.
     */
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // SplashScreen API
        installSplashScreen()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (!checkPermissions()) {
            requestPermissions()
        } else {
            // despues de que se obtiene la location se ejecuta el setUpViewData con esa location
            getLastLocation(){ location ->
                setupViewData(location)
            }
        }
    }

    /**
     * Funciones de men??
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.actions_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_actualizar -> {
                // Toast.makeText(this, "Men?? seleccionado", Toast.LENGTH_SHORT).show()
                showCreateUserDialog("TEMP")
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Funci??n para consultar y cargar datos de clima
     */

    private fun setupViewData(location: Location) {

        if (checkForInternet(this)) {
            // Se coloca en este punto para permitir su ejecuci??n
            showIndicator(true)
            lifecycleScope.launch {
                latitude = location.latitude.toString()
                longitude = location.longitude.toString()
                formatResponse(getWeather())
            }
        } else {
            showError(getString(R.string.no_internet_access))
            binding.detailsContainer.isVisible = false
        }
    }
  //TODO change this retrofit to his own file
    private suspend fun getWeather(): WeatherEntity = withContext(Dispatchers.IO){
        Log.e(TAG, "CORR Lat: $latitude Long: $longitude")
        // showIndicator(true)
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service: WeatherService = retrofit.create(WeatherService::class.java)

        service.getWeatherById(latitude, longitude, "metric", "sp", "30ba6cd1ad33ea67e2dfd78a8d28ae62")
    }

    /**
     * Funci??n para mostrar los datos obtenidos de OpenWeather
     */

    private fun formatResponse(weatherEntity: WeatherEntity){
        try {
            val temp = "${weatherEntity.main.temp.toInt()}??"
            val cityName = weatherEntity.name
            val country = weatherEntity.sys.country
            val address = "$cityName, $country"
            val tempMin = "M??n: ${weatherEntity.main.temp_min.toInt()}??"
            val tempMax = "Max: ${weatherEntity.main.temp_max.toInt()}??"
            // Capitalizar la primera letra de la descripci??n
            var status = ""
            val weatherDescription = weatherEntity.weather[0].description
            if (weatherDescription.isNotEmpty()) {
                status = (weatherDescription[0].uppercaseChar() + weatherDescription.substring(1))
            }
            val dt = weatherEntity.dt

            val hora = SimpleDateFormat(
                "HH",
                Locale.ENGLISH
            ).format(Date(dt * 1000))

            //darle formato a  la fecha de consulta con hora y minutos
            val updatedAt =  getString(R.string.updatedAt) + SimpleDateFormat(
                "hh:mm a",
                Locale.ENGLISH
            ).format(Date(dt * 1000))

            Log.d(TAG, "HORA: $hora ")

            val horaInt = hora.toInt()


            /*bg = when(hora){
                in 0..18 -> 1
                in 19..23 -> 0
                else -> 2
            }*/

            var bg = getDrawable(R.drawable.minibg)

            if(horaInt<18){
                bg = getDrawable(R.drawable.mydaygradient_bg)
            }else{
                bg = getDrawable(R.drawable.mynightgradient_bg)
            }

            Log.d(TAG, "HORA: $horaInt ")



            val sunrise = weatherEntity.sys.sunrise
            val sunriseFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunrise*1000))
            val sunset = weatherEntity.sys.sunset
            val sunsetFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunset*1000))
            val wind = "${weatherEntity.wind.speed} km/h"
            val pressure = "${weatherEntity.main.pressure} mb"
            val humidity = "${weatherEntity.main.humidity}%"
            val feelsLike = getString(R.string.sensation) + weatherEntity.main.feels_like.toInt() + "??"
            val icon = weatherEntity.weather[0].icon
            val iconUrl = "https://openweathermap.org/img/w/$icon.png"


            binding.apply {
                mainConstraintLayout.background = bg
                iconImageView.load(iconUrl)
                adressTextView.text = address
                dateTextView.text = updatedAt
                temperatureTextView.text = temp
                statusTextView.text = status
                tempMinTextView.text = tempMin
                tempMaxTextView.text = tempMax
                sunriseTextView.text = sunriseFormat
                sunsetTextView.text = sunsetFormat
                windTextView.text = wind
                pressureTextView.text = pressure
                humidityTextView.text = humidity
                detailsContainer.isVisible = true
                feelsLikeTextView.text = feelsLike
            }

            showIndicator(false)
        } catch (exception: Exception) {
            showError(getString(R.string.error_ocurred))
            Log.e("Error format", "Ha ocurrido un error")
            showIndicator(false)
        }
    }

    /**
     * Funci??n para generar un cuadro de di??logo
     */
    private fun showCreateUserDialog(temperature: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("La temperatura actual es: \"$temperature\".")
            .setMessage("??Quieres actualizar los datos?")
            .setPositiveButton("Actualizar") { _, _ ->
                onConfirmLocationChange()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                showSnackbar(R.string.canceled_action)
            }
            .show()
    }

    private fun onConfirmLocationChange() {
        //TODO create function
        /* falta implementar */
    }

    /**
     * Complementarios para errores y visibilidad de las views
     */

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showIndicator(visible: Boolean) {
        binding.progressBarIndicator.isVisible = visible
    }

    /**
     * Provee un forma sencilla de obtener la ubicaci??n del dispositivo, muy adecuada para
     * applicaciones que no requieren de una alta preci??n de la ubicaci??n y que no requieran
     * actualizaciones. Obtiene lo mejor y y m??s reciente ubicaci??n disponible, que en algunos
     * casos puede llegar a ser nula, cuando la ubicaci??n no este disponible.
     *
     * La herramienta Lint checa el c??digo del proyecto por bugs y propone optimizaciones.
     *
     * SuppressLint indica que Lint debe ignorar las alertas pera el elemento anotado.
     *
     * Nota: Este m??todo debe llamarse despu??s que los permispos de ubicaci??n fueron otorgados.
     *
     * @param onLocation es un callback que recibir?? la location obtenida por
     * fusedLocationClient.lastLocation
     */

    @SuppressLint("MissingPermission")
    private fun getLastLocation(onLocation: (location: Location) -> Unit) {
        Log.d(TAG, "Aqu?? estoy: $latitude Long: $longitude")
        fusedLocationClient.lastLocation
            .addOnCompleteListener { taskLocation ->
                if (taskLocation.isSuccessful && taskLocation.result != null) {

                    val location = taskLocation.result

                    latitude = location?.latitude.toString()
                    longitude = location?.longitude.toString()
                    Log.d(TAG, "GetLasLoc Lat: $latitude Long: $longitude")

                    onLocation(taskLocation.result)
                } else {
                    Log.w(TAG, "getLastLocation:exception", taskLocation.exception)
                    showSnackbar(R.string.no_location_detected)
                }
            }
    }

    /**
     * Devuelve el estado de los permisos que se necesitan
     */

    private fun checkPermissions() =
        ActivityCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PermissionChecker.PERMISSION_GRANTED

    private fun startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
            REQUEST_PERMISSIONS_REQUEST_CODE)
    }

    private fun requestPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )) {
            // Proporciona una explicaci??n adicional al usuario (rationale). Esto ocurre si el usuario
            // niega el permiso previamente pero no marca la casilla "No volver a preguntar".
            Log.i(TAG, "Muestra explicaci??n rationale para proveer una contexto adicional de porque se requiere el permiso")
            showSnackbar(R.string.permission_rationale, android.R.string.ok) {
                // Solicitar permiso
                startLocationPermissionRequest()
            }

        } else {
            // Solicitar permiso. Es posible que esto pueda ser contestado de forma autom??tica
            // si la configuraci??n del dispositivo define el permiso a un estado predefinido o
            // si el usuario anteriormente activo "No presenter de nuevo".
            Log.i(TAG, "Solicitando permiso")
            startLocationPermissionRequest()
        }
    }

    /**
     * Callback recibido cuando se ha completado una solicitud de permiso.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            when {
                // Si el flujo es interrumpido, la solicitud de permiso es cancelada y se
                // reciben arrays vacios.
                grantResults.isEmpty() -> Log.i(TAG, "La interacci??n del usuario fue cancelada.")

                // Permiso otorgado.
                // Podemos pasar la referencia a una funcion si cumple con el mismo prototipo
                (grantResults[0] == PackageManager.PERMISSION_GRANTED) -> getLastLocation(this::setupViewData)


                else -> {
                    showSnackbar(
                        R.string.permission_denied_explanation, R.string.settings
                    ) {
                        // Construye el intent que muestra la ventana de configuraci??n del app.
                        val intent = Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", APPLICATION_ID, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                    }
                }
            }
        }
    }

    /**
     * Muestra el [Snackbar].
     *
     * @param snackStrId El id del recurso para el el texto en el Snackbar.
     * @param actionStrId El texto para el elemento de acci??n.
     * @param listener El listener asociado con la acci??n del Snackbar.
     */
    private fun showSnackbar(
        snackStrId: Int,
        actionStrId: Int = 0,
        listener: View.OnClickListener? = null
    ) {
        val snackbar = Snackbar.make(findViewById(android.R.id.content), getString(snackStrId),
            BaseTransientBottomBar.LENGTH_INDEFINITE
        )
        if (actionStrId != 0 && listener != null) {
            snackbar.setAction(getString(actionStrId), listener)
        }
        snackbar.show()
    }
}