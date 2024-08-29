package lend.borrow.tool

import User
import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import lend.borrow.tool.shared.R
import lend.borrow.tool.shared.R.drawable

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolDetailScreen(toolId: String, user: User? = null, navController: NavController) {
    val application = (LocalContext.current as Activity).application
    val toolsViewModel: ToolsViewModel = viewModel {
        ToolsViewModel(application, user)
    }
    var favorites: MutableState<Boolean> = rememberSaveable {
        mutableStateOf(toolsViewModel.favorites.value.contains(toolId))
    }
    toolsViewModel.getTool(toolId)
    val tool by toolsViewModel.tool.collectAsState()
    var zoomIn: String? by remember {
        mutableStateOf(null)
    }
    tool?.let { chosenTool ->
        Column {


            val toolOwner = chosenTool.owner
            val toolAvailability: Boolean =
                toolOwner == null || toolOwner.availableAtTheMoment && chosenTool.available
            val toolAlpha: Float = if (toolAvailability) 1f else 0.5f

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(15.dp)
            ) {
                AnimatedVisibility(true) {
                    LazyRow(
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        items(chosenTool.imageUrls) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = it,
                                    modifier = Modifier
                                        .alpha(toolAlpha)
                                        .border(1.dp, Color.LightGray)
                                        .clickable {
                                            zoomIn = it
                                        },
                                    contentDescription = "",
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                    }
                }
                Text(text = "Tool name: ", fontWeight = FontWeight.Bold)
                Text(text = chosenTool.name, modifier = Modifier.padding(5.dp))
                Text(text = "Description: ", fontWeight = FontWeight.Bold)
                Text(text = chosenTool.description, modifier = Modifier.padding(5.dp))
                if (chosenTool.tags?.isNotEmpty() == true)
                    Spacer(modifier = Modifier.height(11.dp))
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(top = 5.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    chosenTool.tags?.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .wrapContentSize()
                                .background(
                                    color = Color.Black,
                                    shape = RoundedCornerShape(5.dp)
                                )
                                .alpha(toolAlpha)
                                .padding(7.dp),
                            Alignment.Center
                        ) {
                            Text(
                                text = tag.trim(),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 5.dp),
                                color = Color.White
                            )
                        }
                    }
                }
                user?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            enabled = chosenTool.available,
                            modifier = Modifier.alpha(toolAlpha),
                            onClick = {
                                if (toolAvailability) {
                                    chosenTool.available = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                Color(
                                    LocalContext.current.getColor(
                                        R.color.primary
                                    )
                                ), Color.White
                            ),
                            shape = RoundedCornerShape(5.dp)
                        ) {
                            Text("May I borrow this item?")
                        }
                        Image(
                            painterResource(if (favorites.value) drawable.baseline_favorite_24 else drawable.baseline_favorite_border_24),
                            contentDescription = "",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .clickable {
                                    if (favorites.value) { // This should be revised and use the single source of the truth.
                                        user.favoriteTools.remove(chosenTool.id)
                                        toolsViewModel.updateUserFavoriteTools(it)
                                        favorites.value = false
                                    } else {
                                        user.favoriteTools.add(chosenTool.id)
                                        toolsViewModel.updateUserFavoriteTools(it)
                                        favorites.value = true
                                    }
                                }
                                .align(Alignment.CenterVertically)
                        )
                    }
                }
            }
        }
    }

    zoomIn?.let {
        ZoomInImage(it) {
            zoomIn = null
        }
    }
}

@Composable
fun ZoomInImage(model: String, contentDescription: String? = null, onDismiss: () -> Unit) {
    var size by remember { mutableStateOf(Size.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties()
    ) {
        Card(
            Modifier.wrapContentSize(),
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp),
            backgroundColor = Color.LightGray
        ) {
            Box(
                Modifier
                    .padding(10.dp)
                    .clipToBounds()
                    .onSizeChanged {
                        size = it.toSize()
                    },
                contentAlignment = Alignment.TopEnd) {
                val angle by remember { mutableStateOf(0f) }
                var zoom by remember { mutableStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }

                AsyncImage(
                    model,
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .wrapContentSize()
                        .graphicsLayer(
                            scaleX = zoom,
                            scaleY = zoom,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .pointerInput(Unit) {
                            detectTransformGestures(
                                onGesture = { _, gesturePan, gestureZoom, _ ->
                                    zoom = (zoom * gestureZoom).coerceIn(1F..4F)
                                    val newOffset = offset + gesturePan.times(zoom)

                                    val maxX = (size.width * (zoom - 1) / 2f)
                                    val maxY = (size.height * (zoom - 1) / 2f)

                                    offset = Offset(
                                        newOffset.x.coerceIn(-maxX, maxX),
                                        newOffset.y.coerceIn(-maxY, maxY)
                                    )
                                    println("Ehsan: offset: $offset")
                                }
                            )
                        }
                )
                IconButton(onClick = { onDismiss() },
                    Modifier.clip(CircleShape)) {
                    Box(modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White)
                        .wrapContentSize()) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            Modifier.clip(CircleShape)
                        )
                    }

                }
            }
        }
    }
}