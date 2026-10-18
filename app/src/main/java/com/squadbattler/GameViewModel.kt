package com.squadbattler

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("squad", Context.MODE_PRIVATE)
    private val repo = Repo(app)

    var coins by mutableIntStateOf(prefs.getInt("coins", 15))
    var rating by mutableIntStateOf(prefs.getInt("rating", 1000))
    var name by mutableStateOf(prefs.getString("name", null) ?: "Player${Random.nextInt(1000, 9999)}")
    var team by mutableStateOf(loadTeam())
    var shop by mutableStateOf(rollShop())
    var pick by mutableStateOf<Int?>(null)
    var busy by mutableStateOf(false)
    var online by mutableStateOf<Boolean?>(null)
    var foeName by mutableStateOf<String?>(null)
    var foeTeam by mutableStateOf<List<String?>>(emptyList())
    var hp by mutableStateOf<Map<String, Float>>(emptyMap())
    var activeKey by mutableStateOf<String?>(null)
    var flashKey by mutableStateOf<String?>(null)
    var flashHeal by mutableStateOf(false)
    var logLines by mutableStateOf<List<String>>(emptyList())
    var result by mutableStateOf("")
    var message by mutableStateOf<String?>(null)
    var board by mutableStateOf<List<BoardRow>>(emptyList())
    val uid: String? get() = repo.uid

    init {
        viewModelScope.launch {
            val ok = repo.init()
            online = ok
            if (ok) repo.board().collect { board = it }
        }
    }

    private fun loadTeam(): List<String?> {
        val parts = prefs.getString("team", null)?.split(",") ?: return List(9) { null }
        if (parts.size != 9) return List(9) { null }
        return parts.map { if (it == "-" || Units.byId[it] == null) null else it }
    }

    private fun rollShop(): List<String?> = List(5) { Units.all.random().id }

    private fun persist() {
        prefs.edit()
            .putInt("coins", coins)
            .putInt("rating", rating)
            .putString("name", name)
            .putString("team", team.joinToString(",") { it ?: "-" })
            .apply()
    }

    private fun say(msg: String) {
        message = msg
        viewModelScope.launch {
            delay(2000)
            if (message == msg) message = null
        }
    }

    fun rename(value: String) {
        name = value.take(20)
        persist()
    }

    fun tapShop(i: Int) {
        if (busy || shop[i] == null) return
        pick = if (pick == i) null else i
    }

    fun tapCell(i: Int) {
        if (busy) return
        val cur = team[i]
        val p = pick
        if (p != null) {
            if (cur != null) {
                say("That slot is taken.")
                return
            }
            val u = shop[p] ?: return
            val cost = Units.byId.getValue(u).cost
            if (coins < cost) {
                say("Not enough coins.")
                return
            }
            coins -= cost
            team = team.toMutableList().also { it[i] = u }
            shop = shop.toMutableList().also { it[p] = null }
            pick = null
        } else if (cur != null) {
            val def = Units.byId.getValue(cur)
            val back = def.cost / 2
            coins += back
            team = team.toMutableList().also { it[i] = null }
            say("Sold ${def.name} for $back")
        } else {
            return
        }
        persist()
    }

    fun battle() {
        if (busy) return
        if (team.none { it != null }) {
            say("Place at least one unit first.")
            return
        }
        busy = true
        pick = null
        result = ""
        logLines = emptyList()
        viewModelScope.launch {
            val found = if (online == true) repo.opponent() else null
            val foe = found ?: ("Training Bot" to botTeam(rating))
            val res = simulate(team, foe.second, Random.nextLong(0L, 4294967296L))
            foeName = foe.first
            foeTeam = foe.second
            hp = res.fighters.mapValues { 1f }

            for (ev in res.log) {
                val actor = res.fighters.getValue(ev.a)
                val victim = res.fighters.getValue(ev.b)
                activeKey = ev.a
                flashKey = ev.b
                flashHeal = ev.heal
                hp = hp + (ev.b to ev.hp.toFloat() / victim.maxhp)
                val who = { f: Fighter -> (if (f.side == 'A') "Your " else "${foe.first}'s ") + Units.byId.getValue(f.unit).name }
                val verb = if (ev.heal) "heals" else "hits"
                val line = "${who(actor)} $verb ${who(victim)} for ${ev.v}${if (ev.dead) " (down)" else ""}"
                logLines = (listOf(line) + logLines).take(6)
                delay(320)
                activeKey = null
                flashKey = null
            }

            val win = res.winner == 'A'
            val draw = res.winner == 'D'
            rating = maxOf(0, rating + if (win) 25 else if (draw) 0 else -20)
            coins += 8 + if (win) 2 else 0
            result = if (win) "Victory +25" else if (draw) "Draw" else "Defeat -20"
            shop = rollShop()
            persist()
            if (online == true) {
                try {
                    repo.save(name, rating, team)
                } catch (e: Exception) {
                    say("Could not save online.")
                }
            }

            delay(1200)
            hp = hp.filterKeys { !it.startsWith("A") }
            busy = false
        }
    }
}
