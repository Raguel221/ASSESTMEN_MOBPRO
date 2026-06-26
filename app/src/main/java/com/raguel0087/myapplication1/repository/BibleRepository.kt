package com.raguel0087.myapplication1.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.raguel0087.myapplication1.model.BibleVerse
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.util.UUID

class BibleRepository {

    companion object {
        const val SUPABASE_URL = "https://dbmvkajbhsqtvsyemwgb.supabase.co"
        const val SUPABASE_ANON_KEY = "sb_publishable_FNao1u98rD8O-ePZ5RDF2w_V47g5RSr"
        const val STORAGE_BUCKET = "bible-verses"
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val versesCollection = firestore.collection("verses")
    private val httpClient = HttpClient()

    fun getVerses(): Flow<Result<List<BibleVerse>>> = callbackFlow {
        val listener = versesCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    try {
                        val verses = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(BibleVerse::class.java)?.copy(id = doc.id)
                        }
                        trySend(Result.success(verses))
                    } catch (e: Exception) {
                        trySend(Result.failure(e))
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun addVerse(verse: BibleVerse): Result<Unit> = runCatching {
        versesCollection.add(verse.toMap()).await()
        Unit
    }

    suspend fun updateVerse(verseId: String, updatedVerse: BibleVerse): Result<Unit> = runCatching {
        val updates = mapOf(
            "verse" to updatedVerse.verse,
            "reference" to updatedVerse.reference,
            "imageUrl" to updatedVerse.imageUrl
        )
        versesCollection.document(verseId).update(updates).await()
        Unit
    }

    suspend fun deleteVerse(verseId: String): Result<Unit> = runCatching {
        versesCollection.document(verseId).delete().await()
        Unit
    }

    suspend fun uploadImage(imageUri: Uri, context: Context): Result<String> = runCatching {
        val inputStream = context.contentResolver.openInputStream(imageUri)
            ?: throw Exception("File tidak bisa dibuka")
        
        val fileBytes = inputStream.readBytes()
        inputStream.close()
        
        if (fileBytes.isEmpty()) throw Exception("File kosong")
        
        val fileName = "verse_images/${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
        val fullPath = "$STORAGE_BUCKET/$fileName"
        
        val response = httpClient.put("$SUPABASE_URL/storage/v1/object/$fullPath") {
            headers {
                append("apikey", SUPABASE_ANON_KEY)
                append("Authorization", "Bearer $SUPABASE_ANON_KEY")
            }
            contentType(ContentType.Image.JPEG)
            setBody(fileBytes)
        }
        
        if (response.status.value !in 200..299) {
            throw Exception("Upload gagal: HTTP ${response.status.value}")
        }
        
        "$SUPABASE_URL/storage/v1/object/public/$fullPath"
    }

    private fun BibleVerse.toMap(): Map<String, Any?> = mapOf(
        "verse" to this.verse,
        "reference" to this.reference,
        "imageUrl" to this.imageUrl,
        "ownerId" to this.ownerId,
        "ownerName" to this.ownerName,
        "createdAt" to this.createdAt
    )

    fun cleanup() {
        httpClient.close()
    }
}
