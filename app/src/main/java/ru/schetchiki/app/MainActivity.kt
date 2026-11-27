package ru.schetchiki.app


import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Orange = Color(0xFFB85A1C)
private val OrangeLight = Color(0xFFE58A48)
private val BgTop = Color(0xFF101216)
private val BgBottom = Color(0xFF21170F)
private val Card = Color(0xFF1B1D22)
private val Muted = Color(0xFF9B9BA2)


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
        controller.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = OrangeLight, background = BgTop)) {
                AppUI(onSendSms = { number, message, result ->
                    try {
                        val intent = Intent(Intent.ACTION_SENDTO, android.net.Uri.fromParts("smsto", number, null))
                        intent.putExtra("sms_body", message)
                        startActivity(intent)
                        result("prepared")
                    } catch (e: android.content.ActivityNotFoundException) {
                        result("failed")
                    } catch (e: SecurityException) {
                        result("failed")
                    }
                })
            }
        }
    }
}

@Composable
private fun AppUI(onSendSms: (String, String, (String) -> Unit) -> Unit) {
    var page by remember { mutableStateOf(0) } // 0 input, 1 phone, 2 success, 3 error
    var splash by remember { mutableStateOf(true) }
    var phone by remember { mutableStateOf("") }
    var sendError by remember { mutableStateOf("") }
    val labels = listOf("ХВ", "ГВ", "ХК", "ГК", "ЭЛ")
    val vals = remember { mutableStateListOf("", "", "", "", "") }
    LaunchedEffect(Unit) { delay(1700); splash = false }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BgTop, Color(0xFF18191D), BgBottom)))) {
        if (splash) {
            Column(Modifier.fillMaxSize().statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Image(painterResource(R.drawable.house_logo), "Логотип", Modifier.size(240.dp), contentScale = ContentScale.Fit)
                Spacer(Modifier.height(24.dp))
                Text("СЧЕТЧИКИ", color = OrangeLight, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
        } else {
            AnimatedVisibility(visible = page == 0) {
                InputPage(labels, vals, onContinue = { page = 1 })
            }
            AnimatedVisibility(visible = page == 1) {
                PhonePage(phone, { phone = it }, onBack = { page = 0 }, onSend = {
                    if (phone.trim().length < 7 || !phone.trim().matches(Regex("^[+0-9() -]+$"))) {
                        sendError = "Проверьте номер телефона"; page = 3
                    } else {
                        val msg = labels.zip(vals).joinToString("\n") { (k,v) -> "$k: ${v.trim()}" }
                        onSendSms(phone.trim(), msg) { status ->
                            if (status == "prepared") page = 2 else { sendError = "Не удалось открыть приложение SMS. Проверьте, установлено ли оно."; page = 3 }
                        }
                    }
                })
            }
            AnimatedVisibility(visible = page == 2) { SuccessPage(onDone = { vals.indices.forEach { vals[it] = "" }; phone = "" ; page = 0 }) }
            AnimatedVisibility(visible = page == 3) { ErrorPage(sendError, onRetry = { page = 1 }, onBack = { page = 0 }) }
        }
    }
}


@Composable
private fun InputPage(labels: List<String>, values: MutableList<String>, onContinue: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 22.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(12.dp))
        Text("СЧЕТЧИКИ", color = OrangeLight, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("ВВОД ДАННЫХ", color = Muted, fontSize = 12.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(22.dp))
        labels.forEachIndexed { index, label ->
            Column(Modifier.fillMaxWidth().padding(bottom = 13.dp)) {
                Text(label, color = Color(0xFFE1E1E3), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = values[index], onValueChange = { input -> values[index] = input.filter { it.isDigit() || it == '.' || it == ',' }.take(12) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Введите число", color = Color(0xFF737780)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp), colors = orangeFieldColors()
                )
            }
        }
        }
        Spacer(Modifier.height(8.dp))
        OrangeButton("ПРОДОЛЖИТЬ", onContinue, Modifier.fillMaxWidth().height(54.dp))
    }
}


@Composable
private fun PhonePage(phone: String, onPhone: (String) -> Unit, onBack: () -> Unit, onSend: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("СЧЕТЧИКИ", color = OrangeLight, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(10.dp)); Text("ОТПРАВКА SMS", color = Muted, fontSize = 12.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(34.dp)); Text("Номер получателя", color = Color(0xFFE1E1E3), fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(phone, onPhone, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("+7XXXXXXXXXX", color = Color(0xFF737780)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), shape = RoundedCornerShape(14.dp), colors = orangeFieldColors())
        }
        Spacer(Modifier.height(24.dp)); OrangeButton("ОТПРАВИТЬ SMS", onSend, Modifier.fillMaxWidth().height(54.dp))
        Spacer(Modifier.height(14.dp)); OutlinedOrangeButton("НАЗАД", onBack, Modifier.fillMaxWidth().height(52.dp))
    }
}


@Composable
private fun SuccessPage(onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(116.dp).border(2.dp, Orange, CircleShape).background(Color(0xFF201B17), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, contentDescription = "Успешно", tint = OrangeLight, modifier = Modifier.size(64.dp))
        }
        Spacer(Modifier.height(26.dp)); Text("ГОТОВО К ОТПРАВКЕ", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp)); Text("SMS подготовлено к отправке", color = Muted, fontSize = 16.sp)
        }
        Spacer(Modifier.height(16.dp)); OrangeButton("ГОТОВО", onDone, Modifier.fillMaxWidth().height(54.dp))
    }
}


@Composable
private fun ErrorPage(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("ОШИБКА", color = OrangeLight, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp)); Text(message, color = Muted, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp)); OrangeButton("ПОВТОРИТЬ", onRetry, Modifier.fillMaxWidth().height(54.dp))
        Spacer(Modifier.height(12.dp)); OutlinedOrangeButton("НА ГЛАВНЫЙ", onBack, Modifier.fillMaxWidth().height(52.dp))
    }
}


@Composable
private fun orangeFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = OrangeLight, unfocusedBorderColor = Orange.copy(alpha = .9f),
    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
    cursorColor = OrangeLight, focusedContainerColor = Card, unfocusedContainerColor = Card
)


@Composable
private fun OrangeButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick, modifier, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF211B17), contentColor = OrangeLight), border = androidx.compose.foundation.BorderStroke(1.dp, Orange)) {
        Text(text, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
    }
}

@Composable
private fun OutlinedOrangeButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick, modifier, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Muted), border = androidx.compose.foundation.BorderStroke(1.dp, Orange)) { Text(text, letterSpacing = 1.sp) }
}
