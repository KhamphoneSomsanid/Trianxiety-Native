package com.trianxiety.myapp.ui.rank

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.trianxiety.myapp.R
import com.trianxiety.myapp.convertTimeStamp
import com.trianxiety.myapp.database.Game
import com.trianxiety.myapp.ui.MainActivity
import kotlinx.android.synthetic.main.recycler_view_my_games_item.view.*

class RAAllGames(private val listener: OnGameSelectedListener, query: Query) :
    FirestoreAdapter<RecyclerView.ViewHolder>(query) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return GameViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recycler_view_all_games_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is GameViewHolder -> {
                holder.bind(snapshots[position], position + 1, listener)
            }
        }
        holder.itemView.txtDate
    }

    override fun onDocumentAdded(change: DocumentChange) {
        snapshots.add(change.newIndex, change.document)
        notifyItemInserted(change.newIndex)
    }

    inner class GameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val txtPosition = itemView.txtPosition
        private val txtDate = itemView.txtDate
        private val txtDuration = itemView.txtDuration
        private val txtSquares = itemView.txtSquares
        private val txtMoves = itemView.txtMoves

        fun bind(snapshot: DocumentSnapshot, position: Int, listener: OnGameSelectedListener) {
            val game: Game? = snapshot.toObject(Game::class.java)
            val mainActivity = listener as MainActivity
            if (game!!.user == FirebaseAuth.getInstance().currentUser?.uid) itemView.setBackgroundColor(
                mainActivity.resources.getColor(R.color.colorMy)
            )
            else itemView.setBackgroundColor(mainActivity.resources.getColor(R.color.colorAll))
            if (game.squaresCompleted > 0) txtPosition.text = "#$position"
            else txtPosition.text = mainActivity.resources.getString(R.string.strNoRank)
            txtDate.text = "Date: ${game.endTime.substring(0, 10)}"
            txtDuration.text = "Duration: ${convertTimeStamp(game.duration)}"
            txtSquares.text = "Squares: ${game.squaresCompleted}/18"
            txtMoves.text = "Moves: ${game.moves.size}"

            itemView.setOnClickListener {
                var positionMyGames = -1
                /*
                    Here we find whether this game belongs to the current user,
                    and if it does, we find its myGames-rank.
                */
                if (game.user == FirebaseAuth.getInstance().currentUser?.uid) {
                    val query = mainActivity.viewModel.firestore.collection("games")
                        .whereEqualTo("user", FirebaseAuth.getInstance().currentUser?.uid)
                        .orderBy("squaresCompleted", Query.Direction.DESCENDING)
                        .orderBy("duration", Query.Direction.ASCENDING)

                    query.get().addOnSuccessListener { documents ->

                        val list: MutableList<Game> = documents.toObjects(Game::class.java)

                        for ((index, gameMy) in list.withIndex()) {
                            if (gameMy.id == game.id) positionMyGames = index + 1
                        }

                        listener.onGameSelected(game, positionMyGames, position)
                    }.addOnFailureListener {

                    }
                } else listener.onGameSelected(game, positionMyGames, position)
            }
        }

    }

}