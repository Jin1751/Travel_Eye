package com.dongjin.traveleye

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Geocoder.GeocodeListener
import android.location.LocationListener
import android.location.LocationManager
import android.location.Location
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.io.ByteArrayOutputStream
import kotlin.system.exitProcess

open class MainActivity : ComponentActivity() {
    private lateinit var locationManager : LocationManager
    private lateinit var locationListener : LocationListener
    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var userLocation : Location
    private var country = mutableStateOf("나라")
    private var city = mutableStateOf("도시")
    private var searchTxt = mutableStateOf("장소 검색")
    private var imgUri = mutableStateOf(Uri.EMPTY)
    private val initBitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
    private var bitmapImg = mutableStateOf(initBitmap)
    private lateinit var functions: FirebaseFunctions
    private lateinit var auth: FirebaseAuth
    private var isSearchImg = mutableStateOf(false)
    private var showProgress = mutableStateOf(false)
    private lateinit var engKorTranslator : Translator
    private lateinit var translatorCondition : DownloadConditions
    private var languageSetting = mutableStateOf("korean")
    private val openDialog = mutableStateOf(false)
    private val errorOccurred = mutableStateOf(false)
    private val errorValue = mutableStateOf("NOT_FOUND")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
        val currentUser = auth.currentUser
        updateUI(currentUser)
        auth.signInAnonymously()// firebase에 익명 사용자로 로그인할때 결과값에 따른 리스너함수
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {// 로그인 성공시
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("Firebase Auth", "signInAnonymously:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {// 로그인 실패시
                    // If sign in fails, display a message to the user.
                    Log.w("Firebase Auth", "signInAnonymously:failure", task.exception)
                    Toast.makeText(baseContext,"Authentication failed.",Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
        this.auth.signInAnonymously()//firebase에 익명 사용자로 로그인
        val langOptions = TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.ENGLISH).setTargetLanguage(TranslateLanguage.KOREAN).build()//영어 -> 한국어 번역옵션 설정
        engKorTranslator = Translation.getClient(langOptions)//mlkit 번역기 생성
        translatorCondition = DownloadConditions.Builder().requireWifi().build()// 번역 언어 다운로드 상태 체크 오브젝트
        engKorTranslator.downloadModelIfNeeded(translatorCondition).addOnFailureListener { Toast.makeText(this,"번역 언어 다운로드 오류",Toast.LENGTH_LONG).show() }//번역 언어 다운로드

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try{//앱 시작시 바로 검색을 하면 에러가 발생해 가장 최근에 알려진 장소를 먼저 사용자 위치로 설정, 위치 권한 설정이 안 돼있을 수 있어 try-catch로 작성함
            fusedLocationClient.lastLocation.addOnSuccessListener {
                userLocation = it//fusedLocation은 여러 위치 제공자를 융합해 정확한 최신 위치 정보를 제공하지만 배터리 소모가 큼, 앱이 실행됐을때 맨 처음 위치를 잡아야해서 fusedLocation을 사용했음, 이후엔 리소스 절약을 위해 LocationManager사용
                getAddress()
            }
        }catch (_: SecurityException){
            checkPermissions()
        }

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager//위치 정보 접근 오브젝트 생성
        locationListener = LocationListener { location ->
            userLocation = location
            getAddress()
        }// 위치 정보가 바뀌었을때 반응할 리스너 오브젝트
        setContent {
            TravelEyeTheme {

                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainActivity(city, country, searchTxt, imgUri, isSearchImg, languageSetting, engKorTranslator, openDialog)//
                    CircularProgressBar(showProgress = showProgress)
                    AIDialog(openDialog = openDialog, languageSetting = languageSetting)
                    ErrorDialog(errorOccurred = errorOccurred, errorValue = errorValue, languageSetting = languageSetting)
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
        checkPermissions()

        startLocationUpdate()

    }

    override fun onResume() {
        super.onResume()

        Log.d("onResume", "onResume")
        if (isSearchImg.value){//이전 액티비티가 카메라 촬영 액티비티였다면 CLoud Vision API 검색 작업 실행
            showProgress.value = true
            searchTxt.value = when(languageSetting.value){
                "korean" -> " 검색중..."
                "english" -> "Searching..."
                else -> " 검색중..."
            }
            val inputStream = contentResolver.openInputStream(imgUri.value)
            bitmapImg.value = BitmapFactory.decodeStream(inputStream)//Cloud Vision API에 보낼 이미지를 Uri에서 비트맵으로 변환
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
            feature.add("maxResults", JsonPrimitive(5))//최대 검색 결과를 5개로 설정
            feature.add("type", JsonPrimitive("LANDMARK_DETECTION"))//Cloud Vision API의 검색 형식을 명소 검색으로 설정
            val features = JsonArray()//Cloud Vision API에 request 보낼 나머지 세부사항을 담을 Json 오브젝트 2
            features.add(feature)// 세부사항을 적은 오브젝트1 를 오브젝트2에 입력
            request.add("features",features)//세부사항 오브젝트를 request 오브젝트에 입력
            val intent = Intent(this, SuccessLandmark::class.java)
            intent.putExtra("userLocation", userLocation)
            Log.d("Intent USERLOC", userLocation.latitude.toString() + ", " + userLocation.longitude)

            annotateImage(request.toString()).addOnCompleteListener{task ->
                if (task.isSuccessful){//Firebase 통신으로 Cloud Vision API와 통신완료 시
                    val e = task.exception
                    showProgress.value = false
                    searchTxt.value = when(languageSetting.value){
                        "korean" -> "장소 검색"
                        "english" -> "Search Place"
                        else -> "장소 검색"
                    }
                    if (e is FirebaseFunctionsException) {//통신은 됐으나 익셉션이 생겼을때
                        val code = e.code
                        val details = e.details
                        errorOccurred.value = true
                        errorValue.value = "VISION_ERR"
                    }
                    if (task.result!!.asJsonArray[0].asJsonObject["landmarkAnnotations"].asJsonArray.size() == 0){//통신은 됐으나 장소를 찾지 못했을 경우
                        errorOccurred.value = true
                        errorValue.value = "NOT_FOUND" }
                    else{//검색 결과가 있을 경우
                        var totalLandmark = 0
                        for (label in task.result!!.asJsonArray[0].asJsonObject["landmarkAnnotations"].asJsonArray) {// 검색 결과를 순서대로 출력
                            val labelObj = label.asJsonObject
                            val landmarkName = labelObj["description"].asString//명소이름
                            val score = labelObj["score"].asDouble//해당 명소일 확률(?)


                            Log.d("VISION DESC", "Description: $landmarkName")
                            Log.d("VISION Score", "score: $score")

                            // Multiple locations are possible, e.g., the location of the depicted
                            // landmark and the location the picture was taken.
                            for (loc in labelObj["locations"].asJsonArray) {
                                val latitude = loc.asJsonObject["latLng"].asJsonObject["latitude"]
                                val longitude = loc.asJsonObject["latLng"].asJsonObject["longitude"]
                                val place = Location(LocationManager.GPS_PROVIDER)
                                place.latitude = latitude.asDouble
                                place.longitude = longitude.asDouble
                                if (userLocation.distanceTo(place) < 5000){//사용자와 5km이내의 장소만 저장
                                    totalLandmark += 1
                                    Log.d("VISION $totalLandmark", "Distance: " + userLocation.distanceTo(place) + ", LANDMARK NAME: " + landmarkName)
                                    val landmark = NearLandmark(landmarkName, landmarkName, score, place)
                                    intent.putExtra("landMark$totalLandmark", landmark)
                                }
                                Log.d("VISION", "Distance: " + userLocation.distanceTo(place))
                            }

                        }
                        isSearchImg.value = false
                        intent.putExtra("totalLandmark",totalLandmark)
                        intent.putExtra("languageSetting", languageSetting.value)
                        startActivity(intent)
                    }

                }
                else{Log.d("VISION", "FAIL")}
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
        //engKorTranslator.close()
        stopLocationUpdate()
    }

    override fun onDestroy() {
        Log.d("onDestroy", "onDestroy")
        super.onDestroy()
    }

    private fun checkPermissions(){
        val requestPermissions = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions())
        {permissions ->
            permissions.entries.forEach{(permission, isGranted) ->
                when{
                    isGranted -> {
                        try{
                            fusedLocationClient.lastLocation.addOnSuccessListener {
                                userLocation = it
                            }
                        }catch (_: SecurityException){
                        }
                    }
                    !isGranted -> {
                        if (permission == Manifest.permission.ACCESS_FINE_LOCATION){
                            shouldShowRequestPermissionRationale(permission)
                            Toast.makeText(this, "위치 권한을 허용해주세요", Toast.LENGTH_SHORT).show()
                            ActivityCompat.finishAffinity(this)
                            exitProcess(0)
                        }
                        if (permission == Manifest.permission.CAMERA) {
                            shouldShowRequestPermissionRationale(permission)
                            Toast.makeText(this, "카메라 권한을 허용해주세요", Toast.LENGTH_SHORT).show()
                            ActivityCompat.finishAffinity(this)
                            exitProcess(0)
                        }
                    }
                    else -> {
                        Toast.makeText(this, "권한 허가가 필요한 앱입니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } //권한이 거부 상태일 경우 토스트 메시지 출력 후 종료
        requestPermissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.CAMERA))


    }//사진, 위치 등 권한 체크를 하는 함수

    private fun startLocationUpdate(){
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000,10f, locationListener, Looper.getMainLooper())

    }//사용자의 위치좌표를 가져올 함수, 5분에 한번씩 가져온다
    private fun stopLocationUpdate(){
        locationManager.removeUpdates(locationListener)
    }//사용자의 위치 좌표를 호출을 중지하는 함수, 리소스 제어를 위해 사용
    private fun getAddress(){ // 사용자의 위치좌표를 가지고 해당 위치의 국가와 도시를 가져와 텍스트를 바꾸는 함수
        val geocoder = Geocoder(this, Locale.getDefault())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){ // API 33부턴 getFromLocation이 deprecated 돼 api 수준에 따라 분리함
            val geoListener = GeocodeListener {
                addresses ->
                if (languageSetting.value != "english") {//언어 설정이 영어가 아닌 경우 번역기로 나라, 도시 번역
                    engKorTranslator.translate(addresses[0].countryName).addOnSuccessListener {
                        country.value = it
                    }
                    engKorTranslator.translate(addresses[0].adminArea).addOnSuccessListener {
                        city.value = it
                    }
                }
                else{
                    country.value = addresses[0].countryName
                    city.value = addresses[0].adminArea
                }

            }
            geocoder.getFromLocation(userLocation.latitude,userLocation.longitude,1,geoListener)
        }
        else{
            val addr = geocoder.getFromLocation(userLocation.latitude,userLocation.longitude,1)
            if (addr != null) {
                if (addr.isNotEmpty()){
                    val address = addr[0]
                    if (languageSetting.value != "english"){//언어 설정이 영어가 아닌 경우 번역기로 나라, 도시 번역
                        engKorTranslator.translate(address.countryName).addOnSuccessListener {
                            country.value =  it
                        }
                        engKorTranslator.translate(address.adminArea).addOnSuccessListener {
                            city.value =  it
                        }
                    }
                    else{
                        country.value = address.countryName
                        city.value = address.adminArea
                    }
                }
            }
        }
    } //현재 사용자 위치에 해당하는 국가와 도시를 가져와 텍스트를 바꾸는 함수

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
    }//Cloud Vision API에 보낼 사진을 규격에 맞게 조절
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
    }//Firebase에 사진 전달
    private fun updateUI(user: FirebaseUser?) {//Firebase 로그인한 사용자에 맞게 Ui 업데이트
    }
}

@Composable
//fun MainActivity(cityState: MutableState<String>, countryState: MutableState<String>, searchTxt: MutableState<String>, imgUri: MutableState<Uri>, isSearch: MutableState<Boolean>, languageSetting: MutableState<String>, modifier: Modifier = Modifier) {
fun MainActivity(cityState: MutableState<String>, countryState: MutableState<String>, searchTxt: MutableState<String>, imgUri: MutableState<Uri>, isSearch: MutableState<Boolean>, languageSetting: MutableState<String>, translator: Translator, openDialog: MutableState<Boolean>, modifier: Modifier = Modifier) {
    val contxt = LocalContext.current
    val config = LocalConfiguration.current
    val screenH = config.screenHeightDp.dp
    Column(modifier.fillMaxSize()) {
        Spacer(modifier = modifier.height(20.dp))
        UserLocaleTxt(cityState = cityState, countryState = countryState)
        Column (modifier = modifier.offset(y = screenH/4), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CameraBtn(searchTxt,imgUri, isSearch, contxt)
            SelectTranslation(languageSetting, cityState, countryState, searchTxt,translator)//
            IconButton(modifier = modifier
                .offset(y = (screenH / 9))
                .size(60.dp),
                onClick = {
                    openDialog.value = true
                }) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AIDialog", tint = Color(38,124,240,255), modifier = modifier.size(40.dp))
            }
        }

    }

}

@Composable
fun UserLocaleTxt(cityState: MutableState<String>, countryState: MutableState<String>, modifier: Modifier = Modifier){
    Row{
        Spacer(modifier = modifier.width(10.dp))
        Icon(imageVector = Icons.Filled.Place, contentDescription = "user_locale",
            modifier
                .size(40.dp)
                .offset(y = 10.dp), tint = LocalContentColor.current)
        Column (modifier = modifier.offset(x = 5.dp)) {
            Text(text = countryState.value, fontSize = 25.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = cityState.value, fontSize = 25.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)// 텍스트를 외부에서 바꾸기 위해선 mutableState의 Value를 외부에서 바꾸면, 컴포즈는 State를 받아 안에 있는 value를 받아 사용한다.
        }

    }
}

@Composable

fun CameraBtn(searchTxt: MutableState<String>,imgUri: MutableState<Uri>, isSearch: MutableState<Boolean>,contxt: Context,modifier: Modifier = Modifier){
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
    Column{
        Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            ElevatedButton(onClick = {
                camLauncher.launch(uri)
            }, modifier.size(200.dp),shape = RoundedCornerShape(50.dp), colors = btnColor, elevation = btnElevation) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "camera_btn",
                    modifier = modifier.size(200.dp),
                    tint = Color.Black
                )

            }
        }
        Row{
            Text(text=searchTxt.value,fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }

    }

}//
@Composable
fun SelectTranslation(languageSetting: MutableState<String>, cityState: MutableState<String>, countryState: MutableState<String>, searchTxt: MutableState<String>, translator: Translator, modifier: Modifier = Modifier){
    var expandState by remember { mutableStateOf(false) }
    val language = when (languageSetting.value) {
        "korean" -> "한국어"
        "english" -> "English"
        else -> {"Language"}
    }
    val langOptions = TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.KOREAN).setTargetLanguage(TranslateLanguage.ENGLISH).build()//영어 -> 한국어 번역옵션 설정
    val korEngTranslator = Translation.getClient(langOptions)//mlkit 번역기 생성
    val translatorCondition = DownloadConditions.Builder().requireWifi().build()// 번역 언어 다운로드 상태 체크 오브젝트
    korEngTranslator.downloadModelIfNeeded(translatorCondition).addOnFailureListener { Log.d("KOR2Eng", "Download Error") }//번역 언어 다운로드

    TextButton(onClick = { expandState = true }, shape = RoundedCornerShape(5.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
        modifier = modifier
            .width(150.dp)
            .offset(y = 15.dp)
            .border(2.dp, LocalContentColor.current, RectangleShape)) {
        Log.d("COLORRRRRRR", MaterialTheme.colorScheme.toString())
        Icon(imageVector = Icons.Default.Translate, contentDescription = "translate")
        Text(text = "  $language  ")
        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "open_menu")
    }
    Column (modifier.offset(x = (-50).dp,y=20.dp)) {
        DropdownMenu(expanded = expandState, onDismissRequest = { expandState = false },modifier.wrapContentSize()) {
            DropdownMenuItem(text = { Text("한국어") },
                onClick = {
                    languageSetting.value = "korean"
                    translator.translate(cityState.value).addOnSuccessListener { cityState.value = it }
                    translator.translate(countryState.value).addOnSuccessListener { countryState.value = it }
                    searchTxt.value = "장소 검색"
                    expandState = false
            })
            DropdownMenuItem(text = { Text("English") },
                onClick = {
                    languageSetting.value = "english"
                    korEngTranslator.translate(cityState.value).addOnSuccessListener { cityState.value = it }
                    korEngTranslator.translate(countryState.value).addOnSuccessListener { countryState.value = it }
                    searchTxt.value = "Search Place"
                    expandState = false })
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
} // 찍은 이미지를 Uri로 사용하기 위한 함수

@Composable
private fun CircularProgressBar(showProgress : MutableState<Boolean>, modifier: Modifier = Modifier) {
    val loading by remember { showProgress }

    if (!loading) return

    Column (modifier = modifier
        .fillMaxSize()
        .offset(y = (-28).dp)) {
        Row (modifier = modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            CircularProgressIndicator(
                modifier = Modifier.width(61.5.dp),
                color = Color.Yellow,
                strokeWidth = 10.7.dp,
            )
        }
    }

}// API로부터 결과값을 받아올 때까지 처리중을 표시할 원형 프로그래스 바
@Composable
private fun AIDialog(openDialog : MutableState<Boolean>, languageSetting: MutableState<String>, modifier: Modifier = Modifier){
    val dialogTxt = when(languageSetting.value)  {
       "korean" -> {
          "본 앱은 AI 기술을 활용하므로\n각종 오류가 생길 수 있습니다.\n장소 정보 및 검색은\n참고용으로만 활용하시길\n 권장합니다."
       }

       else ->{
          "This app utilizes AI.\nTherefore it may occur several errors.\nPlease use landmark information & search results as reference only."
       }
    }
    when {
        openDialog.value ->
            AlertDialog(
                modifier = modifier.fillMaxWidth(),
                icon = {Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "MainAIDialog", tint = Color.Gray, modifier = modifier.size(40.dp))},
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
private fun ErrorDialog(errorOccurred: MutableState<Boolean>, errorValue: MutableState<String>, languageSetting: MutableState<String>, modifier: Modifier = Modifier){
    var titleTxt = ""
    var errorTxt = ""

    when (languageSetting.value){
        "korean" -> {
            when (errorValue.value){
                "NOT_FOUND" -> {
                    titleTxt = "검색 결과 없음"
                    errorTxt = "검색된 장소가 없습니다."
                }
                "VISION_ERR" -> {
                    titleTxt = "장소 검색 오류"
                    errorTxt = "사진 검색 AI에서\n오류가 발생했습니다."
                }
                "MAP_ERR" -> {
                    titleTxt = "지도 오류"
                    errorTxt = "지도를 표현하던 중\n 오류가 발생했습니다."
                }
                "GEMINI_ERR" -> {
                    titleTxt = "장소 정보 오류"
                    errorTxt = "장소 정보 AI에서\n오류가 발생했습니다."
                }
            }
        }
        else -> {
            when (errorValue.value){
                "NOT_FOUND" -> {
                    titleTxt = "No Search Result"
                    errorTxt = "Can't find landmark\nfrom the picture"
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
            }
        }
    }

    when {
        errorOccurred.value -> {
            AlertDialog(
                icon = {Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = "ErrorDialog", tint = Color.Gray, modifier = modifier.size(40.dp))},
                title = { Text(text = titleTxt, textAlign = TextAlign.Center) },
                text = { Text(text = errorTxt, textAlign = TextAlign.Center, fontSize = 23.sp, lineHeight = 30.sp, letterSpacing = 1.sp) },
                onDismissRequest = { errorOccurred.value = false },
                confirmButton = {
                    TextButton(onClick = { errorOccurred.value = false }) {
                        Text(text = "OKAY", fontSize = 25.sp)
                    }
                })
            Log.d("AI Error", "AI ERROR OCCURRED")
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
   //val translator = Translation.getClient(TranslatorOptions.Builder().build())
   //val errorOccurred = mutableStateOf(true)
   //val errorValue = mutableStateOf("VISION_ERR")//NOT_FOUND, VISION_ERR, MAP_ERR, GEMINI_ERR
   TravelEyeTheme {
       //MainActivity(cityState,countryState,searchTxt,img,isSearch, languageSetting)
       CircularProgressBar(showProgress = mutableStateOf(true))
       //ErrorDialog(errorOccurred = errorOccurred, errorValue = errorValue, languageSetting = languageSetting)
       //AIDialog(openDialog = openDialog, languageSetting)
   }
}

