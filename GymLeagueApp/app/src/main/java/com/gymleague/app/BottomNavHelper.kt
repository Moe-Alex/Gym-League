package com.gymleague.app

import android.content.Intent
import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat

enum class NavTab {
    HOME, TRAIN, SQUAD, BATTLE, RANKS, MORE
}

object BottomNavHelper {

    private data class NavItem(
        val tab: NavTab,
        val container: LinearLayout,
        val icon: ImageView,
        val label: TextView
    )

    fun setup(
        activity: AppCompatActivity,
        navRoot: View,
        activeTab: NavTab
    ) {

        val items = listOf(
            NavItem(NavTab.HOME, navRoot.findViewById(R.id.btnHome), navRoot.findViewById(R.id.ivHome), navRoot.findViewById(R.id.tvHome)),
            NavItem(NavTab.TRAIN, navRoot.findViewById(R.id.btnTrain), navRoot.findViewById(R.id.ivTrain), navRoot.findViewById(R.id.tvTrain)),
            NavItem(NavTab.SQUAD, navRoot.findViewById(R.id.btnSquad), navRoot.findViewById(R.id.ivSquad), navRoot.findViewById(R.id.tvSquad)),
            NavItem(NavTab.BATTLE, navRoot.findViewById(R.id.btnBattle), navRoot.findViewById(R.id.ivBattle), navRoot.findViewById(R.id.tvBattle)),
            NavItem(NavTab.RANKS, navRoot.findViewById(R.id.btnRanks), navRoot.findViewById(R.id.ivRanks), navRoot.findViewById(R.id.tvRanks)),
            NavItem(NavTab.MORE, navRoot.findViewById(R.id.btnMore), navRoot.findViewById(R.id.ivMore), navRoot.findViewById(R.id.tvMore))
        )

        val activeColor = ContextCompat.getColor(activity, R.color.gym_nav_active)
        val inactiveColor = ContextCompat.getColor(activity, R.color.gym_nav_inactive)

        for (item in items) {

            val isSelected = item.tab == activeTab

            item.container.background = if (isSelected) {
                ContextCompat.getDrawable(activity, R.drawable.bg_nav_pill)
            } else {
                null
            }

            val tintColor = if (isSelected) activeColor else inactiveColor

            ImageViewCompat.setImageTintList(
                item.icon,
                ColorStateList.valueOf(tintColor)
            )

            item.label.setTextColor(tintColor)

            item.container.setOnClickListener {
                if (item.tab != activeTab) {
                    navigateTo(activity, item.tab)
                }
            }
        }
    }

    private fun navigateTo(activity: AppCompatActivity, tab: NavTab) {

        val intent = when (tab) {

            NavTab.HOME -> Intent(activity, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_SELECTED_TAB, MainActivity.TAB_HOME)
            }

            NavTab.BATTLE -> Intent(activity, BattleActivity::class.java)

            NavTab.MORE -> Intent(activity, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_SELECTED_TAB, MainActivity.TAB_MORE)
            }

            NavTab.TRAIN -> Intent(activity, ExerciseLibraryActivity::class.java)

            NavTab.SQUAD -> Intent(activity, SquadActivity::class.java)

            NavTab.RANKS -> Intent(activity, PersonalRecordsActivity::class.java)
        }

        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT

        activity.startActivity(intent)
    }
}