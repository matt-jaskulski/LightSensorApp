package com.example.lightsensorapp

import android.app.AlertDialog
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.CheckBox
import androidx.activity.ComponentActivity
import kotlin.math.pow
import com.example.lightsensorapp.databinding.ActivityMainBinding
import java.util.regex.Pattern

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null

    private var lux = 0f

    private lateinit var vb: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vb = ActivityMainBinding.inflate(layoutInflater)
        setContentView(vb.root)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (lightSensor == null) {
            showAlertDialog()
        }

        vb.etIso.addTextChangedListener(MyTextWatcher(vb.etIso))
        vb.etTime.addTextChangedListener(MyTextWatcher(vb.etTime))
        vb.etAperture.addTextChangedListener(MyTextWatcher(vb.etAperture))
        vb.etEc.addTextChangedListener(MyTextWatcher(vb.etEc))

        vb.cbIso.setOnClickListener {
            toggleControls(vb.cbIso)
        }

        vb.cbTime.setOnClickListener {
            toggleControls(vb.cbTime)
        }

        vb.cbAperture.setOnClickListener {
            toggleControls(vb.cbAperture)
        }
    }

    private fun toggleControls(checkbox: CheckBox) {

        vb.etIso.isEnabled = true
        vb.etTime.isEnabled = true
        vb.etAperture.isEnabled = true

        when(checkbox) {
            vb.cbIso -> {
                vb.etIso.isEnabled = false
                vb.cbTime.isChecked = false
                vb.cbAperture.isChecked = false
            }
            vb.cbTime -> {
                vb.etTime.isEnabled = false
                vb.cbIso.isChecked = false
                vb.cbAperture.isChecked = false
            }
            vb.cbAperture -> {
                vb.etAperture.isEnabled = false
                vb.cbIso.isChecked = false
                vb.cbTime.isChecked = false
            }
        }

    }

    // Function to show AlertDialog
    private fun showAlertDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.dialog_title_no_light_meter))
        builder.setMessage(getString(R.string.dialog_message_no_light_meter))

        builder.setPositiveButton(getString(R.string.ok)) { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
    }

    private fun updateExposure() {

        var iso = vb.etIso.text.toString().toFloatOrNull() ?: 100f
        var aperture = vb.etAperture.text.toString().toFloatOrNull() ?: 2.8f
        var time = 1f / (vb.etTime.text.toString().toFloatOrNull() ?: 0.5f) // s
        val ec = vb.etEc.text.toString().toFloatOrNull() ?: 0.0f
        val K = 273.8f

        //val lux = K * aperture.pow(2.0f) / iso / time

        if(vb.cbIso.isChecked) {
            iso = K * aperture.pow(2.0f) / lux / time * 2f.pow(ec)
            vb.etIso.setText(getString(R.string.format_iso, iso))
        } else if(vb.cbTime.isChecked) {
            time = K * aperture.pow(2.0f) / lux / iso * 2f.pow(ec)
            time = 1f/time
            vb.etTime.setText(getString(R.string.format_time, time))
        } else if(vb.cbAperture.isChecked) {
            aperture = (lux / K * iso * time).pow(1/2.0f) / 2f.pow(ec)
            vb.etAperture.setText(getString(R.string.format_aperture, aperture))
        }

    }

    override fun onResume() {
        super.onResume()

        // Register the listener for the light sensor
        lightSensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    override fun onPause() {
        super.onPause()

        // Unregister the sensor listener when the activity is paused
        sensorManager.unregisterListener(this)
    }

    // SensorEventListener methods
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_LIGHT) {
                lux = it.values[0]

                // Update the UI with the light sensor data
                vb.tvLightReading.text = getString(R.string.format_light_reading, lux.toInt())

                updateExposure()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Implement if accuracy changes are required
    }

    inner class MyTextWatcher(val source: View) : TextWatcher {

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        override fun afterTextChanged(p0: Editable?) {}
        override fun onTextChanged(charSequence: CharSequence?, p1: Int, p2: Int, p3: Int) {
            when(source) {
                vb.etIso -> {
                    if(numberDecimal(charSequence.toString())) {
                        vb.tilIso.error = null
                    } else {
                        vb.tilIso.error = getString(R.string.incorrect_numerical_value)
                    }
                }
                vb.etTime -> {
                    if(numberDecimal(charSequence.toString())) {
                        vb.tilTime.error = null
                    } else {
                        vb.tilTime.error = getString(R.string.incorrect_numerical_value)
                    }
                }
                vb.etAperture -> {
                    if(numberDecimal(charSequence.toString())) {
                        vb.tilAperture.error = null
                    } else {
                        vb.tilAperture.error = getString(R.string.incorrect_numerical_value)
                    }
                }
                vb.etEc -> {
                    if(numberDecimal(charSequence.toString())) {
                        vb.tilEc.error = null
                    } else {
                        vb.tilEc.error = getString(R.string.incorrect_numerical_value)
                    }
                }

            }
        }

        private fun numberDecimal(value: String?): Boolean {
            return if (value == null) false else Pattern.compile("[+-]?([0-9]*[.,])?[0-9]+").matcher(value).matches()
        }

    }
}