package com.niagaraclone.launcher.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.ui.LauncherViewModel

data class ContactResultModel(
    val id: String,
    val name: String,
    val phoneNumber: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSearchOverlay(
    viewModel: LauncherViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val apps by viewModel.apps.collectAsState()
    val searchContactsEnabled by viewModel.repository.searchContactsEnabled.collectAsState()
    val accentColorArgb by viewModel.accentColorArgb.collectAsState()
    val accentColor = Color(accentColorArgb)

    var matchedContacts by remember { mutableStateOf<List<ContactResultModel>>(emptyList()) }

    // Query contacts dynamically when typing if enabled
    LaunchedEffect(query, searchContactsEnabled) {
        if (!searchContactsEnabled || query.isBlank() || query.length < 2) {
            matchedContacts = emptyList()
            return@LaunchedEffect
        }
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return@LaunchedEffect

        val contactsList = mutableListOf<ContactResultModel>()
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone._ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%${query.trim()}%"),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )
            cursor?.use {
                val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext() && contactsList.size < 4) {
                    contactsList.add(
                        ContactResultModel(
                            id = it.getString(idIdx) ?: "",
                            name = it.getString(nameIdx) ?: "Contact",
                            phoneNumber = it.getString(numIdx) ?: ""
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        matchedContacts = contactsList
    }

    // Prioritize apps starting with query, then containing query
    val filteredApps = remember(query, apps) {
        if (query.isBlank()) {
            apps.take(5) // Show frequent/recent apps when query is empty
        } else {
            val trimmed = query.trim()
            apps.sortedWith(
                compareByDescending<AppModel> { it.label.startsWith(trimmed, ignoreCase = true) }
                    .thenBy { it.label.contains(trimmed, ignoreCase = true) }
            ).filter { it.label.contains(trimmed, ignoreCase = true) }
        }
    }

    val webSuggestions = remember(query) {
        if (query.isBlank()) emptyList()
        else listOf("$query search", "$query news", "$query wiki")
    }

    BackHandler { onDismiss() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f)) // Solid frosted dark overlay matching Niagara
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Search Top Bar
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF161616),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search apps, contacts, or web", color = Color.Gray, fontSize = 15.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (query.isNotBlank()) {
                                    viewModel.launchWebSearch(context, query)
                                    onDismiss()
                                }
                            }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = accentColor
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                    )

                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Apps section
                if (filteredApps.isNotEmpty()) {
                    item(key = "header_apps") {
                        Text(
                            text = if (query.isBlank()) "SUGGESTED APPS" : "APPS",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                        )
                    }

                    items(
                        items = filteredApps,
                        key = { "app_${it.packageName}" }
                    ) { app ->
                        SearchAppResultRow(
                            app = app,
                            subText = "Tap to launch",
                            onClick = {
                                viewModel.trackAppLaunch(app.packageName)
                                context.packageManager.getLaunchIntentForPackage(app.packageName)?.let {
                                    context.startActivity(it)
                                }
                                onDismiss()
                            }
                        )
                    }
                }

                // Contacts section
                if (matchedContacts.isNotEmpty()) {
                    item(key = "header_contacts") {
                        Text(
                            text = "CONTACTS",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 4.dp)
                        )
                    }

                    items(
                        items = matchedContacts,
                        key = { "contact_${it.id}" }
                    ) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phoneNumber}"))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                        onDismiss()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF222222),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = contact.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Text(text = contact.phoneNumber, color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Web suggestions section
                if (query.isNotBlank()) {
                    item(key = "header_web") {
                        Text(
                            text = "WEB SEARCH",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 4.dp)
                        )
                    }

                    items(
                        items = webSuggestions,
                        key = { "web_sug_$it" }
                    ) { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.launchWebSearch(context, suggestion)
                                    onDismiss()
                                }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = suggestion, color = Color.White, fontSize = 16.sp)
                        }
                    }

                    item(key = "web_direct_query") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.launchWebSearch(context, query)
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = accentColor.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = "Search web for \"$query\"", color = accentColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchAppResultRow(app: AppModel, subText: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        app.icon?.let { iconDrawable ->
            Image(
                bitmap = iconDrawable.toBitmap(width = 44, height = 44).asImageBitmap(),
                contentDescription = app.label,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = app.label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(text = subText, color = Color.Gray, fontSize = 12.sp)
        }
    }
}
