package com.example.actions

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log

data class ContactItem(
    val name: String,
    val phoneNumber: String
)

class ContactCallerHelper(private val context: Context) {
    private val TAG = "ContactCallerHelper"

    fun extractContactNameQuery(spokenText: String): String? {
        val lower = spokenText.lowercase().trim()
        val callTriggers = listOf(
            "ko phone lagao", "ko call karo", "ko call lagao",
            "ko call mila", "ko phone karo", "call to", "call"
        )

        for (trigger in callTriggers) {
            if (lower.contains(trigger)) {
                val cleaned = lower.replace(trigger, "")
                    .replace("hello umesh", "")
                    .replace("hey umesh", "")
                    .replace("please", "")
                    .replace("kripya", "")
                    .trim()
                if (cleaned.isNotBlank()) {
                    return cleaned
                }
            }
        }
        return null
    }

    fun searchContacts(query: String): List<ContactItem> {
        val contactList = mutableListOf<ContactItem>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        try {
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val name = it.getString(nameIndex) ?: ""
                    val number = it.getString(numberIndex) ?: ""
                    if (name.isNotBlank() && number.isNotBlank()) {
                        contactList.add(ContactItem(name, number))
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Contacts permission not granted", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error querying contacts", e)
        }

        return contactList.distinctBy { it.phoneNumber.replace("\\s".toRegex(), "") }
    }

    fun makeCall(phoneNumber: String, useDirectCall: Boolean = false) {
        val cleanNumber = phoneNumber.replace("#", "%23")
        val intentAction = if (useDirectCall) Intent.ACTION_CALL else Intent.ACTION_DIAL
        val callIntent = Intent(intentAction).apply {
            data = Uri.parse("tel:$cleanNumber")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(callIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start call intent", e)
        }
    }
}
