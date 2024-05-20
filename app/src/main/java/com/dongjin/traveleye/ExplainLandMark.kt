package com.dongjin.traveleye

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
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
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dongjin.traveleye.ui.theme.TravelEyeTheme
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class ExplainLandMark : ComponentActivity() {

    private lateinit var connectionManager: ConnectivityManager//네트워크 상태를 확인하기 위한 오브젝트
    private lateinit var networkCallback : ConnectivityManager.NetworkCallback//네트워크 상태에 따른 명령을 실행할 Callback 함수

    private lateinit var errorIntent : Intent //에러를 MainActivity에 보낼 Intent

    private lateinit var userCountry : String//현재 사용자가 있는 국가
    private lateinit var userCity : String//현재 사용자가 있는 도시

    private lateinit var landmarkIntent : Intent//SuccessLandmark에서 넘어온 랜드마크 정보를 가진 Intent
    private var landmarkName = ""//검색할 랜드마크 이름
    private var translatedName = ""//번역된 랜드마크 이름

    private var description = mutableStateOf("")//Gemini의 설명을 저장할 mutableState 함수
    private var state = mutableStateOf("none")//Gemini 결과의 상태를 저장할 mutableState 함수 [PROCESSING, ERRORED, COMPLETED]

    private var showProgress = mutableStateOf(true)//Gemini 결과가 도착하기 전까지 Circular Progress bar 표시를 유무를 판별할 함수

    private lateinit var languageSetting : String//사용자 언어설정

    private val openAIDialog = mutableStateOf(false)//AI 설명 주의 다이얼로그를 띄울 유무를 판별할 mutableState 함수

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("EXPLAINLANDMARK", "ONCREATE")
        errorIntent = Intent(this, MainActivity::class.java)//에러가 생기면 MainActivity로 이동시킬 Intent 초기화

        networkCallback = object : ConnectivityManager.NetworkCallback(){
            override fun onLost(network: Network) {//네크워크 연결 문제시 현재 액티비티 종료 후 MainActivity로 복귀
                super.onLost(network)
                backToMainActivity("CONNECTION_LOST")
            }
        }

        connectionManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager//네트워크 연결 상태 체크 오브젝트 초기화
        connectionManager.registerDefaultNetworkCallback(networkCallback)
        when (connectionManager.activeNetwork){//네크워크 연결 문제시 현재 액티비티 종료 후 MainActivity로 복귀
            null -> backToMainActivity("CONNECTION_LOST")
        }

        landmarkIntent = intent//SuccessLandmark에서 넘어온 intent를 저장

        userCountry = landmarkIntent.getStringExtra("country")!!//사용자 위치의 국가를 Intent에서 가져옴
        userCity = landmarkIntent.getStringExtra("city")!!//사용자 위치의 도시를 Intent에서 가져옴
        if (userCity == ""){//싱가포르와 같이 도시 국가는 도시가 따로 없어 비어있을 경우 Gemini에게 요청을 보낼 때 국가와 도시를 같게 해서 요청함
            userCity = userCountry
        }

        landmarkName = landmarkIntent.getStringExtra("LandmarkName")!!//장소 이름을 Intent에서 가져옴
        translatedName = landmarkIntent.getStringExtra("TranslatedName")!!//MLkit으로 번역된 장소 이름을 Intent에서 가져옴
        languageSetting = landmarkIntent.getStringExtra("languageSetting")!!//언어 설정을 Intent에서 가져옴

        setContent {
            TravelEyeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LandmarkDescription(landmarkName, translatedName, description, openAIDialog)//Gemini설명과 장소 이름을 표현할 UI
                    CircularProgress(showProgress = showProgress)//Gemini Response가 오기 전까지 원형 프로그래스바 실행
                    AiDialog(openDialog = openAIDialog, languageSetting = languageSetting)//ai로 만들어진 설명이라는 주의 다이얼로그
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (state.value == "none"){//이전에 검색한 결과가 없으면 GEMINI에 설명 요청
            showProgress.value = true
            useGemini(landmarkName, languageSetting)//firebase gemini에 설명 요청
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        state.value = "none"
        description.value = ""
    }

    private fun backToMainActivity(errorMsg : String){//문제가 생겼을 때 MainActivity로 돌아가 에러 다이얼로그를 띄움
        onDestroy()
        finish()
        errorIntent.putExtra("isError", true)
        errorIntent.putExtra("errorMsg", errorMsg)
        startActivity(errorIntent)
    }

    //private suspend fun askGemini(landmarkName: String): GenerateContentResponse {//안드로이드 sdk에서 바로 gemini를 사용하는 함수, Google의 방침에 따라 사용하지 않음
    //    val model = GenerativeModel(
    //        "gemini-1.0-pro",
    //        //gemini api key here,
    //        generationConfig = generationConfig {
    //            temperature = 0.9f
    //            topK = 0
    //            topP = 1f
    //            maxOutputTokens = 2048
    //        },
    //        safetySettings = listOf(
    //            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
    //            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
    //            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.ONLY_HIGH),
    //            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH),
    //        ),
    //    )
//
    //    val prompt = "ASK TO GEMINI"
//
    //    return model.generateContent(prompt)//Gemini Response를 받아 리턴
    //}


    private fun useGemini(landmarkName: String, languageSetting: String){
        val firestore = Firebase.firestore//firebase에서 gemini extension이 참조하는 firestore
        val collectionValue = hashMapOf(//firebase의 gemini extension의 prompt에 들어갈 값들을 collection에 삽입
            "language" to languageSetting ,
            "landmark" to landmarkName,
            "city" to userCity,
            "country" to userCountry,

        )
        firestore.collection("generate").document("geminidoc").set(collectionValue).addOnSuccessListener {
            Log.d("FIREBASE COLLECTION WRITE", "SUCCESSED")//collection 수정 완료
        }.addOnFailureListener {
            Log.d("FIREBASE COLLECTION WRITE", "FAILED $it")//collection 수정 실패
            backToMainActivity("GEMINI_ERR")//error 발생으로 MainActivity로 전환
        }
        val response = firestore.collection("generate").document("geminidoc")//gemini의 설명과 상태를 가진 firestore 문서

        response.addSnapshotListener{//firestore 문서의 변화가 생기면 반응할 리스너 설정
            snapshot, e ->
            if (e != null) {//gemini exception발생으로 MainActivity로 전환
                Log.d("FIREBASE GEMINI ", "Listen failed.", e)
                showProgress.value = false
                backToMainActivity("GEMINI_ERR")
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {//정상적으로 gemini 설명을 받았을 때
                val geminiData = snapshot.get("description").toString()//문서에서 description 부분을 저장

                val status = snapshot["status"].toString().split("=")//문서의 status 부분을 '='로 나눠서 리스트로 저장 (status 중 state 값만 사용하기 위함, 특정 값만 받아오는 함수가 없음)
                state.value = status[status.size - 1]//status 중 가장 마지막에 있는 state를 저장 (PROCESSING, COMPLETED)
                var errorCheck = false
                status.forEach {
                    if (it == "ERRORED, error") {// error 체크, error는 에러 설명이 리스트 맨 뒤에 나와서 다른 상태처럼 체크가 불가능
                        errorCheck = true
                    }
                }
                if ((state.value == "COMPLETED}") or (errorCheck)){//gemini 응답 상태가 completed나 errored일때만 작동
                    description.value = geminiData//gemini에게 받은 설명 출력
                    showProgress.value = false//원형 프로그래스바 중지
                    if (errorCheck){//gemini에서 error 발생시 MainActivity로 전환
                        backToMainActivity("GEMINI_ERR")
                    }
                }
                else{//gemini가 작업 중일땐 원형 프로그래스바 표현
                    showProgress.value = true
                }
            } else {//gemini 설명이 없을 때 에러로 인한 MainActivity 복귀
                showProgress.value = false
                backToMainActivity("GEMINI_ERR")
            }
        }
    }
}

@Composable
fun LandmarkDescription(landmarkName: String, translatedName : String, description: MutableState<String>, openDialog: MutableState<Boolean>,modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lineColor = when (context.resources.configuration.uiMode) {//스마트폰 다크모드와 라이트 모드에 따라 이름 밑줄 색을 결정
        33 -> {//다크모드일때
            Color.White
        }
        else -> {//라이트 모드일때
            Color.Black
        }
    }

    Column(modifier = modifier.fillMaxSize()) {

        Text(
            text = landmarkName,
            modifier = modifier
                .fillMaxWidth(0.95f)
                .offset(x = 10.dp, y = 30.dp)
                .drawBehind {
                    val y = size.height + 10
                    drawLine(
                        color = lineColor,
                        Offset(0f, y), Offset(size.width, y), 5f,
                    )
                },
            lineHeight = 32.sp,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow =  TextOverflow.Ellipsis
        )
        Row(modifier = modifier.offset(x = 10.dp, y = 40.dp)){
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

        Spacer(modifier = modifier.height(32.dp))
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
            .border(2.dp, LocalContentColor.current, RectangleShape)
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
private fun AiDialog(openDialog : MutableState<Boolean>, languageSetting : String,modifier: Modifier = Modifier){
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
                        Text(text = "OKAY", fontSize = 25.sp)
                    }
                })
    }
}


@Composable
private fun CircularProgress(showProgress : MutableState<Boolean>, modifier: Modifier = Modifier) {
    val loading by remember { showProgress }//mutableStateOf(true)

    if (!loading) return

    Column (modifier = modifier.fillMaxSize()) {
        Spacer(modifier = modifier.fillMaxHeight(0.5f))
        Row (modifier = modifier.fillMaxSize()) {
            Spacer(modifier = modifier.fillMaxWidth(0.42f))
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
    TravelEyeTheme (darkTheme = true) {
        LandmarkDescription("Android","HELLO WORLD",ts,di)
        CircularProgress(showProgress = mutableStateOf(true))//Gemini Response가 오기 전까지 원형 프로그래스바 실행
        AiDialog(openDialog = di, "english")
    }
}