package com.example.weatherforecast.Activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.example.weatherforecast.Models.WeatherModel
import com.example.weatherforecast.R
import com.example.weatherforecast.Utilites.ApiUtilities
import com.example.weatherforecast.databinding.ActivityMainBinding

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding


    private lateinit var currentLocation: Location
    private lateinit var fusedLocationProvider: FusedLocationProviderClient
    private val LOCATION_REQUEST_CODE = 101


    private val apiKey = "99165db875f7e7cd58f62c3f2564e863"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)

        getCurrentLocation()

        binding.citySearch.setOnEditorActionListener { textView, i, keyEvent ->

            if (i == EditorInfo.IME_ACTION_SEARCH) {

                getCityWeather(binding.citySearch.text.toString())

                val view = this.currentFocus

                if (view != null) {

                    val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE)
                            as InputMethodManager

                    imm.hideSoftInputFromWindow(view.windowToken, 0)

                    binding.citySearch.clearFocus()

                }
                return@setOnEditorActionListener true

            } else {

                return@setOnEditorActionListener false
            }
        }

        binding.currentLocation.setOnClickListener {

            getCurrentLocation()


        }


    }


    private fun getCityWeather(city: String) {

        binding.progressBar.visibility = View.VISIBLE

        ApiUtilities.getApiInterface()?.getCityWeatherData(city, apiKey)
            ?.enqueue(object : Callback<WeatherModel> {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(
                    call: Call<WeatherModel>,
                    response: Response<WeatherModel>
                ) {
                    if (response.isSuccessful) {

                        binding.progressBar.visibility = View.GONE

                        response.body()?.let {
                            setData(it)
                        }

                    } else {

                        Toast.makeText(
                            this@MainActivity, "No City Found",
                            Toast.LENGTH_SHORT
                        ).show()

                        binding.progressBar.visibility = View.GONE

                    }

                }

                override fun onFailure(call: Call<WeatherModel>, t: Throwable) {

                }

            })

    }


    private fun fetchCurrentLocationWeather(latitude: String, longitude: String) {

        ApiUtilities.getApiInterface()?.getCurrentWeatherData(latitude, longitude, apiKey)
            ?.enqueue(object : Callback<WeatherModel> {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(
                    call: Call<WeatherModel>,
                    response: Response<WeatherModel>
                ) {

                    if (response.isSuccessful) {

                        binding.progressBar.visibility = View.GONE

                        response.body()?.let {
                            setData(it)
                        }

                    }


                }

                override fun onFailure(call: Call<WeatherModel>, t: Throwable) {


                }

            })


    }


    private fun getCurrentLocation() {

        if (checkPermissions()) {

            if (isLocationEnabled()) {

                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {

                    requestPermission()

                    return
                }
                fusedLocationProvider.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            currentLocation = location

                            binding.progressBar.visibility = View.VISIBLE

                            fetchCurrentLocationWeather(
                                location.latitude.toString(),
                                location.longitude.toString()

                            )


                        }
                    }

            } else {

                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

                startActivity(intent)


            }


        } else {

            requestPermission()
        }

    }

    private fun requestPermission() {

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            LOCATION_REQUEST_CODE
        )

    }

    private fun isLocationEnabled(): Boolean {

        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE)
                as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun checkPermissions(): Boolean {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST_CODE) {

            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                getCurrentLocation()

            } else {

            }

        }

    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setData(body: WeatherModel) {

        binding.apply {

            val currentDate = SimpleDateFormat("dd/MM/yyyy hh:mm").format(Date())

            dateTime.text = currentDate.toString()

            maxTemp.text = "Max " + k2c(body?.main?.tempMax!!) + "°"

            minTemp.text = "Min " + k2c(body?.main?.tempMin!!) + "°"

            temp.text = "" + k2c(body?.main?.temp!!) + "°"

            weatherTitle.text = body.weather[0].main

            pressureValue.text = body.main.pressure.toString()

            humidityValue.text = body.main.humidity.toString() + "%"

            citySearch.setText(body.name)

            feelsLike.text = "Feels Like:" + k2c(body.main.feelsLike) + "°C"

            windValue.text = body.wind.speed.toString() + "m/s"


        }

        updateUI(body.weather[0].id)


    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ts2td(ts: Long): String {

        val localTime = ts.let {

            Instant.ofEpochSecond(it)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()

        }

        return localTime.toString()


    }

    private fun k2c(t: Double): Double {

        var intTemp = t

        intTemp = intTemp.minus(273)

        return intTemp.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()
    }


    private fun updateUI(id: Int) {

        binding.apply {


            when (id) {

                //Thunderstorm
                in 200..232 -> {

                    weatherImg.setImageResource(R.drawable.ic_clear_day)


                }

                //Drizzle
                in 300..321 -> {

                    weatherImg.setImageResource(R.drawable.ic_few_clouds)

                }

                //Rain
                in 500..531 -> {

                    weatherImg.setImageResource(R.drawable.ic_rainy_weather)

                }

                //Snow
                in 600..622 -> {

                    weatherImg.setImageResource(R.drawable.ic_snow_weather)

                }

                //Atmosphere
                in 701..781 -> {

                    weatherImg.setImageResource(R.drawable.ic_broken_clouds)


                }

                //Clear
                800 -> {

                    weatherImg.setImageResource(R.drawable.ic_clear_day)



                }

                //Clouds
                in 801..804 -> {

                    weatherImg.setImageResource(R.drawable.ic_cloudy_weather)


                }

                //unknown
                else -> {

                    weatherImg.setImageResource(R.drawable.ic_unknown)




                }


            }


        }


    }



}



