package com.example.traveleye

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.parcelize.Parcelize
import com.example.traveleye.ui.theme.TravelEyeTheme
@Parcelize
data class NearLandmark(val name: String, val score: Double) : Parcelable

class SuccessLandmark : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val landmarkIntent = intent
        val totalLandmark = landmarkIntent.getIntExtra("totalLandmark",0)
        for (i in 1..totalLandmark){
           val lm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                landmarkIntent.getParcelableExtra("landMark$i",NearLandmark::class.java)
            } else{
                landmarkIntent.getParcelableExtra("landMark$i")
            }
            if (lm != null) {
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
                    MapView("Android")
                }
            }
        }
    }
}

@Composable
fun MapView(name: String, modifier: Modifier = Modifier) {
    Text(text = name)
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun placeInfo(){
    ModalBottomSheetLayout(sheetContent = @Composable {}) {

    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TravelEyeTheme {
        MapView("Android")
    }
}