package com.raguel0087.myapplication1.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class BibleVerse(
    @DocumentId
    val id: String = "",
    val verse: String = "",
    val reference: String = "",
    val imageUrl: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val createdAt: Timestamp = Timestamp.now()
) {
    // Required by Firestore for deserialization
    constructor() : this("", "", "", "", "", "", Timestamp.now())

    fun toMap(): Map<String, Any> = mapOf(
        "verse" to verse,
        "reference" to reference,
        "imageUrl" to imageUrl,
        "ownerId" to ownerId,
        "ownerName" to ownerName,
        "createdAt" to createdAt
    )
}
