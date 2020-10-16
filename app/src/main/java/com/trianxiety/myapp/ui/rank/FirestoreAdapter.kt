package com.trianxiety.myapp.ui.rank

import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import com.trianxiety.myapp.database.Game
import java.util.*

abstract class FirestoreAdapter<VH : RecyclerView.ViewHolder>(private val query: Query) :
    RecyclerView.Adapter<VH>(),
    EventListener<QuerySnapshot> {

    private var registration: ListenerRegistration? = null

    val snapshots =
        ArrayList<DocumentSnapshot>()

    open fun startListening() {
        if (query != null && registration == null) {
            registration = query.addSnapshotListener(this)
        }
    }

    open fun stopListening() {
        if (registration != null) {
            registration!!.remove()
            registration = null
        }
        snapshots.clear()
        notifyDataSetChanged()
    }

    override fun onEvent(p0: QuerySnapshot?, p1: FirebaseFirestoreException?) {

        if (p1 != null) {
            return
        }

        // Dispatch the event
        for (change in p0!!.documentChanges) {
            if (change.type == DocumentChange.Type.ADDED) onDocumentAdded(change)
        }
    }

    abstract fun onDocumentAdded(change: DocumentChange)

    override fun getItemCount(): Int {
        return snapshots.size
    }

    interface OnGameSelectedListener {
        fun onGameSelected(game: Game, positionMyGames: Int, positionAllGames: Int)
    }

}