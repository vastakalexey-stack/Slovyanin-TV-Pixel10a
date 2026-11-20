package ru.pixel10a.slovyanintv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(7, 26, 46)
        window.navigationBarColor = android.graphics.Color.rgb(7, 26, 46)

        setContent {
            val night = Color(0xFF071A2E)
            val parchment = Color(0xFFE5F4FF)
            val sky = Color(0xFF8CCBFF)
            MaterialTheme(colorScheme = darkColorScheme(
                background = night, surface = Color(0xFF102B49), primary = sky
            )) {
                Box(
                    modifier = Modifier.fillMaxSize().background(night),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp)
                            .border(1.dp, sky.copy(alpha = 0.65f), RoundedCornerShape(22.dp))
                            .padding(vertical = 54.dp, horizontal = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("ᚱ   ᚷ   ᚱ", color = sky, fontSize = 30.sp, fontFamily = FontFamily.Serif)
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "Галине",
                            color = parchment,
                            fontSize = 43.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 5.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(22.dp))
                        Box(Modifier.width(118.dp).height(1.dp).background(sky))
                        Spacer(Modifier.height(18.dp))
                        Text(
                            "СЛАВЯНИН ТВ",
                            color = sky,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        )
                        Spacer(Modifier.height(24.dp))
                        Text("ᚱ   ᛉ   ᚱ", color = sky, fontSize = 25.sp, fontFamily = FontFamily.Serif)
                    }
                }
            }

            LaunchedEffect(Unit) {
                delay(2_000)
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish()
            }
        }
    }
}
