package com.dongjin.traveleye


import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.location.Geocoder
import android.location.Geocoder.GeocodeListener
import android.location.LocationListener
import android.location.LocationManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.dongjin.traveleye.ui.theme.TravelEyeTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import java.io.File
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream


open class MainActivity : ComponentActivity() {

    private lateinit var dataStore: StoreLanguageSetting//언어 설정을 가지고 있는 dataStore 오브젝트

    private lateinit var connectionManager: ConnectivityManager//네트워크 상태를 확인하기 위한 오브젝트
    private lateinit var networkCallback : NetworkCallback//네트워크 상태에 따른 명령을 실행할 Callback 함수

    private lateinit var locationManager : LocationManager//사용자 위치정보를 가져올 LocationManager 오브젝트
    private lateinit var locationListener : LocationListener//사용자 위치정보가 바뀌었을때 반응할 LocationListener 오브젝트
    private lateinit var fusedLocationClient : FusedLocationProviderClient//사용자의 마지막 위치를 가져올 FusedLocationProviderClient 오브젝트

    private lateinit var userLocation : Location//사용자의 현재 위치를 저장할 Location 오브젝트

    private var country = mutableStateOf("")//현재 사용자 위치의 국가를 저장할 mutableState 함수
    private var city = mutableStateOf("")//현재 사용자 위치의 도시를 저장할 mutableState 함수
    private var engCountry =  mutableStateOf("")//intent로 Gemini에게 보낼 영어 국가이름 (Gemini에 영어로 질문을 할 것이기 때문)
    private var engCity = mutableStateOf("")//intent로 Gemini에게 보낼 영어 도시이름 (Gemini에 영어로 질문을 할 것이기 때문)

    private var disableBtn = mutableStateOf(false)//네트워크가 안됐을때 검색버튼을 비활성화하기 위한 mutableState 함수 (네트워크 정상시 활성화)

    private var searchTxt = mutableStateOf("장소 검색")//장소 검색 버튼임을 알리는 텍스트를 저장할 mutableState 함수 (영어로 변환 및 "검색중..."으로 바꾸기 위해 mutableState를 사용함)

    private var imgUri = mutableStateOf(Uri.EMPTY)//촬영한 이미지를 저장하기 위한 Uri mutableState 함수 (Cloud Vision에 요청시 Bitmap으로 변환)
    private val initBitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)//bitmapImg를 생성하기 위해 잠시 만든 이미지 (실제 사용 x)
    private var bitmapImg = mutableStateOf(initBitmap)//촬영한 이미지를 Cloud Vision에 검색하기 위한 변환된 Bitmap을 저장할 mutableState 함수
    private var isSearchImg = mutableStateOf(false)//MainActivity가 실행됐을때 이전 엑티비티가 카메라로 사진을 찍고온 엑티비티인지 판별할 mutableState 함수

    private lateinit var functions: FirebaseFunctions//Cloud Vision API를 firebase function으로 접근하기 위한 FirebaseFunction 오브젝트
    private lateinit var auth: FirebaseAuth//firebase에 접근하기 위해 인증할 FirebaseAuth 오브젝트

    private var showProgress = mutableStateOf(false)//Cloud Vision API 결과가 도착하기 전까지 Circular Progress bar 표시를 유무를 판별할 함수

    private var languageSetting = mutableStateOf("english")
    private val translateLanguage = mapOf("korean" to "ko", "english" to "en")//번역기 언어설정을 위한 mapOf 오브젝트 (추후 다른 언어를 추가해 번역기 설정 변경 가능하도록 함)
    private lateinit var translateApi : TranslateApi

    private val openAIDialog = mutableStateOf(false)//AI 설명 주의 다이얼로그를 띄울 유무를 판별할 mutableState 함수

    private val errorOccurred = mutableStateOf(false)//에러가 생겼는지 확인하기 위한 mutableState 함수
    private val errorState = mutableStateOf("UNKNOWN")//에러의 종류를 판별하기 위한 mutableState 함수
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val retrofit = Retrofit.Builder().baseUrl("https://translation.googleapis.com/language/translate/").addConverterFactory(GsonConverterFactory.create()).build()
        translateApi = retrofit.create(TranslateApi::class.java)

        userLocation = Location(LOCATION_SERVICE)

        checkPermissions()
        locationManager = this.applicationContext.getSystemService(LOCATION_SERVICE) as LocationManager//위치 정보 접근 오브젝트 생성
        locationListener = LocationListener { location ->
            userLocation = location
            getAddress()

        }// 위치 정보가 바뀌었을때 반응할 리스너 오브젝트
        startLocationUpdate()//위치 정보 업데이트 시작
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try{//앱 시작시 바로 검색을 하면 에러가 발생해 가장 최근에 알려진 장소를 먼저 사용자 위치로 설정, 위치 권한 설정이 안 돼있을 수 있어 try-catch로 작성함
            fusedLocationClient.lastLocation.addOnSuccessListener {
                    userLocation = it//fusedLocation은 여러 위치 제공자를 융합해 정확한 최신 위치 정보를 제공하지만 배터리 소모가 큼, 앱이 실행됐을때 맨 처음 위치를 잡아야해서 fusedLocation을 사용했음, 이후엔 리소스 절약을 위해 LocationManager사용
                    Log.d("Trans before," ," start")
                    getAddress()
            }
        }catch (_: SecurityException){
            checkPermissions()//권한 설정 함수 실행
        }
        dataStore = StoreLanguageSetting(this)//언어 설정을 가지고 있는 DataStore를 불러옴
        runBlocking {
            languageSetting.value = dataStore.getLanguageSetting.first()//앱 시작시 저장된 언어 설정을 가지고 오기까지 잠시 대기
        }
        searchTxt.value = when(languageSetting.value){//언어 설정에 맞는 장소검색 텍스트 설정
            "korean" -> "장소 검색"
            "english" -> "Search Place"
            else -> "장소 검색"
        }

        networkCallback = object : NetworkCallback(){//네트워크 연결 상태에 따라 반응할 오브젝트
            override fun onAvailable(network: Network) {//네트워크 연결이 가능할 때
                super.onAvailable(network)
                errorOccurred.value = false
                disableBtn.value = false
            }

            override fun onLost(network: Network) {//네트워크 연결이 안됐을 때 오류 메시지 출력
                super.onLost(network)
                errorOccurred.value = true
                errorState.value = "CONNECTION_LOST"
                disableBtn.value = true
            }
        }
        connectionManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager//네트워크 연결 상태 체크 오브젝트 초기화
        connectionManager.registerDefaultNetworkCallback(networkCallback)//네트워크 연결 상태에 따라 반응할 오브젝트를 콜백함수로 설정
        when (connectionManager.activeNetwork){//연결 가능한 네트워크가 없을 때 오류 메세지 출력
            null -> {
                errorOccurred.value = true
                errorState.value = "CONNECTION_LOST"
                disableBtn.value = true
            }
            else -> {
                errorOccurred.value = false
                disableBtn.value = false
            }
        }
        auth = Firebase.auth
        //val currentUser = auth.currentUser
        //updateUI(currentUser)//유저에 맞는 UI 설정 (Travel Eye는 유저에 따르 UI 설정이 필요없음)
        auth.signInAnonymously()// firebase에 익명 사용자로 로그인할때 결과값에 따른 리스너함수
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {// 로그인 성공시
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("Firebase Auth", "signInAnonymously:success")
                    //val user = auth.currentUser
                    //updateUI(user)
                } else {// 로그인 실패시 UNKNOWN 에러 다이얼로그 & 토스트 메세지 실행
                    // If sign in fails, display a message to the user.
                    Log.d("Firebase Auth", "signInAnonymously:failure", task.exception)
                    Toast.makeText(baseContext,"Authentication failed.",Toast.LENGTH_SHORT).show()
                    errorOccurred.value = true
                    errorState.value = "UNKNOWN"
                    //updateUI(null)
                }
            }
        this.auth.signInAnonymously()//firebase에 익명 사용자로 로그인

        setContent {
            TravelEyeTheme {

                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    MainActivity(dataStore, city, engCity, country, engCountry, searchTxt, disableBtn, imgUri, isSearchImg, languageSetting, translateApi, openAIDialog)
                    CircularProgressBar(showProgress = showProgress)//Cloud Vision API 결과가 오기 전까지 원형 프로그래스바 실행
                    AIDialog(openAIDialog = openAIDialog, languageSetting = languageSetting)//ai로 만들어진 설명이라는 주의 다이얼로그
                    ErrorDialog(errorOccurred = errorOccurred, errorState = errorState, languageSetting = languageSetting)//에러를 띄울 다이얼로그
                }
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("onRestart", "onRestart")
    }

    override fun onStart() {
        super.onStart()
        Log.d("onStart", "onStart")
        checkPermissions()//권한 설정 확인
        startLocationUpdate()//사용자 위치 업데이트 시작
        val mainErrorIntent = intent//다른 엑티비티에서 에러로 MainActivity로 왔을때 사용할 intent
        errorOccurred.value = mainErrorIntent.getBooleanExtra("isError", false)//다른 엑티비티에서 넘어왔을 때 intent에 있는 isError 값 확인, 없으면 false
        if (errorOccurred.value) {//에러가 발생했다는 extra가 있으면 에러 다이얼로그 출력
            errorState.value = mainErrorIntent.getStringExtra("errorMsg")!!//에러 유형 extra 참조
            Log.d("MAIN GEMINI ERROR occ", errorOccurred.value.toString())
            Log.d("MAIN GEMINI ERROR sta", errorState.value)
        }
    }

    override fun onResume() {
        super.onResume()

        Log.d("onResume", "onResume")
        if (isSearchImg.value){//이전 액티비티가 카메라 촬영 액티비티였다면 Cloud Vision API 검색 작업 실행
            showProgress.value = true//CircularProgressBar 실행
            searchTxt.value = when(languageSetting.value){//장소 검색 텍스트를 언어설정에 맞는 "검색중"으로 변경
                "korean" -> " 검색중..."
                "english" -> "Searching..."
                else -> " Searching..."
            }
            val inputStream = contentResolver.openInputStream(imgUri.value)
            bitmapImg.value = ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, imgUri.value))//Cloud Vision API에 보낼 이미지를 Uri에서 비트맵으로 변환
            //bitmapImg.value = BitmapFactory.decodeStream(inputStream)//uri -> bitmap 과정에서 사용했더니 emulator에서는 작동했지만 실제 기기에선 cloud vision api가 작동하지 않았음, ImageDecoder를 사용하니 정상작동했음
            inputStream?.close()
            bitmapImg.value = scaleBitmap(bitmapImg.value)//Cloud Vision API에 보낼 이미지를 규격에 맞게 조절
            functions = Firebase.functions

            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmapImg.value.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)//이미지를 압축
            val imageBytes: ByteArray = byteArrayOutputStream.toByteArray()
            val base64encoded = Base64.encodeToString(imageBytes, Base64.NO_WRAP)//이미지를 64비트 문자열로 변환
            val request = JsonObject()//Cloud Vision API에 request 보낼 메세지를 담을 Json 오브젝트
            val img = JsonObject()//Cloud Vision API에 request 보낼 이미지를 담을 Json 오브젝트
            img.add("content", JsonPrimitive(base64encoded))//이미지 변환 64비트 문자열을 오브젝트에 입력
            request.add("image",img)//이미지 오브젝트를 request 오브젝트에 입력
            val feature = JsonObject()//Cloud Vision API에 request 보낼 나머지 세부사항을 담을 Json 오브젝트 1
            feature.add("maxResults", JsonPrimitive(10))//최대 검색 결과를 5개로 설정FACE_DETECTION
            feature.add("type", JsonPrimitive("LANDMARK_DETECTION"))//Cloud Vision API의 검색 형식을 명소 검색으로 설정LANDMARK_DETECTION
            val features = JsonArray()//Cloud Vision API에 request 보낼 나머지 세부사항을 담을 Json 오브젝트 2
            features.add(feature)// 세부사항을 적은 오브젝트1 를 오브젝트2에 입력
            request.add("features",features)//세부사항 오브젝트를 request 오브젝트에 입력
            val intent = Intent(this, SuccessLandmark::class.java)//SuccessLandmark에 보낼 intent
            intent.putExtra("userLocation", userLocation)//SuccessLandmark에 보낼 intent 사용자 위치를 저장
            Log.d("Intent USERLOC", userLocation.latitude.toString() + ", " + userLocation.longitude)

            annotateImage(request.toString()).addOnCompleteListener{task ->
                if (task.isSuccessful){//Firebase 통신으로 Cloud Vision API와 통신완료 시
                    val e = task.exception
                    showProgress.value = false
                    searchTxt.value = when(languageSetting.value){//장소 검색 텍스트를 원래대로 복원
                        "korean" -> "장소 검색"
                        "english" -> "Search Place"
                        else -> "Search Place"
                    }
                    if (e is FirebaseFunctionsException) {//통신은 됐으나 익셉션이 생겼을때
                        val code = e.code
                        val details = e.details
                        errorOccurred.value = true//에러 다이얼로그 실행
                        errorState.value = "VISION_ERR"
                    }
                    if (task.result!!.asJsonArray[0].asJsonObject["landmarkAnnotations"].asJsonArray.size() == 0){//통신은 됐으나 장소를 찾지 못했을 경우
                        errorOccurred.value = true//에러 다이얼로그 실행
                        errorState.value = "NOT_FOUND"
                    }
                    else{//검색 결과가 있을 경우
                        var totalLandmark = 0//SuccessLandmark에 보낼 조건에 맞는 검색된 랜드마크 총 개수
                        for (label in task.result!!.asJsonArray[0].asJsonObject["landmarkAnnotations"].asJsonArray) {// 검색 결과를 순서대로 출력
                            val labelObj = label.asJsonObject
                            val landmarkName = labelObj["description"].asString//명소이름
                            val score = labelObj["score"].asDouble//해당 명소일 확률(?)


                            Log.d("VISION DESC", "Description: $landmarkName")
                            Log.d("VISION Score", "score: $score")

                            // Multiple locations are possible, e.g., the location of the depicted
                            // landmark and the location the picture was taken.

                            for (loc in labelObj["locations"].asJsonArray) {//검색된 장소들의 정보 중 위치 정보만 하나씩 가져옴
                                val latitude = loc.asJsonObject["latLng"].asJsonObject["latitude"]//검색된 장소의 위도
                                val longitude = loc.asJsonObject["latLng"].asJsonObject["longitude"]//검색된 장소의 경도
                                val place = Location(LocationManager.GPS_PROVIDER)
                                place.latitude = latitude.asDouble
                                place.longitude = longitude.asDouble
                                if (userLocation.distanceTo(place) < 5000){//사용자와 검색된 장소의 거리를 계산해 5km이내의 장소만 저장
                                    totalLandmark += 1
                                    Log.d("VISION $totalLandmark", "Distance: " + userLocation.distanceTo(place) + ", LANDMARK NAME: " + landmarkName)
                                    val landmark = NearLandmark(landmarkName, "none", score, place)//NearLandmark 인터페이스로 랜드마크 정보 저장
                                    intent.putExtra("landMark$totalLandmark", landmark)//SuccessLandmark에 보낼 intent에 검색된 랜드마크 저장
                                }
                            }
                            Log.d("VISION SUCCESS", "total: $totalLandmark")

                        }
                        isSearchImg.value = false
                        if (totalLandmark == 0){//검색된 장소 중 사용자와 5km 이내의 장소가 하나도 없다면 검색 실패 오류 실행
                            errorOccurred.value = true
                            errorState.value = "NOT_FOUND"
                        }else{
                            intent.putExtra("country",engCountry.value)//SuccessLandmark에 보낼 intent에 현재 사용자 위치 국가 영어 이름 저장
                            intent.putExtra("city",engCity.value)//SuccessLandmark에 보낼 intent에 현재 사용자 위치 도시 영어 이름 저장
                            intent.putExtra("totalLandmark",totalLandmark)//SuccessLandmark에 보낼 intent에 조건에 맞는 검색된 총 랜드마크 수 저장
                            intent.putExtra("languageSetting", languageSetting.value)//SuccessLandmark에 보낼 intent에 현재 언어설정 저장
                            startActivity(intent)////SuccessLandmark 시작
                        }
                    }

                }
                else{//Cloud Vision API와 통신 실패시 에러 메세지 출력
                    errorOccurred.value = true
                    errorState.value = "VISION_ERR"
                    Log.d("VISION", "FAIL")
                }
            }//Firebase를 통해 Cloud Vision API에 request 후 결과에 따른 리스너
            isSearchImg.value = false
            Log.d("VISION isSearchImg", isSearchImg.value.toString())
        }

    }
    override fun onPause(){
        super.onPause()
        Log.d("onPause", "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d("onStop", "onStop")
        stopLocationUpdate()//위치 정보 업데이트 중단 (리소스 확보)
    }

    override fun onDestroy() {
        Log.d("onDestroy", "onDestroy")
        super.onDestroy()
        connectionManager.unregisterNetworkCallback(networkCallback)//네트워크 상태에 따른 CallBack함수 연결 해제

    }


    private fun checkPermissions(){
        val requestPermissions = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions())//다수의 권한에 대한 요청 실행
        {permissions ->
            permissions.entries.forEach{(permission, isGranted) ->//권한들을 하나씩 확인
                when{
                    isGranted -> {//권한이 허가됐을 때
                        if (permission == (Manifest.permission.ACCESS_FINE_LOCATION)){//정확한 위치정보 권한이 허가됐을때
                            startLocationUpdate()//사용자 위치 정보 업데이트 시작
                        }
                        try{
                            fusedLocationClient.lastLocation.addOnSuccessListener {//마지막으로 알려진 사용자 정보를 불러옴
                                userLocation = it
                                //getAddress()
                            }
                        }catch (_: SecurityException){
                        }
                    }
                    !isGranted -> {//권한이 거부됐을때 (3번 거부시 구글 정책에 따라 앱 실행 불가 & 사용자가 직접 설정에서 권한을 부여해야함)

                        if (permission == Manifest.permission.ACCESS_FINE_LOCATION){//정확한 위치정보 권한이 거부됐을때
                            shouldShowRequestPermissionRationale(permission)//정확한 위치정보 권한 요청
                            Toast.makeText(this, "위치 권한을 허용해주세요", Toast.LENGTH_LONG).show()//토스트 메세지 출력
                            finish()//앱 종료
                        }
                        if (permission == Manifest.permission.CAMERA) {//카메라 권한이 거부됐을때
                            shouldShowRequestPermissionRationale(permission)//카메라 권한 요청
                            Toast.makeText(this, "카메라 권한을 허용해주세요", Toast.LENGTH_LONG).show()//토스트 메세지 출력
                            finish()//앱 종료
                        }
                    }
                    else -> {//권한 허가 & 거부 이외에 관한 작업이 필요할 때
                        Toast.makeText(this, "권한 허가가 필요한 앱입니다.", Toast.LENGTH_LONG).show()//토스트 메세지 출력
                    }
                }
            }
        } //권한이 거부 상태일 경우 토스트 메시지 출력 후 종료
        requestPermissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.CAMERA))//권한 확인 명령 실행


    }//사진, 위치 등 권한 체크를 하는 함수

    private fun startLocationUpdate(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            checkPermissions()
            return
        }//대략적인 위치정보 or 정확한 위치정보 권한이 거부됐을때 권한 요청 함수[checkPermissions()] 실행
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000,10f, locationListener, Looper.getMainLooper())
    }//사용자의 위치좌표를 가져올 함수, 5초에 한번씩 가져온다
    private fun stopLocationUpdate(){
        locationManager.removeUpdates(locationListener)
    }//사용자의 위치 좌표를 호출을 중지하는 함수, 리소스 제어를 위해 사용
    private fun getAddress(){ // 사용자의 위치좌표를 가지고 해당 위치의 국가와 도시를 가져와 텍스트를 바꾸는 함수
        val geocoder = Geocoder(this, Locale.ENGLISH)//주소 검색을 실행할 오브젝트 초기화, 검색 언어를 영어로 함 (일관성을 위해 영어로 설정)
        Log.d("BUILD SDK", Build.VERSION.SDK_INT.toString())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){ // API 33부턴 getFromLocation이 deprecated 돼 api 수준에 따라 분리함 (33이후 버전에 관한 작업)
            val geoListener = GeocodeListener {//사용자 위치에 맞는 주소 검색
                addresses ->

                engCountry.value = addresses[0].countryName//Gemini에 보낼 영어로 된 국가 이름 저장
                if  (addresses[0].adminArea == null){//주소에 도시가 없을 경우 도시는 공백 처리 (싱가포르 같은 도시국가는 도시가 따로 없음)
                    city.value = ""
                    engCity.value = ""
                }else{//주소에 도시가 있을 경우
                    engCity.value = addresses[0].adminArea//Gemini에 보낼 영어로 된 도시 이름 저장
                }

                if (languageSetting.value != "english") {//언어 설정이 영어가 아닌 경우 번역기로 나라, 도시 번역
                    translateAddr(engCountry.value, translateLanguage[languageSetting.value]!!,country)
                    if (addresses[0].adminArea != null){//주소에 도시가 있을 경우 도시도 번역 & 저장
                        translateAddr(engCity.value, translateLanguage[languageSetting.value]!!, city)
                    }
                }
                else{//언어 설정이 영어인 경우
                    country.value = addresses[0].countryName//영어로 된 국가 이름을 UI에 적용
                    if (addresses[0].adminArea != null){//주소에 도시가 있을 경우
                        city.value = addresses[0].adminArea//주소에 도시가 있을 경우 도시도 저장
                    }
                }

            }

            geocoder.getFromLocation(userLocation.latitude,userLocation.longitude,1,geoListener)
        }
        else{// API 33부턴 getFromLocation이 deprecated 돼 api 수준에 따라 분리함 (33이전 버전에 관한 작업)
            val addr = geocoder.getFromLocation(userLocation.latitude,userLocation.longitude,1)
            if (addr != null) {
                if (addr.isNotEmpty()){
                    val address = addr[0]

                    engCountry.value = address.countryName//Gemini에 보낼 영어로 된 국가 이름 저장

                    if(address.adminArea == null){//주소에 도시가 없을 경우 도시는 공백 처리 (싱가포르 같은 도시국가는 도시가 따로 없음)
                        city.value = ""
                        engCity.value = ""
                    } else{//주소에 도시가 있을 경우
                        engCity.value = address.adminArea//Gemini에 보낼 영어로 된 도시 이름 저장
                    }

                    if (languageSetting.value != "english"){//언어 설정이 영어가 아닌 경우 번역기로 나라, 도시 번역
                        translateAddr(engCountry.value, translateLanguage[languageSetting.value]!!, country)
                        if (address.adminArea != null){//주소에 도시가 있을 경우 도시도 번역 & 저장
                            translateAddr(engCity.value, translateLanguage[languageSetting.value]!!, city)
                        }
                    }
                    else{//언어 설정이 영어일 경우
                        country.value = address.countryName//국가 이름을 UI에 적용

                        if (address.adminArea != null){
                            city.value = address.adminArea//도시 이름을 UI에 적용
                        }
                    }
                }
            }
        }
    } //현재 사용자 위치에 해당하는 국가와 도시를 가져와 텍스트를 바꾸는 함수

    private fun translateAddr(addr: String, target: String, txtState : MutableState<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = translateApi.translate(
                key = BuildConfig.CLOUD_TRANSLATION,
                q = addr,
                source = "en",
                target = target,
                format = "text"
            )
            txtState.value = response.data.translations[0].translatedText
            Log.d("Trans Success", response.data.translations[0].translatedText)
            return@launch
        }
    }

    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        var resizedWidth = 640
        var resizedHeight = 640
        if (originalHeight > originalWidth) {
            resizedHeight = 640
            resizedWidth = (resizedHeight * originalWidth.toFloat() / originalHeight.toFloat()).toInt()
        } else if (originalWidth > originalHeight) {
            resizedWidth = 640
            resizedHeight = (resizedWidth * originalHeight.toFloat() / originalWidth.toFloat()).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false)
    }//Cloud Vision API에 보낼 사진을 규격에 맞게 조절 [640x640]
    private fun annotateImage(requestJson: String): Task<JsonElement> {
        return functions
            .getHttpsCallable("annotateImage")
            .call(requestJson)
            .continueWith { task ->
                // This continuation runs on either success or failure, but if the task
                // has failed then result will throw an Exception which will be
                // propagated down.
                val result = task.result?.data

                JsonParser.parseString(Gson().toJson(result))
            }
    }//Firebase에 사진 전달 후 검색 결과를 리턴


    //private fun updateUI(user: FirebaseUser?) {//Firebase 로그인한 사용자에 맞게 Ui 업데이트
    //}
}

@Composable
//fun MainActivity(cityState: MutableState<String>, countryState: MutableState<String>, searchTxt: MutableState<String>, imgUri: MutableState<Uri>, isSearch: MutableState<Boolean>, languageSetting: MutableState<String>, modifier: Modifier = Modifier) {
fun MainActivity(dataStore: StoreLanguageSetting , cityState: MutableState<String>, engCity: MutableState<String>, countryState: MutableState<String>, engCountry: MutableState<String>, searchTxt: MutableState<String>, disableBtn : MutableState<Boolean>, imgUri: MutableState<Uri>, isSearch: MutableState<Boolean>, languageSetting: MutableState<String>, translateApi: TranslateApi, openDialog: MutableState<Boolean>, modifier: Modifier = Modifier) {
    val contxt = LocalContext.current

    Column(modifier.fillMaxSize()) {
        Spacer(modifier = modifier.fillMaxHeight(0.02f))
        UserLocaleTxt(cityState = cityState, countryState = countryState)
        Spacer(modifier = modifier.fillMaxHeight(0.255f))
        CameraBtn(disableBtn, searchTxt,imgUri, isSearch, contxt)
        Row (modifier = modifier.fillMaxWidth() ,horizontalArrangement = Arrangement.Center){
            SelectTranslation(dataStore, languageSetting, cityState, countryState , engCountry, engCity, searchTxt, translateApi)
        }
        Column (verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = modifier.fillMaxHeight(0.35f))
            Row (modifier = modifier.fillMaxWidth() ,horizontalArrangement = Arrangement.Center){
                IconButton(modifier = modifier
                    .fillMaxWidth(0.25f)
                    .fillMaxHeight(0.25f),
                    onClick = {
                        openDialog.value = true
                    }) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AIDialog", tint = Color(38,124,240,255), modifier = modifier.fillMaxSize())
                }
            }
        }

    }

}

@Composable
fun UserLocaleTxt(cityState: MutableState<String>, countryState: MutableState<String>, modifier: Modifier = Modifier){
    Row{
        Spacer(modifier = modifier.fillMaxWidth(0.025f))
        Icon(imageVector = Icons.Filled.Place, contentDescription = "user_locale",
            modifier
                .fillMaxHeight(0.05f)
                .fillMaxWidth(0.1f)
                , tint = LocalContentColor.current)
        Column {
            Text(text = countryState.value, fontSize = 25.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = cityState.value, fontSize = 25.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)// 텍스트를 외부에서 바꾸기 위해선 mutableState의 Value를 외부에서 바꾸면, 컴포즈는 State를 받아 안에 있는 value를 받아 사용한다.
        }

    }
}

@Composable

fun CameraBtn(disableBtn: MutableState<Boolean>, searchTxt: MutableState<String>,imgUri: MutableState<Uri>, isSearch: MutableState<Boolean>,contxt: Context,modifier: Modifier = Modifier){
    val btnColor = ButtonDefaults.buttonColors(Color(0,179,219,255))
    val btnElevation = ButtonDefaults.buttonElevation(defaultElevation = 7.dp)
    //val uri = Uri.EMPTY
    val uri = contxt.createTmpImageUri()
    val camLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture(), onResult = {
        Log.d("CAMERA LAUNCHER", it.toString())
        if (it){
                imgUri.value = uri
                isSearch.value = true
            }
         })
    Column(verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.CenterHorizontally){
        Row(modifier = modifier
            .fillMaxWidth(0.5f)
            .fillMaxHeight(0.39f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            ElevatedButton(enabled = !(disableBtn.value), onClick = {
                camLauncher.launch(uri)
            }, modifier = modifier.fillMaxSize(),shape = RoundedCornerShape(50.dp), colors = btnColor, elevation = btnElevation) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "camera_btn",
                    modifier = modifier.fillMaxSize(),
                    tint = Color.Black
                )

            }
        }
        Spacer(modifier = modifier.fillMaxHeight(0.02f))
        Text(text=searchTxt.value,fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = modifier.fillMaxWidth(), textAlign = TextAlign.Center)

    }

}//
@Composable
fun SelectTranslation(dataStore: StoreLanguageSetting, languageSetting: MutableState<String>, cityState: MutableState<String>, countryState: MutableState<String>, engCountry: MutableState<String>, engCity: MutableState<String>, searchTxt: MutableState<String>, translateApi: TranslateApi, modifier: Modifier = Modifier){
    val scope = rememberCoroutineScope()
    var expandState by remember { mutableStateOf(false) }
    val language: String
    var targetLang = "ko"
    when (languageSetting.value) {
        "korean" -> {
            language = "한국어"
            targetLang = "ko"
        }
        "english" -> {
            language = "English"
        }
        else -> {language = "Language"}
    }
    Row (horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = modifier.fillMaxHeight(0.25f))
        Column {
            TextButton(onClick = { expandState = true }, shape = RoundedCornerShape(5.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
                modifier = modifier
                    .fillMaxWidth(0.36f)
                    .border(2.dp, LocalContentColor.current, RectangleShape)) {
                Icon(imageVector = Icons.Default.Translate, contentDescription = "translate")
                Text(text = "  $language  ")
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "open_menu")
            }
            Column {
                DropdownMenu(expanded = expandState, onDismissRequest = { expandState = false },
                    modifier
                        .wrapContentSize()
                        .fillMaxWidth(0.36f)) {
                    DropdownMenuItem(text = { Text("한국어", textAlign = TextAlign.Center, modifier = modifier.fillMaxWidth()) },//언어를 한국어로 설정
                        onClick = {
                            languageSetting.value = "korean"
                            if(cityState.value != ""){
                                CoroutineScope(Dispatchers.IO).launch {
                                    val response = translateApi.translate(
                                        key = BuildConfig.CLOUD_TRANSLATION,
                                        q = engCity.value,
                                        source = "en",
                                        target = targetLang,
                                        format = "text"
                                    )
                                    cityState.value = response.data.translations[0].translatedText
                                    Log.d("Trans Success", response.data.translations[0].translatedText)
                                    return@launch
                                }
                            }
                            CoroutineScope(Dispatchers.IO).launch {
                                val response = translateApi.translate(
                                    key = BuildConfig.CLOUD_TRANSLATION,
                                    q = engCountry.value,
                                    source = "en",
                                    target = targetLang,
                                    format = "text"
                                )
                                countryState.value = response.data.translations[0].translatedText
                                Log.d("Trans Success", response.data.translations[0].translatedText)
                                return@launch
                            }
                            searchTxt.value = "장소 검색"
                            scope.launch {
                                dataStore.updateLanguageSetting(languageSetting.value)//dataStore에 있는 언어 설정 값을 한국어로 변경
                            }
                            expandState = false
                        })
                    DropdownMenuItem(text = { Text("English", textAlign = TextAlign.Center, modifier = modifier.fillMaxWidth()) },//언어를 영어로 설정
                        onClick = {
                            languageSetting.value = "english"
                            if(cityState.value != ""){
                                cityState.value = engCity.value
                            }
                            countryState.value = engCountry.value
                            searchTxt.value = "Search Place"
                            scope.launch {
                                dataStore.updateLanguageSetting(languageSetting.value)//dataStore에 있는 언어 설정 값을 영어로 변경
                            }
                            expandState = false
                        })
                }
            }
        }
    }


}

fun Context.createTmpImageUri(
    provider: String = "${this.applicationContext.packageName}.provider",
    fileName: String = "picture_${System.currentTimeMillis()}",
    fileExtension: String = ".png"
): Uri {
    val tmpFile = File.createTempFile(fileName, fileExtension, cacheDir).apply { createNewFile() }
    Log.d("Photo", FileProvider.getUriForFile(applicationContext, provider, tmpFile).toString())
    return FileProvider.getUriForFile(applicationContext, provider, tmpFile)
} // 찍은 이미지를 Uri로 사용하기 위한 함수 (이미지는 저장하지 않고 임시 저장소에서 꺼내서 사용 후 없어짐)

@Composable
private fun CircularProgressBar(showProgress : MutableState<Boolean>, modifier: Modifier = Modifier) {
    val loading by remember { showProgress }

    if (!loading) return

    Column (
        modifier
            .fillMaxWidth()
            .fillMaxHeight(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally){
        Row (modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.96f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
        ){
            CircularProgressIndicator(
                modifier = modifier.fillMaxSize(0.16f),
                color = Color.Yellow,
                strokeWidth = 10.5.dp,
            )
        }
    }

}// API로부터 결과값을 받아올 때까지 처리중을 표시할 원형 프로그래스 바
@Composable
private fun AIDialog(openAIDialog : MutableState<Boolean>, languageSetting: MutableState<String>, modifier: Modifier = Modifier){//앱이 AI에 의해 작동함으로 오류가 있음을 알리는 주의사항 다이얼로그
    val dialogTxt = when(languageSetting.value)  {
       "korean" -> {
          "본 앱은 AI 기술을 활용하므로\n각종 오류가 생길 수 있습니다.\n장소 정보 및 검색은\n참고용으로만 활용하시길\n 권장합니다."
       }

       else ->{
          "This app utilizes AI.\nTherefore it may occur several errors.\nPlease use landmark information & search results as reference only."
       }
    }
    when {
        openAIDialog.value ->
            AlertDialog(
                modifier = modifier.fillMaxWidth(),
                icon = {Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "MainAIDialog", tint = Color.Gray, modifier = modifier.size(40.dp))},
                text = { Text(text = dialogTxt, fontSize = 20.sp, textAlign = TextAlign.Center,lineHeight = 30.sp, letterSpacing = 1.sp)},
                onDismissRequest = { openAIDialog.value = false },//다른 곳을 터치했을때 다이얼로그 꺼짐
                confirmButton = {
                        TextButton(onClick = { openAIDialog.value = false }) {
                            Text(text = "OKAY", fontSize = 25.sp)
                        }
                })
    }
}

@Composable
private fun ErrorDialog(errorOccurred: MutableState<Boolean>, errorState: MutableState<String>, languageSetting: MutableState<String>, modifier: Modifier = Modifier){//다양한 에러에 따른 에러 다이얼로그를 언어설정에 맞게 띄울 에러 다이얼로그
    var titleTxt = ""
    var errorTxt = ""

    when (languageSetting.value){
        "korean" -> {
            when (errorState.value){
                "NOT_FOUND" -> {
                    titleTxt = "검색 결과 없음"
                    errorTxt = "검색된 장소가 없습니다."
                }
                "CONNECTION_LOST" -> {
                    titleTxt = "네트워크 없음"
                    errorTxt = "네트워크에 연결할 수\n 없습니다.\n 데이터 연결 확인 후\n 재시도 해주세요."
                }
                "VISION_ERR" -> {
                    titleTxt = "장소 검색 오류"
                    errorTxt = "사진 검색 AI에서\n오류가 발생했습니다."
                }
                "MAP_ERR" -> {
                    titleTxt = "지도 오류"
                    errorTxt = "지도를 표현하던 중\n오류가 발생했습니다."
                }
                "GEMINI_ERR" -> {
                    titleTxt = "장소 정보 오류"
                    errorTxt = "장소 정보 AI에서\n오류가 발생했습니다."
                }
                "UNKNOWN" ->{
                    titleTxt = "알 수 없는 오류"
                    errorTxt = "알 수 없는\n오류가 발생했습니다."
                }
            }
        }
        else -> {
            when (errorState.value){
                "NOT_FOUND" -> {
                    titleTxt = "No Search Result"
                    errorTxt = "Can't find landmark\nfrom the picture"
                }
                "CONNECTION_LOST" -> {
                    titleTxt = "Network Lost"
                    errorTxt = "Can't connect to network.\n Please check network settings."
                }
                "VISION_ERR" -> {
                    titleTxt = "Place Search Error"
                    errorTxt = "Error occurred from Vision AI"
                }
                "MAP_ERR" -> {
                    titleTxt = "Map Error"
                    errorTxt = "Erorr occurred from map"
                }
                "GEMINI_ERR" -> {
                    titleTxt = "Place Information Error"
                    errorTxt = "Error occurred from\nPlace Information AI"
                }
                "UNKNOWN" ->{
                    titleTxt = "Unknown Error"
                    errorTxt = "Unknown\nError Occurred."
                }
            }
        }
    }

    when {
        errorOccurred.value -> {
            AlertDialog(
                icon = {Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = "ErrorDialog", tint = Color.Gray, modifier = modifier.size(40.dp))},
                title = { Text(text = titleTxt, textAlign = TextAlign.Center) },
                text = { Text(text = errorTxt, textAlign = TextAlign.Center, fontSize = 23.sp, lineHeight = 34.sp, letterSpacing = 1.sp) },
                onDismissRequest = {
                    errorOccurred.value = false
                    errorState.value = "" },
                confirmButton = {
                    TextButton(onClick = {
                        errorOccurred.value = false
                        errorState.value = "UNKNOWN"
                    }) {
                        Text(text = "OKAY", fontSize = 25.sp)
                    }
                })
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
   //val cityState = mutableStateOf("Seoul")
   //val countryState = mutableStateOf("Korea")
   //val searchTxt = mutableStateOf("장소검색")
   //val img = mutableStateOf(Uri.EMPTY)
   //val isSearch = mutableStateOf(false)
   //val languageSetting = mutableStateOf("english")
   //val openDialog = mutableStateOf(true)
   //val langOptions = TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.ENGLISH).setTargetLanguage(TranslateLanguage.KOREAN).build()
   //val translator = com.dongjin.traveleye.Translation.getClient(TranslatorOptions.Builder().build())
   //val errorOccurred = mutableStateOf(true)
   //val errorValue = mutableStateOf("VISION_ERR")//NOT_FOUND, VISION_ERR, MAP_ERR, GEMINI_ERR
   TravelEyeTheme {
       //MainActivity(cityState,countryState,searchTxt,img,isSearch, languageSetting)
       CircularProgressBar(showProgress = mutableStateOf(true))
       //ErrorDialog(errorOccurred = errorOccurred, errorValue = errorValue, languageSetting = languageSetting)
       //AIDialog(openDialog = openDialog, languageSetting)
   }
}

