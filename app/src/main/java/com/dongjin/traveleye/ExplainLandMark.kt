package com.dongjin.traveleye

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    private var translatedName = ""
    private var description = mutableStateOf("")
    private var showProgress = mutableStateOf(true)
    private lateinit var languageSetting : String
    private val openDialog = mutableStateOf(false)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        landmarkIntent = intent
        landmarkName = landmarkIntent.getStringExtra("LandmarkName")!!
        translatedName = landmarkIntent.getStringExtra("TranslatedName")!!
        languageSetting = landmarkIntent.getStringExtra("languageSetting")!!
        val aiScope = CoroutineScope(Dispatchers.Main)//Gemini 사용을 위한 코루틴 스코프
        lateinit var response : GenerateContentResponse
        aiScope.launch {//코루틴 실행
            val task = async (Dispatchers.Main) {
                response =  askGemini(landmarkName)//Gemini에게 장소에 대한 설명 요청
            }
            task.await()//Gemini Response를 받을 때까지 wait
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
                    LandmarkDescription(landmarkName, translatedName, description, openDialog)
                    circularProgress(showProgress = showProgress)//Gemini Response가 오기 전까지 원형 프로그래스바 실행
                    aiDialog(openDialog = openDialog, languageSetting = languageSetting)
                }
            }
        }
    }

    private suspend fun askGemini(landmarkName: String): GenerateContentResponse {
        val harassmentSafety = SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH)
        val hateSpeechSafety =
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE)//Gemini Response 안전 규칙 설정
        //val config = generationConfig { }
        val model = GenerativeModel(
            modelName = "gemini-pro",
            apiKey = BuildConfig.GEMINI_API_KEY,
            safetySettings = listOf(harassmentSafety, hateSpeechSafety)
        )//Gemini 응답 모델 설정
        val prompt = "Explain about $landmarkName in $languageSetting"//Gemini에게 요청할 문장
        return model.generateContent(prompt)//Gemini Response를 받아 리턴
    }
}

@Composable
fun LandmarkDescription(landmarkName: String, translatedName : String, description: MutableState<String>, openDialog: MutableState<Boolean>,modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = landmarkName,
            modifier = modifier
                .fillMaxWidth(0.95f)
                .offset(x = 10.dp, y = 30.dp)
                .drawBehind {
                    val y = size.height
                    drawLine(
                        Color.Black,
                        Offset(0f, y), Offset(size.width, y), 2f
                    )
                },
            lineHeight = 32.sp,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow =  TextOverflow.Ellipsis
        )
        Row(modifier = modifier.offset(x = 10.dp, y = 38.dp)){
            Icon(imageVector = Icons.Default.Translate, contentDescription = "translate", tint = Color.Gray)
            Text(text = " $translatedName",
                modifier = modifier.fillMaxWidth(0.95f),
                lineHeight = 32.sp,
                fontSize = 20.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow =  TextOverflow.Ellipsis)
        }

        Spacer(modifier = modifier.height(30.dp))
        val screenW = LocalConfiguration.current.screenWidthDp
        IconButton(modifier = modifier
            .offset(x = (screenW - 55).dp)
            .size(50.dp),
            onClick = {
                openDialog.value = true
            }) {
            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AIDialog", tint = Color(38,124,240,255), modifier = modifier.size(35.dp))
        }
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
                    .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 20.dp),
                fontSize = 20.sp,
                lineHeight = 33.sp,
                letterSpacing = 1.sp
                )
        }

    }

}
@Composable
private fun aiDialog(openDialog : MutableState<Boolean>, languageSetting : String,modifier: Modifier = Modifier){
    val dialogTxt = when(languageSetting)  {
        "korean" -> "본 앱은 AI 기술을 활용하므로\n정보 오류 가능성이 있습니다.\n 장소 정보는 참고용으로만 \n사용하시길 권장합니다."

        else -> "This app utilizes AI.\nTherefore it may contain errors in landmark information.\nPlease use landmark information as reference only."
    }
    when {
        openDialog.value ->
            AlertDialog(
                modifier = modifier.fillMaxWidth(),
                icon = { androidx.compose.material3.Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "ExplainAIDialog", tint = Color.Gray, modifier = modifier.size(40.dp)) },
                text = { Text(text = dialogTxt, fontSize = 20.sp, textAlign = TextAlign.Center,lineHeight = 30.sp, letterSpacing = 1.sp)},
                onDismissRequest = { openDialog.value = false },
                confirmButton = {
                    TextButton(onClick = { openDialog.value = false }) {
                        Text(text = "OKAY", color = Color.Blue, fontSize = 25.sp)
                    }
                })
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
    val di = mutableStateOf(false)
    TravelEyeTheme {
        LandmarkDescription("Android","HELLO WORLD",ts,di)
        aiDialog(openDialog = di, "english")
    }
}