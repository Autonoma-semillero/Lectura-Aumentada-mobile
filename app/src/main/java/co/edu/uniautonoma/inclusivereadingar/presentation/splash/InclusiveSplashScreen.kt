package co.edu.uniautonoma.inclusivereadingar.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Toys
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InclusiveSplashScreen(
    progress: Float,
    loadingText: String
) {
    val normalizedProgress = progress.coerceIn(0f, 1f)
    val primary = Color(0xFFE53734)
    val bg = Color(0xFFF8F6F6)
    val darkText = Color(0xFF091331)
    val softText = Color(0xFF4C5A72)

    val activeDot = when {
        normalizedProgress < 0.34f -> 0
        normalizedProgress < 0.67f -> 1
        else -> 2
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        Icon(
            imageVector = Icons.Rounded.SentimentVerySatisfied,
            contentDescription = null,
            tint = Color.Black.copy(alpha = 0.04f),
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.TopStart)
                .offset(x = 40.dp, y = 72.dp)
        )

        Icon(
            imageVector = Icons.Rounded.Toys,
            contentDescription = null,
            tint = Color.Black.copy(alpha = 0.04f),
            modifier = Modifier
                .size(96.dp)
                .align(Alignment.BottomEnd)
                .offset(x = (-24).dp, y = (-190).dp)
        )

        Icon(
            imageVector = Icons.Rounded.Extension,
            contentDescription = null,
            tint = Color.Black.copy(alpha = 0.04f),
            modifier = Modifier
                .size(84.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-18).dp, y = (-220).dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(34.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Star, null, tint = primary, modifier = Modifier.size(52.dp))
                Icon(Icons.Rounded.Star, null, tint = primary, modifier = Modifier.size(36.dp))
                Icon(Icons.Rounded.Star, null, tint = primary, modifier = Modifier.size(52.dp))
            }

            Spacer(modifier = Modifier.height(56.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(320.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .clip(CircleShape)
                        .background(primary.copy(alpha = 0.12f))
                        .border(4.dp, primary, CircleShape)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SentimentVerySatisfied,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(72.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Surface(
                        color = primary,
                        shape = RoundedCornerShape(40.dp),
                        shadowElevation = 8.dp
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .padding(horizontal = 30.dp, vertical = 12.dp)
                                .size(34.dp)
                        )
                    }
                }

                Surface(
                    color = Color.White,
                    shape = CircleShape,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 14.dp, y = (-72).dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoStories,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(30.dp)
                    )
                }
            }

            Text(
                text = "Lectura Inclusiva",
                style = TextStyle(
                    fontSize = 52.sp,
                    lineHeight = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = darkText
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "¡Bienvenidos a tu aventura!",
                style = TextStyle(
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = softText
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = loadingText,
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = primary.copy(alpha = 0.8f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { normalizedProgress },
                color = primary,
                trackColor = primary.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(999.dp))
            )

            Spacer(modifier = Modifier.height(28.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Dot(color = if (activeDot == 0) primary else primary.copy(alpha = 0.35f))
                Dot(color = if (activeDot == 1) primary else primary.copy(alpha = 0.35f))
                Dot(color = if (activeDot == 2) primary else primary.copy(alpha = 0.35f))
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(color)
    )
}
