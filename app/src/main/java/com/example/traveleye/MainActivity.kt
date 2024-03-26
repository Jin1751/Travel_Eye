package com.example.traveleye

import android.Manifest
import android.app.Instrumentation.ActivityResult
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Geocoder.GeocodeListener
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.util.Consumer
import com.example.traveleye.ui.theme.TravelEyeTheme
import java.io.IOException
import java.util.Locale
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    private lateinit var locationManager : LocationManager
    private lateinit var locationListener : LocationListener
    private var lat : Double = 0.0
    private var lon : Double = 0.0
    var country = mutableStateOf("나라")
    var city = mutableStateOf("도시")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationListener = LocationListener {
            lat = it.latitude
            lon = it.longitude
            getAddress()

            Log.d("LOCATION11", "LAT: " + lat + ", LON: " + lon)
            Log.d("LOCALE", country.value + ", " + city.value)
            }

        setContent {
            TravelEyeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainActivity(country, city)
                }
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
    }

    override fun onStart() {
        super.onStart()
        perMissionCheck()
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdate()
        Log.d("LOCATION22", "LAT: " + lat + ", LON: " + lon)
    }

    override fun onPause(){
        super.onPause()

    }

    override fun onStop() {
        super.onStop()

    }

    override fun onDestroy() {
        super.onDestroy()

    }

    private fun perMissionCheck(){
        val requestPermission = registerForActivityResult(
            ActivityResultContracts.RequestPermission())
        {
            if (it == false) {
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                Toast.makeText(this, "위치 권한을 허용해주세요", Toast.LENGTH_SHORT).show()
                ActivityCompat.finishAffinity(this)
                System.exit(0)
            }
        } //위치 권한이 거부 상태일 경우 토스트 메시지 출력 후 종료
        requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

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
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 300000,0f, locationListener, Looper.getMainLooper())

    }
    private fun stopLocationUpdate(){

    }

    private fun getAddress(){
        val geocoder = Geocoder(this, Locale.getDefault())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            val geoListener : GeocodeListener = GeocodeListener {  }
            var addr = geocoder.getFromLocation(lat,lon,1,geoListener)
            if (addr != null){
                //val address = addr.
            }
        }
        else{
            var addr = geocoder.getFromLocation(lat,lon,1)
            if (addr != null) {
                if (addr.isNotEmpty()){
                    val address = addr[0]
                    Log.d("QWER", address.toString())
                    country.value = address.countryName
                    city.value = address.adminArea
                }
            }
        }

    }
}

@Composable
fun MainActivity(cityState: MutableState<String>, countryState: MutableState<String>, modifier: Modifier = Modifier) {
    val gpsIcon : ImageBitmap = ImageBitmap.imageResource(id = R.drawable.pngegg)

    Row(modifier.padding(horizontal = 10.dp, vertical = 30.dp)) {
        Image(bitmap = gpsIcon, contentDescription = "Gps_Icon", modifier.width(30.dp))
        Text(text = (cityState.value + ", "), fontSize = 25.sp)// 텍스트를 외부에서 바꾸기 위해선 mutableState의 Value를 외부에서 바꾸면, 컴포즈는 State를 받아 안에 있는 value를 받아 사용한다.
        Text(text = countryState.value, fontSize = 25.sp)
        modifier.padding(vertical = 10.dp)
    }
}

//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    var country = mutableStateOf("나라")
//    var city = mutableStateOf("도시")
//    TravelEyeTheme {
//        MainActivity("나라","도시")
//    }
//}