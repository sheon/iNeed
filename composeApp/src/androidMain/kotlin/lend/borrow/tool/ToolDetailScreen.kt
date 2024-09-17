package lend.borrow.tool

import ToolDetailUiState
import User
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import lend.borrow.tool.shared.R
import lend.borrow.tool.utility.CustomDialogWithResult
import lend.borrow.tool.utility.DropDownMenu
import lend.borrow.tool.utility.LogCompositions

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun ToolDetailScreen(toolId: String, user: User? = null, navController: NavController) {
    val application = (LocalContext.current as Activity).application
    val toolDetailViewModel: ToolDetailViewModel = viewModel(key = toolId) {
        ToolDetailViewModel(application, toolId, user?.id)
    }
    // This would cause the tool info cards to recreated constantly and that is not efficient. But this is also needed to update the dropdown menu.
    //Todo: maybe I can tweak the composable views so not the whole card is recomposed.
//    LaunchedEffect(Unit) {
//        toolDetailViewModel.initiateViewModel()
//    }
    ToolDetailContent(toolDetailViewModel = toolDetailViewModel, user = user, navController)
}

@Composable
fun ToolDetailContent(toolDetailViewModel: ToolDetailViewModel, user: User?, navController: NavController) {
    val tool by toolDetailViewModel.tool.collectAsState()
    val latestErrorMessage by toolDetailViewModel.latestErrorMessage.collectAsState()
    val isEditingToolInfo by toolDetailViewModel.isEditingToolInfo.collectAsState()
    when {
        tool != null -> {
            Column {
                if (!isEditingToolInfo){
                    DropDownMenu(tool!!.defaultTool, toolDetailViewModel, navController, user?.id)
                    StaticToolInfoScreen(tool!!, user, toolDetailViewModel, true)
                } else {
                    EditingToolInfoScreen(tool!!, toolDetailViewModel)
                }
            }
        }
        latestErrorMessage != null -> {
            Box(modifier = Modifier
                .fillMaxSize(),
                contentAlignment = Alignment.Center) {
                Text(text = latestErrorMessage.toString(), fontWeight = FontWeight.W900)
            }
        }
        else -> {}
    }
    ProgressbarView(toolDetailViewModel)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StaticToolInfoScreen(chosenTool: ToolDetailUiState, user: User?, toolDetailViewModel: ToolDetailViewModel, userOwnThisTool: Boolean = false) {
    var zoomIn: String? by remember {
        mutableStateOf(null)
    }

    val toolOwner = chosenTool.owner
    val toolAvailability: Boolean = userOwnThisTool || toolOwner.availableAtTheMoment && chosenTool.isAvailable
    val toolAlpha: Float = if (toolAvailability) 1f else 0.5f

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp)
    ) {
        AnimatedVisibility(true) {
            LazyRow(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                items(chosenTool.imageUrlsRefMap.keys.toList()) {
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
        Text(text = "Instruction: ", fontWeight = FontWeight.Bold)
        Text(text = chosenTool.instruction, modifier = Modifier.padding(5.dp))
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
            chosenTool.tags?.replace(" ", "")?.split(",")?.forEach { tag ->
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .background(
                            color = Color.Black, shape = RoundedCornerShape(5.dp)
                        )
                        .alpha(toolAlpha)
                        .padding(7.dp),
                    Alignment.Center
                ) {
                    Text(
                        text = tag,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 5.dp),
                        color = Color.White
                    )
                }
            }
        }

        UserBorrowRequestButtonAndFavoritesView(user, toolDetailViewModel, chosenTool)

    }
    zoomIn?.let {
        ZoomInImage(it) {
            zoomIn = null
        }
    }
}

@Composable
fun EditingToolInfoScreen(
    chosenTool: ToolDetailUiState,
    toolDetailViewModel: ToolDetailViewModel
) {
    val ownerToolDetailUi by toolDetailViewModel.toolDetailUiState.collectAsState()
    val takingPics by toolDetailViewModel.takingAPicture.collectAsState()
    var zoomIn: String? by remember {
        mutableStateOf(null)
    }
    var deletingAPic: String? by remember {
        mutableStateOf(null)
    }
    val numberOfImagesForTool = 5
    val scrollState = rememberScrollState()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(Modifier.size(10.dp))
        AnimatedVisibility(true) {
            LazyRow(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                items(numberOfImagesForTool) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(5.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        ownerToolDetailUi.imageUrlsRefMap.keys.toList().getOrNull(it).let {
                            Box(modifier = Modifier.size(150.dp, 200.dp)) {
                                AsyncImage(
                                    model = it,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.LightGray)
                                        .clickable {
                                            if (it != null)
                                                zoomIn = it
                                            else
                                                toolDetailViewModel.startCamera(true)
                                        },
                                    alpha = if (it == null) 0.3f else 1f,
                                    placeholder = painterResource(id = lend.borrow.tool.R.drawable.baseline_image_24),
                                    fallback = painterResource(id = lend.borrow.tool.R.drawable.baseline_image_24),
                                    contentDescription = "",
                                    contentScale = ContentScale.Fit
                                )
                                it?.let {
                                    IconButton(onClick = {
                                        deletingAPic = ownerToolDetailUi.imageUrlsRefMap[it]
                                    }) {
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(Color.White)
                                                .wrapContentSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Delete image"
                                            )
                                        }
                                    }
                                }
                            }
                        }

                    }
                }

            }
        }

        Spacer(modifier = Modifier.size(10.dp))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = ownerToolDetailUi.name,
            label = {
                Text("Tool name")
            },
            onValueChange = {
                if (it != ownerToolDetailUi.name)
                    toolDetailViewModel.onToolNameChanged(it)
            }

        )
        
        Spacer(modifier = Modifier.size(10.dp))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = ownerToolDetailUi.description,
            label = {
                Text("Tool description")
            },
            onValueChange = {
                if (it != ownerToolDetailUi.description)
                    toolDetailViewModel.onToolDescriptionChanged(it)
            }
        )
        Spacer(modifier = Modifier.height(11.dp))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = ownerToolDetailUi.instruction,
            label = {
                Text("Instruction")
            },
            onValueChange = {
                if (it != ownerToolDetailUi.instruction)
                    toolDetailViewModel.onToolInstructionChanged(it)
            }
        )

        Spacer(modifier = Modifier.height(11.dp))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = ownerToolDetailUi.tags ?: "",
            label = {
                Text("Tags")
            },
            onValueChange = {
                if (it != ownerToolDetailUi.tags)
                    toolDetailViewModel.onToolTagsChanged(it)
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                onClick = {
                    toolDetailViewModel.discardChangesInToolDetail(chosenTool)
                }, colors = ButtonDefaults.buttonColors(
                    Color.Gray, Color.White
                )
            ) {
                Text(text = AnnotatedString("Discard changes"))
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                onClick = {
                    toolDetailViewModel.saveChangesInToolDetail()
                }, colors = ButtonDefaults.buttonColors(
                    Color(LocalContext.current.resources.getColor(R.color.primary)), Color.White
                )
            ) {
                Text(text = AnnotatedString("Save changes"))
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                onClick = {
                    toolDetailViewModel.deleteTool(ownerToolDetailUi)
                }, colors = ButtonDefaults.buttonColors(
                    Color.Red, Color.White
                )
            ) {
                Text(text = AnnotatedString("Delete this tool"))
            }
        }
        Spacer(Modifier.size(10.dp))
    }
    zoomIn?.let {
        ZoomInImage(it) {
            zoomIn = null
        }
    }
    deletingAPic?.let {
        CustomDialogWithResult(onDismiss = { deletingAPic = null}) {
            Card(
                elevation = 8.dp,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)) {
                    Text("Are you sure you want to delete this picture?", Modifier.padding(10.dp))
                    Spacer(modifier = Modifier.size(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            modifier = Modifier
                                .wrapContentWidth()
                                .padding(horizontal = 10.dp),
                            shape = RoundedCornerShape(10.dp),
                            onClick = {
                                deletingAPic = null
                            }, colors = ButtonDefaults.buttonColors(
                                Color.Gray, Color.White
                            )
                        ) {
                            Text(text = AnnotatedString("Cancel"))
                        }

                        Button(
                            modifier = Modifier
                                .wrapContentWidth()
                                .padding(horizontal = 10.dp),
                            shape = RoundedCornerShape(10.dp),
                            onClick = {
                                toolDetailViewModel.onToolImageDeleted(it)
                                deletingAPic = null
                            }, colors = ButtonDefaults.buttonColors(
                                Color.Red, Color.White
                            )
                        ) {
                            Text(text = AnnotatedString("I am sure"))
                        }
                    }
                }
            }

        }
    }

    if (takingPics)
        TakePictureOfTool(toolDetailViewModel)

}

@Composable
fun ProgressbarView(toolDetailViewModel: ToolDetailViewModel) {
    val uploadInProgress by toolDetailViewModel.isProcessing.collectAsState()
    val latestProgressMessage by toolDetailViewModel.progressMessage.collectAsState()
    if (uploadInProgress) {
        Box(Modifier
            .fillMaxSize()
            .background(Color.LightGray.copy(alpha = 0.8f))
            .clickable(
                indication = null, // disable ripple effect
                interactionSource = remember { MutableInteractionSource() },
                onClick = { }
            ), // This should prevent the layer under from catching the press event
            contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    modifier = Modifier.wrapContentSize(),
                    color = Color(
                        ContextCompat.getColor(
                            LocalContext.current,
                            R.color.primary
                        )
                    )
                )
                androidx.compose.material.Text(
                    text = latestProgressMessage?: "FUCK",
                    fontWeight = FontWeight.Bold,
                    color = Color(
                        ContextCompat.getColor(
                            LocalContext.current,
                            R.color.primary
                        )
                    )
                )
            }
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
                            detectTransformGestures(onGesture = { _, gesturePan, gestureZoom, _ ->
                                zoom = (zoom * gestureZoom).coerceIn(1F..4F)
                                val newOffset = offset + gesturePan.times(zoom)

                                val maxX = (size.width * (zoom - 1) / 2f)
                                val maxY = (size.height * (zoom - 1) / 2f)

                                offset = Offset(
                                    newOffset.x.coerceIn(-maxX, maxX),
                                    newOffset.y.coerceIn(-maxY, maxY)
                                )
                            })
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

@Composable
fun TakePictureOfTool(toolDetailViewModel: ToolDetailViewModel) {
    val context = LocalContext.current
    lateinit var uri: Uri

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
        if (it) {
            toolDetailViewModel.onToolImageAdded(uri.toString())
        }
        toolDetailViewModel.startCamera(false)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            uri = context.createImageFile()
            cameraLauncher.launch(uri)
        }
    }
    val permissionCheckResult =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
    if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
        uri = context.createImageFile()
        SideEffect {
            cameraLauncher.launch(uri)
        }
    } else {
        // Request a permission
        SideEffect {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}


@Composable
fun UserBorrowRequestButtonAndFavoritesView(user: User?, toolDetailViewModel: ToolDetailViewModel, chosenTool: ToolDetailUiState) {
    LogCompositions("Ehsan", "UserBorrowRequestButtonAndFavoritesView INSIDE")
    var tool_tmp: ToolDetailUiState by remember {
        mutableStateOf(chosenTool)
    }
    var favorites = remember {
        mutableStateListOf<String>().also {
            it.addAll(user?.favoriteTools ?: emptyList())
        }
    }

    val requestSentForThisTool by toolDetailViewModel.requestsReceivedForThisTool.collectAsState()

    val userOwnsThisTool = tool_tmp.owner.id == user?.id
    val toolOwner = tool_tmp.owner
    val toolAvailability: Boolean = userOwnsThisTool || toolOwner.availableAtTheMoment && tool_tmp.isAvailable
    val toolAlpha: Float = if (toolAvailability) 1f else 0.5f
    user?.let { borrowerUser ->
        if (borrowerUser.id != tool_tmp.owner.id) {
            val borrowRequestAvailability = toolAvailability && requestSentForThisTool.any { it.requesterId == borrowerUser.id }.not()
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    enabled = borrowRequestAvailability,
                    modifier = Modifier.alpha(toolAlpha),
                    onClick = {
                        if (borrowRequestAvailability) {
                            tool_tmp = tool_tmp.copy(somethingIsChanging = true)
                            toolDetailViewModel.onRequestToBorrow(borrowerUser, tool_tmp) { // defaultTool is the ToolInApp instance while the rest is the UI states
                                tool_tmp = tool_tmp.copy(somethingIsChanging = false)
                            }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                        Text(if (borrowRequestAvailability) "May I borrow this item?" else "Request pending" )

                        if (tool_tmp.somethingIsChanging)
                            CircularProgressIndicator(modifier = Modifier
                                .height(20.dp)
                                .aspectRatio(1f),
                                color = Color.White
                            )
                    }
                }
                Image(
                    painterResource(if (favorites.contains(tool_tmp.id)) R.drawable.baseline_favorite_24 else R.drawable.baseline_favorite_border_24),
                    contentDescription = "",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .clickable {
                            if (favorites.contains(tool_tmp.id)) { // This should be revised and use the single source of the truth.
                                user.favoriteTools.remove(tool_tmp.id)
                                toolDetailViewModel.onAddToolToUserFavorites(borrowerUser)
                                favorites.remove(tool_tmp.id)
                            } else {
                                user.favoriteTools.add(tool_tmp.id)
                                toolDetailViewModel.onAddToolToUserFavorites(borrowerUser)
                                favorites.add(tool_tmp.id)

                            }
                        }
                        .align(Alignment.CenterVertically)
                )

            }
        }
    }
}