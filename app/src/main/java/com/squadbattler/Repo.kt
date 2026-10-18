package com.squadbattler

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class BoardRow(val id: String, val name: String, val rating: Int)

class Repo(context: Context) {
    private val enabled = FirebaseApp.getApps(context).isNotEmpty()
    var uid: String? = null
        private set

    suspend fun init(): Boolean {
        if (!enabled) return false
        return try {
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser ?: auth.signInAnonymously().await().user
            uid = user?.uid
            uid != null
        } catch (e: Exception) {
            false
        }
    }

    suspend fun save(name: String, rating: Int, team: List<String?>) {
        val id = uid ?: return
        val data = hashMapOf(
            "name" to name,
            "rating" to rating,
            "team" to team.map { it ?: "" },
            "updatedAt" to FieldValue.serverTimestamp()
        )
        FirebaseFirestore.getInstance().collection("players").document(id).set(data).await()
    }

    suspend fun opponent(): Pair<String, List<String?>>? {
        return try {
            val snap = FirebaseFirestore.getInstance().collection("players")
                .orderBy("rating", Query.Direction.DESCENDING)
                .limit(40)
                .get()
                .await()
            val pool = snap.documents.mapNotNull { d ->
                if (d.id == uid) return@mapNotNull null
                val team = (d.get("team") as? List<*>)?.map { (it as? String)?.takeIf { s -> s.isNotEmpty() } }
                if (team == null || team.size != 9 || team.all { it == null }) null
                else (d.getString("name") ?: "Player") to team
            }
            pool.randomOrNull()
        } catch (e: Exception) {
            null
        }
    }

    fun board(): Flow<List<BoardRow>> = callbackFlow {
        val reg = FirebaseFirestore.getInstance().collection("players")
            .orderBy("rating", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    trySend(snap.documents.map {
                        BoardRow(it.id, it.getString("name") ?: "", (it.getLong("rating") ?: 0L).toInt())
                    })
                }
            }
        awaitClose { reg.remove() }
    }
}
