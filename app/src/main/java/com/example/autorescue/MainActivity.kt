package com.example.autorescue

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.core.content.ContextCompat

import kotlinx.coroutines.delay

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : ComponentActivity() {

    private lateinit var sensorManager: PhoneSensorManager
    private lateinit var locationManager: RescueLocationManager
    private lateinit var repository: IncidentRepository

    private var latestSnapshot by mutableStateOf(
        SensorSnapshot()
    )

    private var history by mutableStateOf(
        emptyList<IncidentHistoryEntry>()
    )

    private var gpsReady by mutableStateOf(false)

    private var latitude by mutableStateOf<Double?>(null)
    private var longitude by mutableStateOf<Double?>(null)
    private var speedMps by mutableStateOf(0f)

    private var monitoring by mutableStateOf(false)

    private var emergencyActive by mutableStateOf(false)
    private var countdown by mutableStateOf(10)

    private var lastAssessment by mutableStateOf<IncidentAssessment?>(null)

    private var emergencyPhoneNumber by mutableStateOf("")

    private var locationPermissionGranted by mutableStateOf(false)
    private var smsPermissionGranted by mutableStateOf(false)
    private var notificationPermissionGranted by mutableStateOf(false)

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            refreshPermissionState()
            refreshLocation()
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            refreshPermissionState()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager =
            PhoneSensorManager(this) { snapshot ->

                latestSnapshot =
                    snapshot.copy(
                        gpsAvailable = gpsReady,
                        latitude = latitude,
                        longitude = longitude,
                        speedMps = speedMps
                    )
            }

        locationManager =
            RescueLocationManager(this)

        repository =
            IncidentRepository(this)

        history =
            repository.load()

        refreshPermissionState()

        setContent {

            MaterialTheme {

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {

                    AutoRescueDashboard()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        refreshPermissionState()
        refreshLocation()

        sensorManager.start()
    }

    override fun onPause() {
        sensorManager.stop()

        super.onPause()
    }

    private fun refreshPermissionState() {

        locationPermissionGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

        smsPermissionGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED

        notificationPermissionGranted =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

            } else {
                true
            }
    }

    private fun requestPermissions() {

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.SEND_SMS
            )
        )
    }

    private fun requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )

            } else {

                Toast.makeText(
                    this,
                    "Notifications are already enabled.",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } else {

            Toast.makeText(
                this,
                "Notification permission is not required on Android 12 or below.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun refreshLocation() {

        if (!locationPermissionGranted) {
            gpsReady = false
            return
        }

        locationManager.getCurrentLocation { lat, lon, speed ->

            latitude = lat
            longitude = lon
            speedMps = speed

            gpsReady =
                lat != null && lon != null

            latestSnapshot =
                latestSnapshot.copy(
                    gpsAvailable = gpsReady,
                    latitude = lat,
                    longitude = lon,
                    speedMps = speed
                )
        }
    }

    private fun startMonitoring() {

        if (!monitoring) {

            val intent =
                Intent(
                    this,
                    CrashMonitoringService::class.java
                )

            ContextCompat.startForegroundService(
                this,
                intent
            )

            monitoring = true

            Toast.makeText(
                this,
                "AutoRescue monitoring started.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun stopMonitoring() {

        val intent =
            Intent(
                this,
                CrashMonitoringService::class.java
            )

        stopService(intent)

        monitoring = false

        Toast.makeText(
            this,
            "AutoRescue monitoring stopped.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun simulateCrash() {

        refreshLocation()

        val simulatedSnapshot =
            latestSnapshot.copy(
                accelerationMagnitude = 28f,
                rotationMagnitude = 5.5f,
                speedMps = 0f,
                gpsAvailable = gpsReady,
                latitude = latitude,
                longitude = longitude
            )

        latestSnapshot =
            simulatedSnapshot

        startEmergencyWorkflow()
    }

    private fun startEmergencyWorkflow() {

        countdown = 10
        emergencyActive = true
        lastAssessment = null
    }

    private fun driverIsSafe() {

        completeEmergency(
            response = "DRIVER SAFE"
        )
    }

    private fun continueEmergency() {

        completeEmergency(
            response = "EMERGENCY CONTINUED"
        )
    }

    private fun completeEmergency(
        response: String
    ) {

        emergencyActive = false

        val assessment =
            RiskEngine.assess(
                snapshot = latestSnapshot,
                driverResponse = response
            )

        lastAssessment =
            assessment

        val entry =
            IncidentHistoryEntry(
                timestamp = System.currentTimeMillis(),
                priority = assessment.priority,
                score = assessment.score,
                impactMagnitude = assessment.impactMagnitude,
                rotationMagnitude = assessment.rotationMagnitude,
                driverResponse = assessment.driverResponse,
                gpsAvailable = assessment.gpsAvailable,
                latitude = latestSnapshot.latitude,
                longitude = latestSnapshot.longitude
            )

        repository.save(entry)

        history =
            repository.load()

        Toast.makeText(
            this,
            "Incident assessment completed.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun sendEmergencySms() {

        val assessment =
            lastAssessment

        if (assessment == null) {

            Toast.makeText(
                this,
                "Complete an emergency assessment first.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (!smsPermissionGranted) {

            Toast.makeText(
                this,
                "Please grant SMS permission first.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (emergencyPhoneNumber.isBlank()) {

            Toast.makeText(
                this,
                "Enter an emergency contact number.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val result =
            EmergencySender.sendSms(
                context = this,
                phoneNumber = emergencyPhoneNumber,
                assessment = assessment,
                latitude = latitude,
                longitude = longitude
            )

        result
            .onSuccess {

                Toast.makeText(
                    this,
                    "Emergency SMS sent.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .onFailure {

                Toast.makeText(
                    this,
                    "SMS failed: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun dial112() {

        val intent =
            Intent(
                Intent.ACTION_DIAL,
                Uri.parse("tel:112")
            )

        if (
            intent.resolveActivity(packageManager) != null
        ) {

            startActivity(intent)

        } else {

            Toast.makeText(
                this,
                "Phone dialer is not available.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun clearHistory() {

        repository.clear()

        history =
            emptyList()

        Toast.makeText(
            this,
            "Incident history cleared.",
            Toast.LENGTH_SHORT
        ).show()
    }

    @androidx.compose.runtime.Composable
    private fun AutoRescueDashboard() {

        val liveImpact =
            latestSnapshot.accelerationMagnitude

        val liveRotation =
            latestSnapshot.rotationMagnitude

        val liveRiskScore =
            calculateLiveRisk(
                liveImpact,
                liveRotation
            )

        LaunchedEffect(emergencyActive) {

            if (emergencyActive) {

                while (countdown > 0 && emergencyActive) {

                    delay(1000)

                    if (!emergencyActive) {
                        break
                    }

                    if (countdown <= 1) {

                        countdown = 0

                        completeEmergency(
                            response = "NO RESPONSE"
                        )

                        break

                    } else {

                        countdown -= 1
                    }
                }
            }
        }

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            item {

                HeaderCard()
            }

            item {

                SensorStatusCard(
                    impact = liveImpact,
                    rotation = liveRotation,
                    speed = speedMps,
                    gpsAvailable = gpsReady
                )
            }

            item {

                RiskScoreCard(
                    score = liveRiskScore,
                    priority =
                        when {
                            liveRiskScore >= 70 -> "HIGH"
                            liveRiskScore >= 45 -> "MEDIUM"
                            else -> "LOW"
                        }
                )
            }

            item {

                MonitoringCard(
                    monitoring = monitoring,
                    onStart = {
                        startMonitoring()
                    },
                    onStop = {
                        stopMonitoring()
                    }
                )
            }

            item {

                CrashSimulationCard(
                    onSimulateCrash = {
                        simulateCrash()
                    }
                )
            }

            if (emergencyActive) {

                item {

                    EmergencyCountdownCard(
                        countdown = countdown,
                        onSafe = {
                            driverIsSafe()
                        },
                        onContinue = {
                            continueEmergency()
                        }
                    )
                }
            }

            if (lastAssessment != null) {

                item {

                    AssessmentCard(
                        assessment = lastAssessment!!
                    )
                }

                item {

                    EmergencyActionsCard(
                        phoneNumber = emergencyPhoneNumber,
                        onPhoneNumberChange = {
                            emergencyPhoneNumber = it
                        },
                        smsPermissionGranted =
                            smsPermissionGranted,
                        onSendSms = {
                            sendEmergencySms()
                        },
                        onDial112 = {
                            dial112()
                        }
                    )
                }
            }

            item {

                PermissionCard(
                    locationPermissionGranted =
                        locationPermissionGranted,
                    smsPermissionGranted =
                        smsPermissionGranted,
                    notificationPermissionGranted =
                        notificationPermissionGranted,
                    onRequestPermissions = {
                        requestPermissions()
                    },
                    onRequestNotificationPermission = {
                        requestNotificationPermission()
                    }
                )
            }

            item {

                LocationCard(
                    gpsAvailable = gpsReady,
                    latitude = latitude,
                    longitude = longitude,
                    speed = speedMps,
                    onRefresh = {
                        refreshLocation()
                    }
                )
            }

            item {

                HistoryCard(
                    history = history,
                    onClear = {
                        clearHistory()
                    }
                )
            }

            item {

                FooterCard()
            }
        }
    }

    private fun calculateLiveRisk(
        impact: Float,
        rotation: Float
    ): Int {

        val impactScore =
            when {
                impact >= 30f -> 50
                impact >= 22f -> 40
                impact >= 15f -> 30
                impact >= 10f -> 15
                else -> 0
            }

        val rotationScore =
            when {
                rotation >= 7f -> 20
                rotation >= 5f -> 15
                rotation >= 3.5f -> 10
                else -> 0
            }

        return (
                impactScore + rotationScore
                ).coerceIn(0, 100)
    }

    @androidx.compose.runtime.Composable
    private fun HeaderCard() {

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color(0xFF0D47A1)
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(20.dp)
            ) {

                Text(
                    text = "AutoRescue",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text =
                        "Phone-first crash detection and emergency assistance",
                    fontSize = 14.sp,
                    color = Color.White
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    StatusDot(
                        active = true
                    )

                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )

                    Text(
                        text = "SYSTEM READY",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun SensorStatusCard(
        impact: Float,
        rotation: Float,
        speed: Float,
        gpsAvailable: Boolean
    ) {

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                    Modifier.padding(18.dp)
            ) {

                Text(
                    text = "Live Sensor Status",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                SensorRow(
                    name = "Accelerometer",
                    value =
                        String.format(
                            Locale.US,
                            "%.2f m/s²",
                            impact
                        ),
                    active =
                        sensorManager.hasAccelerometer()
                )

                SensorRow(
                    name = "Gyroscope",
                    value =
                        String.format(
                            Locale.US,
                            "%.2f rad/s",
                            rotation
                        ),
                    active =
                        sensorManager.hasGyroscope()
                )

                SensorRow(
                    name = "GPS",
                    value =
                        if (gpsAvailable) {
                            "LOCATION READY"
                        } else {
                            "WAITING"
                        },
                    active = gpsAvailable
                )

                SensorRow(
                    name = "Vehicle Speed",
                    value =
                        String.format(
                            Locale.US,
                            "%.1f m/s",
                            speed
                        ),
                    active = gpsAvailable
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun SensorRow(
        name: String,
        value: String,
        active: Boolean
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                StatusDot(active)

                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )

                Text(
                    text = name,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = value,
                fontWeight = FontWeight.Bold
            )
        }
    }

    @androidx.compose.runtime.Composable
    private fun StatusDot(
        active: Boolean
    ) {

        Box(
            modifier =
                Modifier
                    .size(10.dp)
                    .background(
                        color =
                            if (active) {
                                Color(0xFF2E7D32)
                            } else {
                                Color(0xFF9E9E9E)
                            },
                        shape =
                            RoundedCornerShape(50)
                    )
        )
    }

    @androidx.compose.runtime.Composable
    private fun RiskScoreCard(
        score: Int,
        priority: String
    ) {

        val riskColor =
            when (priority) {
                "HIGH" -> Color(0xFFD32F2F)
                "MEDIUM" -> Color(0xFFF57C00)
                else -> Color(0xFF388E3C)
            }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                    Modifier.padding(18.dp)
            ) {

                Text(
                    text = "Live Risk Assessment",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                Text(
                    text = "$score / 100",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = riskColor
                )

                Text(
                    text = "CURRENT LEVEL: $priority",
                    fontWeight = FontWeight.Bold,
                    color = riskColor
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text =
                        "Risk score is calculated locally from phone motion data."
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun MonitoringCard(
        monitoring: Boolean,
        onStart: () -> Unit,
        onStop: () -> Unit
    ) {

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                    Modifier.padding(18.dp)
            ) {

                Text(
                    text = "Crash Monitoring",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                Text(
                    text =
                        if (monitoring) {
                            "Background monitoring service is running."
                        } else {
                            "Background monitoring service is stopped."
                        }
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    Button(
                        onClick = onStart,
                        modifier =
                            Modifier.weight(1f),
                        enabled = !monitoring
                    ) {
                        Text("START")
                    }

                    OutlinedButton(
                        onClick = onStop,
                        modifier =
                            Modifier.weight(1f),
                        enabled = monitoring
                    ) {
                        Text("STOP")
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun CrashSimulationCard(
        onSimulateCrash: () -> Unit
    ) {

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                    Modifier.padding(18.dp)
            ) {

                Text(
                    text = "Crash Detection Demo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                Text(
                    text =
                        "Simulate a high-impact event to demonstrate the emergency workflow."
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                Button(
                    onClick = onSimulateCrash,
                    modifier =
                        Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color(0xFFD32F2F)
                        )
                ) {
                    Text("SIMULATE CRASH")
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun EmergencyCountdownCard(
        countdown: Int,
        onSafe: () -> Unit,
        onContinue: () -> Unit
    ) {

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color(0xFFFFEBEE)
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(20.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    text = "⚠ CRASH SUSPECTED",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828)
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text = "Emergency assessment starting",
                    fontWeight = FontWeight.Medium
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                Text(
                    text = "$countdown",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828)
                )

                Text(
                    text =
                        "seconds remaining"
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                Button(
                    onClick = onSafe,
                    modifier =
                        Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color(0xFF2E7D32)
                        )
                ) {
                    Text("I'M SAFE")
                }

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                OutlinedButton(
                    onClick = onContinue,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text("CONTINUE EMERGENCY")
                }

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text =
                        "If there is no response, AutoRescue records NO RESPONSE."
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun AssessmentCard(
        assessment: IncidentAssessment
    ) {

        val priorityColor =
            when (assessment.priority) {
                "HIGH" -> Color(0xFFD32F2F)
                "MEDIUM" -> Color(0xFFF57C00)
                else -> Color(0xFF388E3C)
            }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                    Modifier.padding(18.dp)
            ) {

                Text(
                    text = "Incident Assessment",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                Text(
                    text =
                        assessment.priority,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = priorityColor
                )

                Text(
                    text =
                        "Risk score: ${assessment.score}/100"
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                AssessmentRow(
                    "Impact score",
                    "${assessment.impactScore}"
                )

                AssessmentRow(
                    "Rotation score",
                    "${assessment.rotationScore}"
                )

                AssessmentRow(
                    "Stop score",
                    "${assessment.stopScore}"
                )

                AssessmentRow(
                    "GPS score",
                    "${assessment.gpsScore}"
                )

                AssessmentRow(
                    "Response score",
                    "${assessment.responseScore}"
                )

                AssessmentRow(
                    "Driver response",
                    assessment.driverResponse
                )

                AssessmentRow(
                    "GPS available",
                    if (assessment.gpsAvailable) {
                        "YES"
                    } else {
                        "NO"
                    }
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun AssessmentRow(
        label: String,
        value: String
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(label)

            Text(
                text = value,
                fontWeight = FontWeight.Bold
            )
        }
    }

    @androidx.compose.runtime.Composable
    private fun EmergencyActionsCard(
        phoneNumber: String,
        onPhoneNumberChange: (String) -> Unit,
        smsPermissionGranted: Boolean,
        onSendSms: () -> Unit,
        onDial112: () -> Unit
    ) {

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                    Modifier.padding(18.dp)
            ) {

                Text(
                    text = "Emergency Actions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = onPhoneNumberChange,
                    modifier =
                        Modifier.fillMaxWidth(),
                    label = {
                        Text("Emergency contact number")
                    },
                    singleLine = true
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Button(
                    onClick = onSendSms,
                    modifier =
                        Modifier.fillMaxWidth(),
                    enabled = smsPermissionGranted
                ) {
                    Text("SEND SMS ALERT")
                }

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                OutlinedButton(
                    onClick = onDial112,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text("DIAL 112")
                }

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text =
                        "112 opens the device dialer. The app does not place the call automatically."
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun PermissionCard(
        locationPermissionGranted: Boolean,
        smsPermissionGranted: Boolean,
        notificationPermissionGranted: Boolean,
        onRequestPermissions: () -> Unit,
        onRequestNotificationPermission: () -> Unit
    ) {

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                    Modifier.padding(18.dp)
            ) {

                Text(
                    text = "Device Permissions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                Text(
                    text =
                        "AutoRescue uses location for emergency coordinates, SMS for emergency alerts, and notifications for monitoring status."
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                PermissionStatusRow(
                    name = "Location",
                    granted = locationPermissionGranted
                )

                PermissionStatusRow(
                    name = "SMS",
                    granted = smsPermissionGranted
                )

                PermissionStatusRow(
                    name = "Notifications",
                    granted = notificationPermissionGranted
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                Button(
                    onClick = onRequestPermissions,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text("GRANT LOCATION & SMS")
                }

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                OutlinedButton(
                    onClick =
                        onRequestNotificationPermission,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text("ENABLE NOTIFICATIONS")
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun PermissionStatusRow(
        name: String,
        granted: Boolean
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(name)

            Text(
                text =
                    if (granted) {
                        "GRANTED"
                    } else {
                        "NOT GRANTED"
                    },
                fontWeight = FontWeight.Bold,
                color =
                    if (granted) {
                        Color(0xFF2E7D32)
                    } else {
                        Color(0xFFC62828)
                    }
            )
        }
    }

    @androidx.compose.runtime.Composable
    private fun LocationCard(
        gpsAvailable: Boolean,
        latitude: Double?,
        longitude: Double?,
        speed: Float,
        onRefresh: () -> Unit
    ) {

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                    Modifier.padding(18.dp)
            ) {

                Text(
                    text = "Emergency Location",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                if (gpsAvailable) {

                    Text(
                        text =
                            "Latitude: ${latitude ?: "N/A"}"
                    )

                    Text(
                        text =
                            "Longitude: ${longitude ?: "N/A"}"
                    )

                    Text(
                        text =
                            String.format(
                                Locale.US,
                                "Speed: %.1f m/s",
                                speed
                            )
                    )

                } else {

                    Text(
                        text =
                            "GPS location is currently unavailable."
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                OutlinedButton(
                    onClick = onRefresh,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text("REFRESH LOCATION")
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun HistoryCard(
        history: List<IncidentHistoryEntry>,
        onClear: () -> Unit
    ) {

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                    Modifier.padding(18.dp)
            ) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text = "Incident History",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(
                        onClick = onClear,
                        enabled = history.isNotEmpty()
                    ) {
                        Text("CLEAR")
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                if (history.isEmpty()) {

                    Text(
                        text =
                            "No incidents recorded yet."
                    )

                } else {

                    history.take(10).forEach { item ->

                        HistoryEntryCard(
                            entry = item
                        )

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun HistoryEntryCard(
        entry: IncidentHistoryEntry
    ) {

        val date =
            SimpleDateFormat(
                "dd MMM yyyy, HH:mm",
                Locale.getDefault()
            ).format(
                Date(entry.timestamp)
            )

        Card(
            modifier =
                Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color(0xFFF5F5F5)
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(12.dp)
            ) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Text(
                        text = entry.priority,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${entry.score}/100",
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = date,
                    fontSize = 12.sp
                )

                Text(
                    text =
                        "Response: ${entry.driverResponse}"
                )

                Text(
                    text =
                        String.format(
                            Locale.US,
                            "Impact: %.2f | Rotation: %.2f",
                            entry.impactMagnitude,
                            entry.rotationMagnitude
                        )
                )

                Text(
                    text =
                        if (entry.gpsAvailable) {
                            "GPS: Available"
                        } else {
                            "GPS: Unavailable"
                        }
                )

                if (
                    entry.latitude != null &&
                    entry.longitude != null
                ) {

                    Text(
                        text =
                            "Location: ${entry.latitude}, ${entry.longitude}",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun FooterCard() {

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                    Modifier.padding(18.dp)
            ) {

                Text(
                    text =
                        "AutoRescue Hackathon Prototype",
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text =
                        "Phone-first crash detection using accelerometer, gyroscope and GPS with local deterministic risk assessment."
                )

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                Text(
                    text =
                        "Prototype thresholds are demonstration values and are not a certified crash-detection system.",
                    fontSize = 12.sp
                )
            }
        }
    }
}