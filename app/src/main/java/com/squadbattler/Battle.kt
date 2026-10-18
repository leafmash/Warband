package com.squadbattler

import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

data class Fighter(
    val key: String,
    val side: Char,
    val unit: String,
    val row: Int,
    var hp: Int,
    val maxhp: Int,
    val atk: Int,
    val spd: Int
)

data class BattleEvent(
    val a: String,
    val b: String,
    val heal: Boolean,
    val v: Int,
    val hp: Int,
    val dead: Boolean
)

data class BattleResult(
    val winner: Char,
    val log: List<BattleEvent>,
    val fighters: Map<String, Fighter>
)

private fun build(team: List<String?>, side: Char): List<Fighter> {
    val counts = team.filterNotNull().groupingBy { it }.eachCount()
    return team.mapIndexedNotNull { i, id ->
        if (id == null) return@mapIndexedNotNull null
        val d = Units.byId.getValue(id)
        var hp = d.hp
        var atk = d.atk
        if (id == "knight" && (counts["knight"] ?: 0) >= 3) hp = (hp * 1.3).roundToInt()
        if (id == "archer" && (counts["archer"] ?: 0) >= 2) atk = (atk * 1.2).roundToInt()
        Fighter("$side$i", side, id, i / 3, hp, hp, atk, d.spd)
    }
}

fun simulate(teamA: List<String?>, teamB: List<String?>, seed: Long): BattleResult {
    val r = Random(seed)
    val all = build(teamA, 'A') + build(teamB, 'B')
    val log = mutableListOf<BattleEvent>()

    fun alive(s: Char) = all.filter { it.side == s && it.hp > 0 }
    fun <T> pick(l: List<T>): T = l[r.nextInt(l.size)]

    fun target(f: Fighter): Fighter? {
        val foes = alive(if (f.side == 'A') 'B' else 'A')
        if (foes.isEmpty()) return null
        val d = Units.byId.getValue(f.unit)
        if (d.range > 1) return if (d.weakest) foes.minBy { it.hp } else pick(foes)
        val front = foes.minOf { it.row }
        return pick(foes.filter { it.row == front })
    }

    fun hit(f: Fighter, t: Fighter, v: Int) {
        t.hp = maxOf(0, t.hp - v)
        log.add(BattleEvent(f.key, t.key, false, v, t.hp, t.hp == 0))
    }

    var round = 1
    while (round <= 40 && alive('A').isNotEmpty() && alive('B').isNotEmpty()) {
        val order = all
            .filter { it.hp > 0 }
            .map { it to it.spd + r.nextDouble() }
            .sortedByDescending { it.second }
            .map { it.first }

        for (f in order) {
            if (f.hp <= 0) continue
            if (alive('A').isEmpty() || alive('B').isEmpty()) break
            val d = Units.byId.getValue(f.unit)

            if (d.heal > 0) {
                val hurt = alive(f.side).filter { it.hp < it.maxhp }
                if (hurt.isNotEmpty()) {
                    val t = hurt.minBy { it.hp.toDouble() / it.maxhp }
                    val v = min(d.heal, t.maxhp - t.hp)
                    t.hp += v
                    log.add(BattleEvent(f.key, t.key, true, v, t.hp, false))
                    continue
                }
            }

            val t = target(f) ?: continue
            hit(f, t, f.atk)

            if (d.splash) {
                val others = alive(t.side).filter { it !== t }
                if (others.isNotEmpty()) hit(f, pick(others), (f.atk * 0.5).roundToInt())
            }
        }
        round++
    }

    val hpA = alive('A').sumOf { it.hp }
    val hpB = alive('B').sumOf { it.hp }
    val winner = when {
        hpA > hpB -> 'A'
        hpB > hpA -> 'B'
        else -> 'D'
    }
    return BattleResult(winner, log, all.associateBy { it.key })
}

fun botTeam(rating: Int): List<String?> {
    val n = min(9, 3 + rating / 250)
    val picks = List(n) { Units.all.random() }.sortedBy { it.range }
    val team = MutableList<String?>(9) { null }
    picks.forEachIndexed { i, u -> team[i] = u.id }
    return team
}
