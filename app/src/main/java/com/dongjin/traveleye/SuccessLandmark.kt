package com.dongjin.traveleye

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import android.graphics.Color as Colorg
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.parcelize.Parcelize
import com.dongjin.traveleye.ui.theme.TravelEyeTheme
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.model.AdvancedMarkerOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapCapabilities
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.PinConfig
import com.google.maps.android.compose.AdvancedMarker
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberMarkerState

@Parcelize
data class NearLandmark(val name: String, val score: Double, val locate : Location) : Parcelable

class SuccessLandmark : ComponentActivity() {
    private lateinit var locationIntent : Intent
    private lateinit var userLocation :Location
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationIntent = intent
        userLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            locationIntent.getParcelableExtra("userLocation",Location::class.java)!!
        }else{
            locationIntent.getParcelableExtra("userLocation")!!
        }
        Log.d("SUCCESS USERLOC", userLocation.latitude.toString() + ", " + userLocation.longitude)
        val totalLandmark = locationIntent.getIntExtra("totalLandmark",0)
        val landmarkArray : MutableList<NearLandmark> = mutableListOf()
        for (i in 1..totalLandmark){
           val lm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
               locationIntent.getParcelableExtra("landMark$i",NearLandmark::class.java)
            } else{
               locationIntent.getParcelableExtra("landMark$i")
            }
            if (lm != null) {
                landmarkArray.add(lm)
                Log.d("LandMark$i", lm.name + ", " + lm.score)
            }
            else{
                Log.d("LandMark$i", "NULL")
            }
        }

       setContent {
                TravelEyeTheme {
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MapView(landmarkArray, userLocation)
                    }
                }

           }
       }
}

@Composable
fun MapView(landmarkArray: MutableList<NearLandmark>, userLocation: Location, modifier: Modifier = Modifier) {
    val contxt = LocalContext.current
    val mapCamera = CameraPosition.fromLatLngZoom(LatLng(userLocation.latitude,userLocation.longitude),15.0f)
    var placeInfoShow = false
    var placeName = ""
    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = modifier.fillMaxSize(),
            cameraPositionState = CameraPositionState(mapCamera),
            googleMapOptionsFactory = { GoogleMapOptions().mapId(BuildConfig.MAP_ID) }) {
            val pinConfig = PinConfig.builder()
                .setBackgroundColor(Colorg.RED)
                .setBorderColor(Colorg.WHITE)
                .build()
            landmarkArray.forEach { nl ->
                val ll = LatLng(nl.locate.latitude, nl.locate.longitude)
                AdvancedMarker(
                    state = MarkerState(ll),
                    title = nl.name,
                    snippet = "Marker ${nl.name}",
                    pinConfig = pinConfig,
                    onClick = { it ->
                        Log.d("MARKER", "CLICK")
                        placeName = nl.name
                        placeInfoShow = true
                        false
                    })
            }

            val userLoc = LatLng(userLocation.latitude, userLocation.longitude)
            val uGlyph = PinConfig.Glyph(BitmapDescriptorFactory.fromResource(R.drawable.ic_my_location))
            val uPinConfig =PinConfig.builder()
                            .setBackgroundColor(Colorg.BLUE)
                            .setBorderColor(Colorg.WHITE)
                            .setGlyph(uGlyph)
                            .build()
            Log.d("UPIN", ((uPinConfig).toString()))
            AdvancedMarker(
                state = MarkerState(userLoc),
                title = "User",
                snippet = "User Location",
                pinConfig = uPinConfig
            )

            //placeInfo(placeName = placeName, showInfo = placeInfoShow, context = contxt)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun placeInfo(placeName : String, showInfo : Boolean, context: Context, modifier: Modifier = Modifier){
    ModalBottomSheetLayout(
        sheetState = rememberModalBottomSheetState(initialValue = if(showInfo) {ModalBottomSheetValue.Hidden} else {ModalBottomSheetValue.Expanded}
        ), sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        sheetContent = @Composable {
        Column {
            Text("${placeName}", color = Color.Black, fontSize = 25.sp)
            Button(onClick = { Toast.makeText(context, "AI 검색 토스트", Toast.LENGTH_LONG).show()})
            {
                Text("AI 설명 보기")
            }
        }
        }) {

    }
}

//@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    val loc = Location("TEST")
    loc.latitude = 52.9715487
    loc.longitude = -9.4411821
    val lm = NearLandmark("TEST LOC", 0.5, loc)
    val arr : MutableList<NearLandmark> = mutableListOf(lm)
    var user = Location("TEST")
    user.latitude = 52.9715
    user.longitude = -9.4411821
    TravelEyeTheme {
        MapView(arr,user)
    }
}