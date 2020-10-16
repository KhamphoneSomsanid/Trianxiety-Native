package com.trianxiety.myapp.ui.rank

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.trianxiety.myapp.R
import com.trianxiety.myapp.ui.MainActivity
import kotlinx.android.synthetic.main.fragment_rank.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [Rank.newInstance] factory method to
 * create an instance of this fragment.
 */
class Rank : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var RAMyGames: RAMyGames
    private lateinit var RAAllGames: RAAllGames

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_rank, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ResultsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Rank().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onResume() {
        super.onResume()
        initRecyclerView()
        RAMyGames.startListening()
        RAAllGames.startListening()
    }

    private fun initRecyclerView() {
        val mainActivity = activity as MainActivity
        rclResultsMyGames.apply {
            layoutManager = LinearLayoutManager(activity)
            val queryMyGames = mainActivity.viewModel.firestore.collection("games")
                .whereEqualTo("user", FirebaseAuth.getInstance().currentUser?.uid)
                .orderBy("squaresCompleted", Query.Direction.DESCENDING)
                .orderBy("duration", Query.Direction.ASCENDING)
            RAMyGames =
                RAMyGames(mainActivity, queryMyGames)
            adapter = RAMyGames
        }
        rclResultsAllGames.apply {
            layoutManager = LinearLayoutManager(activity)
            val queryAllGames = mainActivity.viewModel.firestore.collection("games")
                .orderBy("squaresCompleted", Query.Direction.DESCENDING)
                .orderBy("duration", Query.Direction.ASCENDING)
                .limit(100L)
            RAAllGames =
                RAAllGames(mainActivity, queryAllGames)
            adapter = RAAllGames
        }
    }

}
