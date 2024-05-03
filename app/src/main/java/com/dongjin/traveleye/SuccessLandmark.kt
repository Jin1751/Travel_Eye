package com.dongjin.traveleye

import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PinConfig
import com.google.maps.android.compose.AdvancedMarker
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberMarkerState
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.launch

@Parcelize
data class NearLandmark(var engName: String, var translatedName : String, val score: Double, val locate : Location) : Parcelable

class SuccessLandmark : ComponentActivity() {
    private lateinit var locationIntent : Intent
    private lateinit var userLocation :Location
    private val landmarkArray : MutableList<NearLandmark> = mutableListOf()//랜드마크들을 저장할 리스트
    private lateinit var engKorTranslator : Translator
    private lateinit var translatorCondition : DownloadConditions
    private lateinit var languageSetting : String
    private val translateLanguage = mapOf("korean" to TranslateLanguage.KOREAN, "english" to TranslateLanguage.ENGLISH)
    private val buttonTxt = mapOf("korean" to "AI 설명 보기", "english" to "See AI Description")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationIntent = intent
        userLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {//빌드 버전 33 이상 시 인텐트에서 사용자 위치 가져오는 작업
            locationIntent.getParcelableExtra("userLocation",Location::class.java)!!
        }else{//빌드 버전 33 미만시
            locationIntent.getParcelableExtra("userLocation")!!
        }
        Log.d("SUCCESS USERLOC", userLocation.latitude.toString() + ", " + userLocation.longitude)
        val langOptions = TranslatorOptions.Builder().setSourceLanguage(translateLanguage["english"]!!).setTargetLanguage(translateLanguage["korean"]!!).build()
        engKorTranslator = Translation.getClient(langOptions)
        translatorCondition = DownloadConditions.Builder().requireWifi().build()
        engKorTranslator.downloadModelIfNeeded(translatorCondition).addOnFailureListener { Toast.makeText(this,"번역 언어 다운로드 오류",Toast.LENGTH_LONG).show() }
        languageSetting = locationIntent.getStringExtra("languageSetting")!!
       setContent {
                TravelEyeTheme {
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        LandMarkInfoMap(landmarkArray, userLocation, languageSetting, buttonTxt)
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
                if (languageSetting != "english"){
                    engKorTranslator.translate(lm.engName).addOnSuccessListener {
                        lm.translatedName = it
                    }
                }

                landmarkArray.add(lm)
                Log.d("LandMark$i", lm.engName + ", " + lm.score)
            }
            else{
                Log.d("LandMark$i", "NULL")
            }
        }

    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LandMarkInfoMap( landmarkArray: MutableList<NearLandmark>, userLocation: Location, languageSetting : String, buttonTxt : Map<String, String>, modifier: Modifier = Modifier){
    val context = LocalContext.current
    val deviceInfo = LocalConfiguration.current //현재 디바이스의 정보
    val mapCamera = CameraPosition.fromLatLngZoom(LatLng(userLocation.latitude,userLocation.longitude),15.0f)//구글 지도 카메라
    val scope = rememberCoroutineScope()//하단의 정보창을 끄고 킬 때 사용할 코루딘 스코프
    val bottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)//하단 정보창의 상태를 컨트롤 할 상태 변수
    val placeName = mutableStateOf("Please Select LandMark")//랜드마크 이름을 표시할 텍스트 뷰에 들어갈 이름 변수
    val translatedName = mutableStateOf("Please Select LandMark")

    ModalBottomSheetLayout(//지도 하단에 표시될 랜드마크 정보창 & 구글 맵
        sheetState = bottomSheetState, sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        sheetContent = @Composable {
            Column {
                Row(modifier = modifier.offset(x = (deviceInfo.screenWidthDp / 2 - 30).dp)){
                    Icon(modifier = modifier.offset(y = (-10).dp),imageVector = ImageVector.vectorResource(R.drawable.handle), contentDescription = "")
                }//하단 랜드마크 정보창의 핸들 배치
                Text(modifier = modifier.offset(x=5.dp, y = (-15).dp),text = placeName.value, color = Color.Black, fontSize = 25.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)//선택된 랜드마크의 이름
                Row(modifier = modifier.offset(x = 8.dp)){
                    Icon(imageVector = Icons.Default.Translate, contentDescription = "translate", tint = Color.Gray)
                    Text(modifier = modifier.offset(x=5.dp, y = (-5).dp),text = translatedName.value, color = Color.Gray, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)//선택된 랜드마크의 이름
                }
                Spacer(modifier = modifier.height(20.dp))
                Button(modifier = modifier
                    .width(deviceInfo.screenWidthDp.dp)
                    .height((deviceInfo.screenHeightDp * 0.065).dp), shape = RoundedCornerShape(10.dp)
                    ,onClick = {
                        val intent = Intent(context, ExplainLandMark::class.java)
                        intent.putExtra("languageSetting", languageSetting)
                        intent.putExtra("LandmarkName", placeName.value)
                        intent.putExtra("TranslatedName", translatedName.value)
                        startActivity(context,intent,null)})// AI 설명을 요청할 버튼
                {
                    Text(buttonTxt[languageSetting]!!, fontSize = 20.sp)
                }
                Spacer(modifier = modifier.height(3.dp))
            }
        },
        content = {//랜드마크 설명 창 외에 표시 될 콘텐츠 [구글 지도]
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
    TravelEyeTheme {
        LandMarkInfoMap(arr, user, "korean", buttonTxt)
    }
}
