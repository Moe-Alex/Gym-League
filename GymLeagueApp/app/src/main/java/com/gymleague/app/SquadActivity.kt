package com.gymleague.app

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gymleague.app.repository.SquadRepository
import kotlinx.coroutines.launch

class SquadActivity : AppCompatActivity() {

    private val squadRepository = SquadRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_squad)

        val btnRefresh =
            findViewById<Button>(R.id.btnRefreshSquad)

        btnRefresh.setOnClickListener {
            loadSquads()
        }
    }

    private fun loadSquads() {

        lifecycleScope.launch {

            val result =
                squadRepository.getSquads()

            result.onSuccess { squads ->

                Toast.makeText(
                    this@SquadActivity,
                    "Loaded ${squads.size} squad(s)",
                    Toast.LENGTH_SHORT
                ).show()
            }

            result.onFailure { error ->

                Toast.makeText(
                    this@SquadActivity,
                    "Could not load squads: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}