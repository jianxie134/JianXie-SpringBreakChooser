package com.example.springbreakchooser

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.speech.RecognizerIntent
import android.media.MediaPlayer
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import java.util.Locale
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    // UI components
    private lateinit var phraseEditText: EditText
    private lateinit var languageSelector: Spinner
    private lateinit var confirmButton: Button

    // Sensors for shake detection
    private lateinit var sensorManager: SensorManager
    private var acceleration = 0f
    private var currentAcceleration = 0f
    private var lastAcceleration = 0f

    // Stores the result of speech recognition
    private val speechResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { recognizedText ->
            phraseEditText.setText(recognizedText)
        }
    }

    // Maps for storing language-based locations and greetings
    private val languageLocationsMap = mapOf(
        // Language to geographic location mappings
        "English" to listOf("geo:51.5074,-0.1278?z=10", "geo:-33.8688,151.2093?z=10"), // London, England; Sydney,Australia
        "Chiese" to listOf("geo:39.9042,116.4074?z=10", "geo:31.2304,121.4737?z=10"), // Beijing, China; Shanghai, China
        "French" to listOf("geo:48.8566,2.3522?z=10", "geo:46.2044,6.1432?z=10"), // Paris, France; Geneva, Switzerland
        "German" to listOf("geo:52.5200,13.4050?z=10", "geo:49.6116,6.1319?z=10"), // Berlin, Germany; Luxembourg, Luxembourg
        "Italian" to listOf("geo:41.9028,12.4964?z=10", "geo:45.4642,9.1900?z=10") // Rome, Italy; Milan, Italy
    )

    private val languageGreetingsMap = mapOf(
        // Language to audio greeting resource mappings
        "English" to R.raw.greeting_english,
        "Chinese" to R.raw.greeting_chinese,
        "French" to R.raw.greeting_french,
        "German" to R.raw.greeting_german,
        "Italian" to R.raw.greeting_italian
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Binding UI components
        phraseEditText = findViewById(R.id.phrase_editText)
        languageSelector = findViewById(R.id.language_spinner)
        confirmButton = findViewById(R.id.confirm_button)

        // Set up speech recognition button click listener
        confirmButton.setOnClickListener {
            initiateSpeechRecognition()
        }

        // Initialize sensor management for shake detection
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)
        acceleration = 10f // Shake threshold acceleration
        currentAcceleration = SensorManager.GRAVITY_EARTH // Initial current acceleration
        lastAcceleration = SensorManager.GRAVITY_EARTH // Initial last acceleration
    }

    // Initiates speech recognition based on selected language
    private fun initiateSpeechRecognition() {
        val selectedLanguage = languageSelector.selectedItem.toString()
        val languageTag = getLanguageTag(selectedLanguage)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt))
        }

        try {
            speechResultLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.speech_not_supported), Toast.LENGTH_SHORT).show()
        }
    }

    // Returns the language tag for speech recognition
    private fun getLanguageTag(language: String): String {
        return when (language) {
            "English" -> "en"
            "Chinese" -> "zh"
            "French" -> "fr"
            "German" -> "de"
            "Italian" -> "it"
            else -> Locale.getDefault().toLanguageTag()
        }
    }

    // Listener for shake detection
    private val sensorListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            currentAcceleration = currentAcceleration

            currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta: Float = currentAcceleration - lastAcceleration
            acceleration = acceleration * 0.9f + delta

            // When shake is detected, navigate to the location and play greeting
            if (acceleration > 12) {
                navigateToLanguageLocation()
                playLanguageGreeting()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    // Handles navigation to the selected language location
    private fun navigateToLanguageLocation() {
        val selectedLanguage = languageSelector.selectedItem.toString()
        val locations = languageLocationsMap[selectedLanguage] ?: return
        val randomLocationUri = Uri.parse(locations.random())
        startActivity(Intent(Intent.ACTION_VIEW, randomLocationUri).setPackage("com.google.android.apps.maps"))
    }

    // Plays the greeting for the selected language
    private fun playLanguageGreeting() {
        val selectedLanguage = languageSelector.selectedItem.toString()
        val greetingResId = languageGreetingsMap[selectedLanguage] ?: return
        val mediaPlayer = MediaPlayer.create(this, greetingResId)
        mediaPlayer?.start()
        mediaPlayer?.setOnCompletionListener {
            it.release()
        }
    }

    override fun onResume() {
        sensorManager?.registerListener(sensorListener, sensorManager!!.getDefaultSensor(
            Sensor .TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL
        )
        super.onResume()
    }

    override fun onPause() {
        sensorManager!!.unregisterListener(sensorListener)
        super.onPause()
    }
}