package com.example.ia4_3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random
import com.example.ia4_3.ui.theme.Ia4_3Theme

data class TemperatureReading(
    val time: String,
    val value: Float
)

class TemperatureViewModel : ViewModel() {
    private val _readings = MutableStateFlow<List<TemperatureReading>>(emptyList())
    val readings: StateFlow<List<TemperatureReading>> = _readings

    private val _isRunning = MutableStateFlow(true)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val random = Random(System.currentTimeMillis())

    init {
        startSimulation()
    }

    private fun startSimulation() {
        viewModelScope.launch {
            while (true) {
                if (_isRunning.value) {
                    val newTemp = random.nextFloat() * (85f - 65f) + 65f
                    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    val newReading = TemperatureReading(timestamp, newTemp)

                    _readings.value = (listOf(newReading) + _readings.value).take(20)
                }
                delay(2000L)
            }
        }
    }

    fun toggleRunning() {
        _isRunning.value = !_isRunning.value
    }

    fun current(): Float? = _readings.value.firstOrNull()?.value
    fun average(): Float = if (_readings.value.isEmpty()) 0f else _readings.value.map { it.value }.average().toFloat()
    fun min(): Float = _readings.value.minOfOrNull { it.value } ?: 0f
    fun max(): Float = _readings.value.maxOfOrNull { it.value } ?: 0f
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel = TemperatureViewModel()
            MaterialTheme {
                TemperatureDashboard(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemperatureDashboard(viewModel: TemperatureViewModel) {
    val readings by viewModel.readings.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Temperature Dashboard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.toggleRunning() },
                containerColor = if (isRunning)
                    MaterialTheme.colorScheme.secondary
                else
                    MaterialTheme.colorScheme.primary
            ) {
                Text(if (isRunning) "PAUSE" else "RUN")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF7F7F7))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TemperatureStats(viewModel)
            TemperatureChart(readings)
            TemperatureList(readings)
        }
    }
}

@Composable
fun TemperatureStats(viewModel: TemperatureViewModel) {
    val current = viewModel.current()
    val avg = viewModel.average()
    val min = viewModel.min()
    val max = viewModel.max()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Current: ${current?.let { String.format("%.1f°F", it) } ?: "--"}", fontWeight = FontWeight.Bold)
        Text("Average: ${String.format("%.1f°F", avg)}")
        Text("Min: ${String.format("%.1f°F", min)}")
        Text("Max: ${String.format("%.1f°F", max)}")
    }
}

@Composable
fun TemperatureChart(readings: List<TemperatureReading>) {
    if (readings.isEmpty()) return

    val maxVal = readings.maxOf { it.value }
    val minVal = readings.minOf { it.value }
    val points = readings.reversed() // oldest → newest for graph order

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(Color.White)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path()
            val stepX = size.width / (points.size - 1).coerceAtLeast(1)
            points.forEachIndexed { i, temp ->
                val normalized = (temp.value - minVal) / (maxVal - minVal).coerceAtLeast(0.01f)
                val y = size.height - (normalized * size.height)
                val x = i * stepX
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = Color.Red, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
        }
    }
}

@Composable
fun TemperatureList(readings: List<TemperatureReading>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(8.dp)
    ) {
        Text("Recent Readings:", fontWeight = FontWeight.Bold)
        LazyColumn {
            items(readings) { reading ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(reading.time)
                    Text(String.format("%.1f°F", reading.value))
                }
            }
        }
    }
}
