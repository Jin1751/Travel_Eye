package com.dongjin.traveleye

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dongjin.traveleye.ui.theme.TravelEyeTheme
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
//import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class ExplainLandMark : ComponentActivity() {
    private lateinit var landmarkIntent : Intent
    private var landmarkName = ""
    private var description = mutableStateOf("")
    private var showProgress = mutableStateOf(true)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        landmarkIntent = intent
        landmarkName = landmarkIntent.getStringExtra("LandmarkName")!!

        val aiScope = CoroutineScope(Dispatchers.Main)
        lateinit var response : GenerateContentResponse
        aiScope.launch {
            val task = async (Dispatchers.Main) {
                response =  askGemini(landmarkName)
            }
            task.await()
            description.value = response.text!!
            if (task.isCompleted) { showProgress.value = false}
            Log.d("GEMINI RESPONSE", response.text.toString())
        }

        setContent {
            TravelEyeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LandmarkDescription(landmarkName, description)
                    circularProgress(showProgress = showProgress)
                }
            }
        }
    }

    private suspend fun askGemini(landmarkName: String): GenerateContentResponse {
        val harassmentSafety = SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH)
        val hateSpeechSafety =
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE)
        //val config = generationConfig { }
        val model = GenerativeModel(
            modelName = "gemini-pro",
            apiKey = BuildConfig.GEMINI_API_KEY,
            safetySettings = listOf(harassmentSafety, hateSpeechSafety)
        )
        val prompt = landmarkName + "에 대해 간단히 설명해줘"
        return model.generateContent(prompt)
    }
}

@Composable
fun LandmarkDescription(landmarkName: String, description: MutableState<String>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = landmarkName,
            modifier = modifier
                .offset(x = 10.dp, y = 35.dp)
                .drawBehind {
                    val y = size.height
                    drawLine(
                        Color.Black,
                        Offset(0f, y), Offset(size.width, y), 2f
                    )
                },
            lineHeight = 32.sp,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = modifier.height(60.dp))
        Box(modifier = modifier
            .padding(5.dp)
            .border(2.dp, Color.Black, RectangleShape)
            .fillMaxWidth()
            .fillMaxHeight()
            ){
            Text(
                text = description.value,
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .padding(start = 10.dp, end = 10.dp, top= 10.dp, bottom = 20.dp),
                fontSize = 20.sp,
                lineHeight = 33.sp,
                letterSpacing = 1.sp,
                maxLines = 100
                )
        }

    }

}

@Composable
private fun circularProgress(showProgress : MutableState<Boolean>, modifier: Modifier = Modifier) {
    val loading by remember { showProgress }

    if (!loading) return

    val config = LocalConfiguration.current
    val screenH = config.screenHeightDp.dp
    val screenW = config.screenWidthDp.dp
    Column (modifier = modifier.fillMaxSize()) {
        Spacer(modifier = modifier.height((screenH / 2) - 75.dp))
        Row (modifier = modifier.fillMaxSize()) {
            Spacer(modifier = modifier.width((screenW / 2) - 33.5.dp))
            CircularProgressIndicator(
                modifier = Modifier.width(61.5.dp),
                color = Color.Cyan,
                strokeWidth = 10.7.dp,
            )
        }
    }

}// API로부터 결과값을 받아올 때까지 처리중을 표시할 원형 프로그래스 바

@Preview(showBackground = true)
@Composable
fun GreetingPreview2() {
    val ts = mutableStateOf("Hello")
    TravelEyeTheme {
        LandmarkDescription("Android",ts)
    }
}