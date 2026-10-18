package com.squadbattler

data class UnitDef(
    val id: String,
    val name: String,
    val cost: Int,
    val hp: Int,
    val atk: Int,
    val spd: Int,
    val range: Int,
    val glyph: String,
    val note: String,
    val heal: Int = 0,
    val splash: Boolean = false,
    val weakest: Boolean = false
)

object Units {
    val all = listOf(
        UnitDef("knight", "Knight", 3, 70, 8, 2, 1, "K", "Front-line tank. Three Knights gain 30% health."),
        UnitDef("archer", "Archer", 3, 32, 10, 5, 3, "A", "Fast. Shoots the weakest enemy. Two Archers gain 20% attack.", weakest = true),
        UnitDef("healer", "Healer", 4, 38, 3, 3, 3, "H", "Heals the most wounded ally.", heal = 9),
        UnitDef("mage", "Mage", 5, 26, 14, 3, 3, "M", "Hits one enemy and splashes a second.", splash = true)
    )

    val byId = all.associateBy { it.id }
}
