package co.edu.uniautonoma.inclusivereadingar.presentation.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SessionSummaryScreen(categoryName: String, cardsCount: Int, onFinish: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF8F7)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "Sesión completada", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = Color(0xFFE53734))
                Text(text = categoryName, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(
                    text = "Hoy viste $cardsCount palabras en esta sesión.",
                    textAlign = TextAlign.Center,
                    color = Color(0xFF475569),
                    fontSize = 18.sp
                )
                Text(
                    text = "Puedes volver a tus temas o continuar con otra práctica después.",
                    textAlign = TextAlign.Center,
                    color = Color(0xFF64748B)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().height(56.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE53734),
                contentColor = Color.White
            )
        ) {
            Text(text = "Volver a temas", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}
