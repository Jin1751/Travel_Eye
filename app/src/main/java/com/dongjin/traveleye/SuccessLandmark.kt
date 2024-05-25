package com.dongjin.traveleye

import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import android.graphics.Color as Colorg
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import kotlinx.parcelize.Parcelize
import com.dongjin.traveleye.ui.theme.TravelEyeTheme
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PinConfig
import com.google.maps.android.compose.AdvancedMarker
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MarkerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Parcelize
data class NearLandmark(var engName: String, var translatedName : String, val score: Double, val locate : Location) : Parcelable//사용자와 가까운 검색된 랜드마크들의 정보를 저장할 Parcelable 인터페이스

class SuccessLandmark : ComponentActivity() {

    private lateinit var connectionManager: ConnectivityManager//네트워크 상태를 확인하기 위한 오브젝트
    private lateinit var networkCallback : ConnectivityManager.NetworkCallback//네트워크 상태에 따른 명령을 실행할 Callback 함수
    private lateinit var errorIntent : Intent//에러를 MainActivity에 보낼 Intent

    private lateinit var locationIntent : Intent//MainActivity에서 넘어온 사용자 위치 정보를 가진 Intent

    private lateinit var userLocation :Location//사용자 위치 Location 오브젝트
    private lateinit var userCountry : String//현재 사용자가 있는 국가
    private lateinit var userCity : String//현재 사용자가 있는 도시


    private val landmarkArray : MutableList<NearLandmark> = mutableListOf()//랜드마크들을 저장할 리스트

    private lateinit var languageSetting : String//사용자 언어설정
    private val translateLanguage = mapOf("korean" to "ko", "english" to "en")//번역기 언어설정을 위한 mapOf 오브젝트 (추후 다른 언어를 추가해 번역기 설정 변경 가능하도록 함)
    private lateinit var translateApi : TranslateApi

    private val buttonTxt = mapOf("korean" to "AI 설명 보기", "english" to "See AI Description")//ExplainLandMark로 Gemini에 설명을 요청할 버튼
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        errorIntent = Intent(this, MainActivity::class.java)//에러가 생기면 MainActivity로 이동시킬 Intent 초기화
        networkCallback = object : ConnectivityManager.NetworkCallback(){
            override fun onLost(network: Network) {//네크워크 연결 문제시 현재 액티비티 종료 후 MainActivity로 복귀
                super.onLost(network)
                backToMainActivity()
            }
        }
        connectionManager = this.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectionManager.registerDefaultNetworkCallback(networkCallback)
        when (connectionManager.activeNetwork){//네크워크 연결 문제시 현재 액티비티 종료 후 MainActivity로 복귀
            null -> backToMainActivity()
        }

        val retrofit = Retrofit.Builder().baseUrl("https://translation.googleapis.com/language/translate/").addConverterFactory(GsonConverterFactory.create()).build()
        translateApi = retrofit.create(TranslateApi::class.java)

        locationIntent = intent//MainActivity에서 넘어온 intent를 저장

        userCountry = locationIntent.getStringExtra("country")!!//사용자 위치의 국가를 Intent에서 가져옴
        userCity = locationIntent.getStringExtra("city")!!//사용자 위치의 도시를 Intent에서 가져옴

        userLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {//빌드 버전 33 이상 시 인텐트에서 사용자 위치 가져오는 작업
            locationIntent.getParcelableExtra("userLocation",Location::class.java)!!
        }else{//빌드 버전 33 미만시 인텐트에서 사용자 위치 가져오는 작업
            locationIntent.getParcelableExtra("userLocation")!!
        }

        Log.d("SUCCESS USERLOC", userLocation.latitude.toString() + ", " + userLocation.longitude)

        languageSetting = locationIntent.getStringExtra("languageSetting")!!//사용자 언어설정의 도시를 Intent에서 가져옴

       setContent {
                TravelEyeTheme {
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        LandMarkInfoMap(userCountry, userCity,landmarkArray, userLocation, languageSetting, buttonTxt)
                    }
                }

           }
       }
    override fun onStart(){
        super.onStart()
        val totalLandmark = locationIntent.getIntExtra("totalLandmark",0)//인텐트에 있는 랜드마크의 갯수

        for (i in 1..totalLandmark){//인텐트에 있는 랜드마크를 모두 리스트에 저장
            val lm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                locationIntent.getParcelableExtra("landMark$i",NearLandmark::class.java)
            } else{
                locationIntent.getParcelableExtra("landMark$i")
            }
            if (lm != null) {//랜드마크가 null이 아닐 경우만 리스트에 저장
                if ((languageSetting != "english") and (lm.translatedName == "none")){//언어 설정이 영어가 아닐경우 설정된 언어로 번역
                    translateLandmark(lm, translateLanguage[languageSetting]!!)
                }
                landmarkArray.add(lm)
            }
            else{
                Log.d("LandMark$i", "NULL")
            }
        }

    }

    override fun onDestroy() {
        Log.d("onDestroy", "onDestroy")
        super.onDestroy()

        connectionManager.unregisterNetworkCallback(networkCallback)//네트워크 상태에 따른 CallBack함수 연결 해제
    }
    private fun translateLandmark(lm : NearLandmark,target: String){
        CoroutineScope(Dispatchers.IO).launch {
            val response = translateApi.translate(
                key = BuildConfig.CLOUD_TRANSLATION,
                q = lm.engName,
                source = "en",
                target = target,
                format = "text")
            lm.translatedName = response.data.translations[0].translatedText
            Log.d("Trans Success", response.data.translations[0].translatedText)
            return@launch
        }
    }

    private fun backToMainActivity(){//문제가 생겼을 때 MainActivity로 돌아가 에러 다이얼로그를 띄움
        onDestroy()
        finish()
        errorIntent.putExtra("isError", true)
        errorIntent.putExtra("errorMsg", "CONNECTION_LOST")
        startActivity(errorIntent)
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LandMarkInfoMap(userCountry: String, userCity : String, landmarkArray: MutableList<NearLandmark>, userLocation: Location, languageSetting : String, buttonTxt : Map<String, String>, modifier: Modifier = Modifier){
    val context = LocalContext.current
    val mapCamera = CameraPosition.fromLatLngZoom(LatLng(userLocation.latitude,userLocation.longitude),15.0f)//구글 지도 카메라
    val scope = rememberCoroutineScope()//하단의 정보창을 끄고 킬 때 사용할 코루딘 스코프
        val bottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)//하단 정보창의 상태를 컨트롤 할 상태 변수
    val placeName = mutableStateOf("Please Select LandMark")//랜드마크 이름을 표시할 텍스트 뷰에 들어갈 이름 변수
    val translatedName = mutableStateOf("Please Select LandMark")

    val backgroundColor = when (context.resources.configuration.uiMode) {//스마트폰 다크모드와 라이트 모드에 따라 하단 정보창 배경색을 결정
       33 -> {//다크모드(33)일때
            Color.Black
        }
        else -> {//라이트 모드일때
            Color.White
        }
    }

    ModalBottomSheetLayout(//지도 하단에 표시될 랜드마크 정보창 & 구글 맵
        sheetState = bottomSheetState,
        sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        sheetContentColor = LocalContentColor.current, sheetBackgroundColor = backgroundColor,
        sheetElevation = 10.dp,
        sheetContent = @Composable {
            Column {
                Row{//(modifier = modifier.offset(x = (deviceInfo.screenWidthDp / 2 - 30).dp))
                    Spacer(modifier = modifier.fillMaxWidth(0.43f))
                    Icon(modifier = modifier.offset(y = (-10).dp),imageVector = ImageVector.vectorResource(R.drawable.handle), contentDescription = "handle")
                }//하단 랜드마크 정보창의 핸들 배치

                Text(modifier = modifier.offset(x=5.dp, y = (-15).dp),text = placeName.value,fontSize = 25.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)//선택된 랜드마크의 이름
                if(languageSetting != "english")
                {
                    Row{//(modifier = modifier.offset(x = 8.dp))
                        Spacer(modifier = modifier.fillMaxWidth(0.01f))
                        Icon(imageVector = Icons.Default.Translate, contentDescription = "translate", tint = Color.Gray)
                        Text(modifier = modifier.offset(x=5.dp, y = (-5).dp),text = translatedName.value, color = Color.Gray, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)//선택된 랜드마크의 이름
                    }
                }
                Spacer(modifier = modifier.fillMaxHeight(0.02f))
                Button(modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.08f), shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(38,122,240), contentColor = Color.White)
                    ,onClick = {
                        val intent = Intent(context, ExplainLandMark::class.java)//ExplainLandMark로 정보를 넘길 Intent 초기화
                        intent.putExtra("country",userCountry)//현재 사용자의 국가 영어 이름을 Intent에 저장 (Gemini에 영어로 질문을 할 것이기 때문)
                        intent.putExtra("city",userCity)//현재 사용자의 도시 영어 이름을 Intent에 저장 (Gemini에 영어로 질문을 할 것이기 때문)
                        intent.putExtra("languageSetting", languageSetting)//현재 사용자 언어설정을 Intent에 저장
                        intent.putExtra("LandmarkName", placeName.value)//설명을 요청할 랜드마크 영어 이름을 Intent에 저장 (Gemini에 영어로 질문을 할 것이기 때문)
                        intent.putExtra("TranslatedName", translatedName.value)//설명을 요청할 번역된 랜드마크 이름을 Intent에 저장 (이름 표현용)
                        startActivity(context,intent,null)})// AI 설명을 요청할 버튼 (ExplainLandMark로 이동)
                {
                    Text(buttonTxt[languageSetting]!!,fontSize = 20.sp)
                }
                Spacer(modifier = modifier.height(3.dp))
            }
        },
        content = {//랜드마크 설명 창 외에 표시 될 콘텐츠 [구글 지도]
            Box{
                GoogleMap(
                    modifier = modifier.fillMaxSize(),
                    cameraPositionState = CameraPositionState(mapCamera),
                    properties = MapProperties(isMyLocationEnabled = true),
                    googleMapOptionsFactory = { GoogleMapOptions().mapId(BuildConfig.MAP_ID) }) {//Cloud 콘솔에 있는 구글 맵 ID
                    val pinConfig = PinConfig.builder().setBackgroundColor(Colorg.RED).setBorderColor(Colorg.WHITE).build()//랜드마크를 표현할 마커 설정
                    var indexNum = 0
                    landmarkArray.forEach { nl ->//검색된 랜드마크들의 위치에 마커 표현
                        val ll = LatLng(nl.locate.latitude, nl.locate.longitude)
                        AdvancedMarker(
                            state = MarkerState(ll),
                            pinConfig = pinConfig,
                            onClick = {//마커가 눌렸을때 하단의 랜드마크 정보창 Open
                                placeName.value = nl.engName
                                translatedName.value = nl.translatedName
                                scope.launch { bottomSheetState.show() }
                                false
                            },
                            onInfoWindowClose = {scope.launch { bottomSheetState.hide() }})//다른 곳을 터치했을때 하단의 랜드마크 정보창 Close
                        indexNum += 1
                    }
                }
            }

        })
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    val loc = Location("TEST")
    loc.latitude = 52.9715487
    loc.longitude = -9.4411821
    val lm = NearLandmark("TEST LOC", "Test",0.5, loc)
    val arr : MutableList<NearLandmark> = mutableListOf(lm)
    val user = Location("TEST")
    val buttonTxt = mapOf("korean" to "AI 설명 보기", "english" to "See AI Description")

    user.latitude = 52.9715
    user.longitude = -9.4411821
    TravelEyeTheme (darkTheme = false) {

        LandMarkInfoMap("SOUTH KOREA","SEOUL",arr, user, "korean", buttonTxt)
    }
}
