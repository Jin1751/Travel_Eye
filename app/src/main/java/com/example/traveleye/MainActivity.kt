package com.example.traveleye

import android.Manifest
import android.app.Instrumentation.ActivityResult
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Bundle
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import com.example.traveleye.ui.theme.TravelEyeTheme
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val locationListener = LocationListener{  }
        val requestPermission = registerForActivityResult(
            ActivityResultContracts.RequestPermission()){
            when(it) {
                true -> { Toast.makeText(this,"권한 허가",Toast.LENGTH_SHORT).show()}
                false -> {
                    Toast.makeText(this,"권한 거부",Toast.LENGTH_SHORT).show()
                }
            }
        }//위치 권한이 거부 상태일 경우 토스트 메시지 출력 후 종료
        Log.d("LOCATION","START")
        requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        Log.d("LOCATION", "END")
        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,0f, locationListener)
        setContent {
            TravelEyeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                }
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
    }

    override fun onStart() {
        super.onStart()

    }

    override fun onResume() {
        super.onResume()

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

}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val gpsIcon : ImageBitmap = ImageBitmap.imageResource(id = R.drawable.pngegg)
    Row {
        Image(bitmap = gpsIcon, contentDescription = "Gps_Icon", modifier.width(30.dp))
        Text(text = "도시, ")
        Text(text = "나라")
        modifier.padding(vertical = 10.dp)
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TravelEyeTheme {
        Greeting("Android")
    }
}