package com.sinjartv.rstv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ScreenBlack = Color(0xFF080808)
private val AccentRed = Color(0xFFD71920)
private val SoftWhite = Color(0xFFF4F4F4)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                RSTVHome()
            }
        }
    }
}

@Composable
private fun RSTVHome() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScreenBlack,
        contentColor = SoftWhite
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("RS", color = AccentRed, fontSize = 48.sp, fontWeight = FontWeight.Black)
                Text(" TV", color = SoftWhite, fontSize = 48.sp, fontWeight = FontWeight.Black)
            }
            Text(
                text = "SINJAR TV SHOW",
                color = SoftWhite,
                fontSize = 14.sp,
                letterSpacing = 4.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "مرحباً بك في RS TV",
                color = SoftWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 36.dp)
            )
            Text(
                text = "يجري تجهيز مكتبة القنوات المجانية المصرّح بها.",
                color = Color.LightGray,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                text = "واجهة تأسيسية — تشغيل القنوات سيضاف بعد مراجعة المصادر.",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 18.dp)
            )
        }
    }
}
