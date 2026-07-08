package mad.project.mdp_project

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SplashScreen {
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                finish()
            }
        }
    }
}

// ─── Color Palette ───
private val BrandPrimary = Color(0xFF004B4F)
private val BrandGreenSoft = Color(0xFF2E7D32)
private val SubtitleGray = Color(0xFF9E9E9E)

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    // Animation states
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.8f) }
    val textAlpha = remember { Animatable(0f) }
    val loaderAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Phase 1: Logo fade-in + scale (0 → 800ms)
        launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
        }
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
        }

        // Phase 2: Subtitle fade-in (after 400ms)
        delay(400)
        launch {
            textAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
        }

        // Phase 3: Loader fade-in (after 600ms more)
        delay(400)
        launch {
            loaderAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        }

        // Phase 4: Wait, then navigate (total ~2.5s)
        delay(1200)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ─── Logo ───
            Image(
                painter = painterResource(id = R.drawable.ic_logo_loading),
                contentDescription = "MindfulLife Logo",
                modifier = Modifier
                    .size(120.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ─── App Name ───
            Text(
                text = "MindfulLife",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = BrandPrimary,
                modifier = Modifier
                    .alpha(logoAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ─── Subtitle ───
            Text(
                text = "Your Wellness Companion",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = SubtitleGray,
                modifier = Modifier
                    .alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // ─── Loading Indicator ───
            CircularProgressIndicator(
                modifier = Modifier
                    .width(28.dp)
                    .alpha(loaderAlpha.value),
                color = BrandGreenSoft,
                strokeWidth = 3.dp,
                trackColor = Color(0xFFE8F5E9),
                strokeCap = StrokeCap.Round
            )
        }
    }
}
