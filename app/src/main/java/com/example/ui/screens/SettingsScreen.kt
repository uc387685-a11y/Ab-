package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.actions.DeviceControlHelper
import com.example.core.AssistantLanguage
import com.example.core.GreetingType
import com.example.core.PreferencesManager
import com.example.service.WakeWordService
import com.example.ui.theme.AssistantAccentGlow
import com.example.ui.theme.MicActiveRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.UmeshPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferencesManager: PreferencesManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs by preferencesManager.preferencesFlow.collectAsState(initial = null)
    val deviceControlHelper = remember { DeviceControlHelper(context) }

    // Multi-permission request launcher
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        Toast.makeText(context, "अनुमतियों की स्थिति अपडेट हुई (Permissions updated)", Toast.LENGTH_SHORT).show()
    }

    val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val hasContacts = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    val hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else true

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "सेटिंग्स (Settings)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Hello Umesh AI प्राथमिकताएं और Redmi अनुकूलन",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Language Preference
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = UmeshPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "प्राथमिक भाषा (Primary Language):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    AssistantLanguage.entries.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { preferencesManager.setLanguage(lang) }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = prefs?.language == lang,
                                onClick = { scope.launch { preferencesManager.setLanguage(lang) } }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(lang.displayName, fontSize = 14.sp)
                        }
                    }
                }
            }

            // 2. Greeting Behavior Settings
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "वेक वर्ड अभिवादन (Wake Word Greeting):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = UmeshPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "'Hello Umesh' बोलने पर असिस्टेंट का पहला जवाब चुनें:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    GreetingType.entries.forEach { greeting ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { preferencesManager.setGreetingType(greeting) }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = prefs?.greetingType == greeting,
                                onClick = { scope.launch { preferencesManager.setGreetingType(greeting) } }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(greeting.labelHi, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                if (greeting.defaultSpeechHi.isNotBlank()) {
                                    Text(
                                        text = "\"${greeting.defaultSpeechHi}\"",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Wake Word & Screen-Off Listener Toggle
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "वेक-वर्ड सुनना (Wake Word Listening)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "'Hello Maayra' या 'Hello Umesh' पर एक्टिवेट होगा",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = prefs?.wakeWordEnabled == true,
                            onCheckedChange = { isChecked ->
                                scope.launch {
                                    preferencesManager.setWakeWordEnabled(isChecked)
                                    if (isChecked) {
                                        WakeWordService.start(context)
                                    } else {
                                        WakeWordService.stop(context)
                                    }
                                }
                            },
                            modifier = Modifier.testTag("wake_word_switch")
                        )
                    }
                }
            }

            // 4. Redmi / Xiaomi / MIUI / HyperOS Optimization Guide
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BatteryAlert, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Redmi फ़ोन बैकग्राउंड गाइड (Redmi Optimization)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Redmi और Xiaomi फ़ोन (MIUI / HyperOS) बैटरी बचाने के लिए बैकग्राउंड ऐप्स को रोक देते हैं। स्क्रीन-ऑफ़ वेक वर्ड के सुचारू कार्य के लिए ये दो सेटिंग्स करें:",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "1. ऑटोस्टार्ट (Autostart) चालू करें\n2. बैटरी सेवर (Battery Saver) को 'No restrictions' पर सेट करें",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { deviceControlHelper.openRedmiBatterySettings() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ऐप सेटिंग्स (Autostart)", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { deviceControlHelper.openBatteryOptimizationSettings() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("बैटरी प्रतिबंध हटाएँ", fontSize = 11.sp)
                        }
                    }
                }
            }

            // 5. System Permissions Status
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "अनुमतियों की स्थिति (Permission Status):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = UmeshPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    PermissionRow("माइक्रोफ़ोन (आवाज़ व वेक वर्ड)", hasMic)
                    PermissionRow("संपर्क (फ़ोन लगाने के लिए)", hasContacts)
                    PermissionRow("स्थान (मैप्स व मौसम)", hasLocation)
                    PermissionRow("सूचनाएं (बैकग्राउंड लिसनिंग)", hasNotification)

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val perms = mutableListOf(
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.READ_CONTACTS,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                perms.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionsLauncher.launch(perms.toTypedArray())
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("सभी आवश्यक अनुमतियाँ दें (Grant Permissions)")
                    }
                }
            }

            // 6. Security & Privacy Disclosure
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = SuccessGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "सुरक्षा और गोपनीयता (Security & Privacy)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• कोई भी गोपनीय API की APK में हार्डकोड नहीं है।\n• बातचीत का इतिहास केवल आपके डिवाइस पर सुरक्षित रूप से सहेजा जाता है।\n• संपर्क व कॉल केवल आपकी स्पष्ट पुष्टि के बाद ही किए जाते हैं।\n• किसी भी गुप्त ऑडियो रिकॉर्डिंग या ट्रैकिंग की अनुमति नहीं है।",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(title: String, isGranted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) SuccessGreen else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isGranted) "स्वीकृत" else "अस्वीकृत",
                fontSize = 12.sp,
                color = if (isGranted) SuccessGreen else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
