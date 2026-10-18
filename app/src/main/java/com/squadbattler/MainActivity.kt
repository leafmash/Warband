package com.squadbattler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val Bg = Color(0xFF10252A)
private val Panel = Color(0xFF17343A)
private val Line = Color(0xFF2B5058)
private val Paper = Color(0xFFEFE6D2)
private val Dim = Color(0xFF9FB3B0)
private val Brass = Color(0xFFC9A24B)
private val Ember = Color(0xFFD0523A)
private val Moss = Color(0xFF6F9B6A)
private val HurtBg = Color(0xFF6A2B22)
private val HealBg = Color(0xFF2D5A40)

private fun unitColor(id: String) = when (id) {
    "knight" -> Color(0xFF20403F)
    "archer" -> Color(0xFF2D3D27)
    "healer" -> Color(0xFF2F3A45)
    else -> Color(0xFF44302F)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SquadApp() }
    }
}

@Composable
fun SquadApp(vm: GameViewModel = viewModel()) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Brass,
            background = Bg,
            surface = Panel,
            onBackground = Paper,
            onSurface = Paper
        )
    ) {
        Surface(color = Bg, contentColor = Paper, modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .systemBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text("Warband", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        when (vm.online) {
                            null -> "Connecting"
                            true -> "Online"
                            false -> "Offline mode"
                        },
                        fontSize = 12.sp,
                        color = Dim
                    )
                }

                vm.message?.let {
                    Text(
                        it,
                        color = Bg,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Paper)
                            .padding(10.dp)
                    )
                }

                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox("Coins", vm.coins.toString(), Modifier.weight(1f))
                    StatBox("Rating", vm.rating.toString(), Modifier.weight(1f))
                    OutlinedTextField(
                        value = vm.name,
                        onValueChange = vm::rename,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Brass,
                            unfocusedBorderColor = Line,
                            focusedContainerColor = Panel,
                            unfocusedContainerColor = Panel
                        ),
                        modifier = Modifier.weight(1.4f)
                    )
                }

                vm.foeName?.let { foe ->
                    Title(foe)
                    Board(listOf(6, 7, 8, 3, 4, 5, 0, 1, 2), "B", vm.foeTeam, vm, null)
                    Caption("Front line")
                }

                Title("Your squad")
                Caption("Front line")
                Board((0..8).toList(), "A", vm.team, vm) { vm.tapCell(it) }

                Title("Shop")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    vm.shop.forEachIndexed { i, id ->
                        ShopCard(id, vm.pick == i, Modifier.weight(1f)) { vm.tapShop(i) }
                    }
                }
                val picked = vm.pick?.let { vm.shop[it] }?.let { Units.byId[it] }
                Text(
                    picked?.note ?: "Tap a unit, then tap an empty slot. Tap a placed unit to sell it for half price.",
                    fontSize = 12.sp,
                    color = Dim,
                    modifier = Modifier.padding(top = 6.dp).height(34.dp)
                )

                Button(
                    onClick = vm::battle,
                    enabled = !vm.busy,
                    colors = ButtonDefaults.buttonColors(containerColor = Ember, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(52.dp)
                ) {
                    Text("Battle", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                }

                Text(
                    vm.result,
                    color = Brass,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(26.dp)
                )

                Column(Modifier.fillMaxWidth().height(100.dp)) {
                    vm.logLines.forEach { Text(it, fontSize = 12.sp, color = Dim) }
                }

                Title("Live leaderboard")
                when {
                    vm.online == false -> Caption("Connect Firebase to see the live leaderboard.")
                    vm.board.isEmpty() -> Caption("No players yet. Win a battle to appear here.")
                    else -> vm.board.forEachIndexed { i, row ->
                        val mine = row.id == vm.uid
                        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                            Text("${i + 1}", color = if (mine) Brass else Paper, modifier = Modifier.weight(0.12f))
                            Text(row.name, color = if (mine) Brass else Paper, modifier = Modifier.weight(0.68f))
                            Text(row.rating.toString(), color = if (mine) Brass else Paper, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Dim, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
}

@Composable
private fun Caption(text: String) {
    Text(text, fontSize = 11.sp, color = Dim, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Panel)
            .border(1.dp, Line, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, fontSize = 13.sp)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Brass)
    }
}

@Composable
private fun ShopCard(id: String?, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val def = id?.let { Units.byId[it] }
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier
            .height(84.dp)
            .alpha(if (def == null) 0.2f else 1f)
            .clip(shape)
            .background(if (selected) Color(0xFF234850) else Panel)
            .border(1.dp, if (selected) Brass else Line, shape)
            .clickable(enabled = def != null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (def != null) {
            Text(def.glyph, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Brass)
            Text(def.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(def.cost.toString(), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Ember)
        }
    }
}

@Composable
private fun Board(order: List<Int>, prefix: String, team: List<String?>, vm: GameViewModel, onTap: ((Int) -> Unit)?) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Panel)
            .border(1.dp, Line, shape)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        order.chunked(3).forEach { rowIdx ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowIdx.forEach { i ->
                    val key = prefix + i
                    Cell(
                        Modifier.weight(1f),
                        team.getOrNull(i),
                        vm.hp[key],
                        vm.flashKey == key,
                        vm.flashHeal,
                        vm.activeKey == key
                    ) { onTap?.invoke(i) }
                }
            }
        }
    }
}

@Composable
private fun Cell(
    modifier: Modifier,
    unit: String?,
    hp: Float?,
    flash: Boolean,
    heal: Boolean,
    active: Boolean,
    onClick: () -> Unit
) {
    val def = unit?.let { Units.byId[it] }
    val bg = when {
        flash && heal -> HealBg
        flash -> HurtBg
        def != null -> unitColor(def.id)
        else -> Bg
    }
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .aspectRatio(1.15f)
            .alpha(if (hp != null && hp <= 0f) 0.25f else 1f)
            .clip(shape)
            .background(bg)
            .border(if (active) 2.dp else 1.dp, if (active || def != null) Brass else Line, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (def != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(def.glyph, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Brass)
                Text(def.name, fontSize = 11.sp)
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 6.dp, vertical = 5.dp)
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x88000000))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((hp ?: 1f).coerceIn(0f, 1f))
                        .background(Moss)
                )
            }
        }
    }
}
