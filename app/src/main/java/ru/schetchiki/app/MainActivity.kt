package ru.schetchiki.app


import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

private val Orange = Color(0xFFB85A1C)
private val OrangeLight = Color(0xFFE58A48)
private val BgTop = Color(0xFF101216)
private val BgBottom = Color(0xFF21170F)
private val Card = Color(0xFF1B1D22)
private val Muted = Color(0xFF9B9BA2)


class MainActivity : ComponentActivity() {
    companion object { const val ACTION_SMS_RESULT = "ru.schetchiki.app.SMS_RESULT" }
    private var smsListener: ((String) -> Unit)? = null
    private val smsResultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { smsListener?.invoke(intent?.getStringExtra("status") ?: "failed") }
    }
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pendingSend?.invoke() else Toast.makeText(this, "Разрешение на SMS не предоставлено", Toast.LENGTH_LONG).show()
        pendingSend = null
    }
    private var pendingSend: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        ContextCompat.registerReceiver(this, smsResultReceiver, IntentFilter(ACTION_SMS_RESULT), ContextCompat.RECEIVER_NOT_EXPORTED)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = OrangeLight, background = BgTop)) {
                AppUI(
                    onSendSms = { number, message, result ->
                        smsListener = result
                        val sendAction = {
                            try {
                                val pi = PendingIntent.getBroadcast(this, 0, Intent(this, SmsSentReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                                val manager = getSystemService(SmsManager::class.java)
                                manager.sendTextMessage(number, null, message, pi, null)
                            } catch (e: Exception) {
                                result("failed")
                                Toast.makeText(this, "Не удалось отправить SMS: ${e.localizedMessage ?: "ошибка"}", Toast.LENGTH_LONG).show()
                            }
                        }
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) sendAction()
                        else { pendingSend = sendAction; permissionLauncher.launch(Manifest.permission.SEND_SMS) }
                    }
                )
            }
        }
    }
    override fun onDestroy() { unregisterReceiver(smsResultReceiver); super.onDestroy() }
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
            Image(painter = painterResource(R.drawable.splash_art), contentDescription = "Заставка СЧЕТЧИКИ", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
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
                            if (status == "sent") page = 2 else { sendError = "SMS не удалось отправить. Проверьте номер, сеть и баланс."; page = 3 }
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
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
        Spacer(Modifier.height(8.dp))
        OrangeButton("ПРОДОЛЖИТЬ", onContinue, Modifier.fillMaxWidth().height(54.dp))
    }
}


@Composable
private fun PhonePage(phone: String, onPhone: (String) -> Unit, onBack: () -> Unit, onSend: () -> Unit) {
    Column(Modifier.fillMaxSize().imePadding().padding(22.dp), horizontalAlignment = Alignment.Center, verticalArrangement = Arrangement.Center) {
        Text("СЧЕТЧИКИ", color = OrangeLight, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(10.dp)); Text("ОТПРАВКА SMS", color = Muted, fontSize = 12.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(34.dp)); Text("Номер получателя", color = Color(0xFFE1E1E3), fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(phone, onPhone, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("+7XXXXXXXXXX", color = Color(0xFF737780)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), shape = RoundedCornerShape(14.dp), colors = orangeFieldColors())
        Spacer(Modifier.height(24.dp)); OrangeButton("ОТПРАВИТЬ SMS", onSend, Modifier.fillMaxWidth().height(54.dp))
        Spacer(Modifier.height(14.dp)); OutlinedOrangeButton("НАЗАД", onBack, Modifier.fillMaxWidth().height(52.dp))
    }
}


@Composable
private fun SuccessPage(onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(116.dp).border(2.dp, Orange, CircleShape).background(Color(0xFF201B17), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, contentDescription = "Успешно", tint = OrangeLight, modifier = Modifier.size(64.dp))
        }
        Spacer(Modifier.height(26.dp)); Text("УСПЕШНО!", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp)); Text("SMS отправлено", color = Muted, fontSize = 16.sp)
        Spacer(Modifier.height(56.dp)); OrangeButton("ГОТОВО", onDone, Modifier.fillMaxWidth().height(54.dp))
    }
}


@Composable
private fun ErrorPage(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("ОШИБКА", color = OrangeLight, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp)); Text(message, color = Muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp)); OrangeButton("ПОВТОРИТЬ", onRetry, Modifier.fillMaxWidth().height(54.dp))
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
