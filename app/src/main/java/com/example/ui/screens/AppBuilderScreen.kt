package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiAppBuilder
import com.example.ai.GeneratedAppProject
import com.example.ui.theme.AssistantAccentGlow
import com.example.ui.theme.CardHighlight
import com.example.ui.theme.UmeshPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBuilderScreen(
    appBuilder: GeminiAppBuilder,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var appIdeaPrompt by remember {
        mutableStateOf("Ek simple calculator Android app banao jisme Material 3, clean UI aur history list ho.")
    }

    var isBuilding by remember { mutableStateOf(false) }
    var projectResult by remember { mutableStateOf<GeneratedAppProject?>(null) }
    var selectedFileTab by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val sampleIdeas = listOf(
        "Calculator App (कैलकुलेटर)",
        "Notes & Todo (नोट्स व कार्य सूची)",
        "Expense Tracker (खर्च ट्रैकर)",
        "Weather Forecast (मौसम ऐप)"
    )

    val fileTabs = listOf("MainActivity.kt", "build.gradle.kts", "AndroidManifest.xml", "Instructions")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI App Builder",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Android ऐप्स का पूरा कोड बनाएँ (Kotlin + Compose)",
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
            // Description input card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "कैसा Android ऐप बनाना चाहते हैं? (Describe App Idea):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = appIdeaPrompt,
                        onValueChange = { appIdeaPrompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("app_builder_input"),
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "त्वरित सुझाव (Quick ideas):",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sampleIdeas.forEach { idea ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    appIdeaPrompt = "Ek $idea banao with modern Kotlin Jetpack Compose and clean Material 3 UI."
                                },
                                label = { Text(idea, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (appIdeaPrompt.isBlank()) return@Button
                            scope.launch {
                                isBuilding = true
                                errorMessage = null
                                val result = appBuilder.generateApp(appIdeaPrompt)
                                isBuilding = false
                                if (result.isSuccess) {
                                    projectResult = result.getOrNull()
                                    selectedFileTab = 0
                                } else {
                                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "ऐप जनरेशन विफल रहा।"
                                }
                            }
                        },
                        enabled = !isBuilding,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("build_app_button")
                    ) {
                        if (isBuilding) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("पूरा प्रोजेक्ट जनरेट हो रहा है...", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.Build, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ऐप कोड बनाएँ (Generate App Code)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Error Card
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "त्रुटि: $errorMessage",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Generated Code Tabs & Viewer
            if (projectResult != null) {
                val project = projectResult!!
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = project.appTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = UmeshPrimary
                                )
                                Text(
                                    text = "पूर्ण Android Studio प्रोजेक्ट",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            IconButton(
                                onClick = {
                                    val currentContent = when (selectedFileTab) {
                                        0 -> project.mainActivityCode
                                        1 -> project.gradleDependencies
                                        2 -> project.manifestCode
                                        else -> project.buildAndRunInstructions
                                    }
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Hello Umesh Code", currentContent))
                                    Toast.makeText(context, "${fileTabs[selectedFileTab]} कॉपी किया गया!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("copy_code_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy code")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Tab Row
                        TabRow(
                            selectedTabIndex = selectedFileTab,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            fileTabs.forEachIndexed { index, tabTitle ->
                                Tab(
                                    selected = selectedFileTab == index,
                                    onClick = { selectedFileTab = index },
                                    text = { Text(tabTitle, fontSize = 11.sp, maxLines = 1) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Code Container Box
                        val codeToShow = when (selectedFileTab) {
                            0 -> project.mainActivityCode
                            1 -> project.gradleDependencies
                            2 -> project.manifestCode
                            else -> project.buildAndRunInstructions
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .padding(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = codeToShow,
                                    color = Color(0xFFE2E8F0),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
