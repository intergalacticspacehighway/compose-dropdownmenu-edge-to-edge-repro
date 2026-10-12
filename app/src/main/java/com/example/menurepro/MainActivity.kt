package com.example.menurepro

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val TAG = "MenuRepro"

/**
 * Repro: Material3 DropdownMenu opens detached above an anchor near the bottom of an
 * edge-to-edge window.
 *
 * Launch extras (optional, for scripted runs):
 *   --es open middle|raised|bottom   open that menu on launch
 *   --ez clipping false              use PopupProperties(clippingEnabled = false)
 */
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    val autoOpen = intent.getStringExtra("open")
    val initialClipping = intent.getBooleanExtra("clipping", true)
    setContent {
      MaterialTheme {
        ReproScreen(autoOpen, initialClipping)
      }
    }
  }
}

@Composable
private fun ReproScreen(autoOpen: String?, initialClipping: Boolean) {
  var clippingEnabled by remember { mutableStateOf(initialClipping) }
  val results = remember { mutableStateListOf<String>() }
  val density = LocalDensity.current
  val view = LocalView.current
  val statusBar = WindowInsets.statusBars.getTop(density)
  val navBar = WindowInsets.navigationBars.getBottom(density)
  val visibleFrame = Rect().also { view.getWindowVisibleDisplayFrame(it) }
  val report: (String) -> Unit = { line ->
    Log.i(TAG, line)
    results.add(line)
  }

  Box(
    Modifier
      .fillMaxSize()
      .background(Color.White)
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .padding(8.dp)
  ) {
    Column {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("clippingEnabled = $clippingEnabled  ")
        Switch(checked = clippingEnabled, onCheckedChange = {
          clippingEnabled = it
          results.clear()
        })
      }
      Text(
        "density=${density.density} statusBar=${statusBar}px navBar=${navBar}px " +
          "screenH=${view.rootView.height}px visibleFrame=${visibleFrame.toShortString()}",
        fontSize = 11.sp
      )
      results.forEach { Text(it, fontSize = 11.sp) }
    }

    // Controls: anchors with room around them.
    MenuAnchor("middle", clippingEnabled, autoOpen, report, Modifier.align(Alignment.Center))
    MenuAnchor(
      "raised",
      clippingEnabled,
      autoOpen,
      report,
      Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)
    )
    // The reported case: anchor at the bottom of the safe area.
    MenuAnchor("bottom", clippingEnabled, autoOpen, report, Modifier.align(Alignment.BottomStart))
  }
}

@Composable
private fun MenuAnchor(
  name: String,
  clippingEnabled: Boolean,
  autoOpen: String?,
  report: (String) -> Unit,
  modifier: Modifier
) {
  var expanded by remember { mutableStateOf(autoOpen == name) }
  var anchorTop by remember { mutableStateOf(0) }
  var anchorBottom by remember { mutableStateOf(0) }

  Box(modifier) {
    Box(
      Modifier
        .size(27.dp)
        .border(1.dp, Color.Black)
        .clickable { expanded = true }
        .onGloballyPositioned {
          anchorTop = it.positionOnScreen().y.roundToInt()
          anchorBottom = anchorTop + it.size.height
        },
      contentAlignment = Alignment.Center
    ) {
      Text(name.first().uppercase())
    }

    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      properties = PopupProperties(focusable = true, clippingEnabled = clippingEnabled)
    ) {
      val popupView = LocalView.current
      LaunchedEffect(Unit) {
        // Wait for the enter animation, then read the popup window's real position.
        delay(800)
        val location = IntArray(2)
        popupView.getLocationOnScreen(location)
        val menuTop = location[1]
        val menuBottom = menuTop + popupView.height
        val gap = if (menuBottom <= anchorTop) anchorTop - menuBottom else menuTop - anchorBottom
        report(
          "[$name clipping=$clippingEnabled] anchor=$anchorTop..$anchorBottom " +
            "menu=$menuTop..$menuBottom gap=${gap}px"
        )
      }
      DropdownMenuItem(text = { Text("Upload from library") }, onClick = { expanded = false })
      DropdownMenuItem(text = { Text("Upload from files") }, onClick = { expanded = false })
      DropdownMenuItem(text = { Text("Open camera") }, onClick = { expanded = false })
    }
  }
}
