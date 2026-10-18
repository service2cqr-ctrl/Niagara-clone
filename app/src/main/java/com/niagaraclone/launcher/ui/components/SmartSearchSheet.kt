package com.niagaraclone.launcher.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.niagaraclone.launcher.data.AppModel
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.theme.LocalLauncherPalette

data class ContactSearchResult(val name: String, val phoneNumber: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSearchSheet(
    viewModel: LauncherViewModel,
    apps: List<AppModel>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalLauncherPalette.current
    var query by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf<List<ContactSearchResult>>(emptyList()) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val accentColorArgb by viewModel.accentColorArgb.collectAsState()
    val accentColor = Color(accentColorArgb)

    val filteredApps = remember(query, apps) {
        if (query.isBlank()) emptyList()
        else apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    LaunchedEffect(query) {
        if (query.length >= 2) {
            contacts = queryContacts(context, query)
        } else {
            contacts = emptyList()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.sheetBackground,
        contentColor = palette.textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search apps, contacts & web...", color = palette.textSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = accentColor) },
                textStyle = LocalTextStyle.current.copy(color = palette.textPrimary),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = palette.borderColor,
                    focusedContainerColor = palette.cardBackground,
                    unfocusedContainerColor = palette.cardBackground
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                if (filteredApps.isNotEmpty()) {
                    item(key = "header_apps") {
                        Text(
                            text = "APPLICATIONS",
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(
                        items = filteredApps,
                        key = { "app_${it.packageName}" }
                    ) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.trackAppLaunch(app.packageName)
                                    context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { context.startActivity(it) }
                                    onDismiss()
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            app.icon?.let {
                                Image(bitmap = it.toBitmap(width = 40, height = 40).asImageBitmap(), contentDescription = null, modifier = Modifier.size(30.dp))
                                Spacer(modifier = Modifier.width(14.dp))
                            } ?: Spacer(modifier = Modifier.width(44.dp))
                            Text(text = app.label, color = palette.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                if (contacts.isNotEmpty()) {
                    item(key = "header_contacts") {
                        Text(
                            text = "CONTACTS",
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(
                        items = contacts,
                        key = { "contact_${it.phoneNumber}" }
                    ) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phoneNumber}"))
                                    context.startActivity(intent)
                                    onDismiss()
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = palette.cardBackground,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = contact.name, color = palette.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Text(text = contact.phoneNumber, color = palette.textSecondary, fontSize = 12.sp)
                            }
                            Icon(Icons.Default.Call, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                if (query.isNotBlank()) {
                    item(key = "web_search_item") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.launchWebSearch(context, query)
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = accentColor.copy(alpha = 0.2f),
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(text = "Search web for \"$query\"", color = accentColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

private fun queryContacts(context: Context, query: String): List<ContactSearchResult> {
    val results = mutableListOf<ContactSearchResult>()
    try {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$query%"),
            null
        )
        cursor?.use {
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext() && results.size < 5) {
                results.add(ContactSearchResult(it.getString(nameIdx), it.getString(phoneIdx)))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return results
}
