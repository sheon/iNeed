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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import lend.borrow.tool.R.drawable
import lend.borrow.tool.R.string
import lend.borrow.tool.shared.R
import lend.borrow.tool.utility.CustomButton
import lend.borrow.tool.utility.DropDownMenu
import lend.borrow.tool.utility.GenericWarningDialog
import lend.borrow.tool.utility.LogCompositions
import lend.borrow.tool.utility.WarningButton
import lend.borrow.tool.utility.primaryColor
import lend.borrow.tool.utility.secondaryColor

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun ToolDetailScreen(toolId: String, user: User? = null, navController: NavController) {
    val application = (LocalContext.current as Activity).application
    val toolDetailViewModel: ToolDetailViewModel = viewModel(key = toolId) {
        ToolDetailViewModel(application, toolId, user?.id)
    }
    LaunchedEffect(Unit) {
        toolDetailViewModel.initiateViewModel()
    }
    ToolDetailContent(toolDetailViewModel = toolDetailViewModel, user = user, navController)
}

@Composable
fun ToolDetailContent(toolDetailViewModel: ToolDetailViewModel, user: User?, navController: NavController) {
    val tool by toolDetailViewModel.tool.collectAsState(null)
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

@Composable
fun StaticToolInfoScreen(chosenTool: ToolDetailUiState, user: User?, toolDetailViewModel: ToolDetailViewModel, userOwnThisTool: Boolean = false, showAllDetail: Boolean = true) {
    var zoomIn: String? by remember {
        mutableStateOf(null)
    }
    ToolDetailContent(Modifier.padding(horizontal = 10.dp), fetchedTool = chosenTool, user = user, toolDetailViewModel = toolDetailViewModel) {
        zoomIn = it
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
    var deletingTool: Boolean by remember {
        mutableStateOf(false)
    }
    val numberOfImagesForTool = 5
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberLazyListState()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp)
            .verticalScroll(verticalScrollState)
    ) {
        Spacer(Modifier.size(10.dp))
        AnimatedVisibility(true) {
            Box(contentAlignment = Alignment.Center) {
                LazyRow(
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 10.dp),
                    state = horizontalScrollState
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
                                        placeholder = painterResource(id = drawable.baseline_image_24),
                                        fallback = painterResource(id = drawable.baseline_image_24),
                                        contentDescription = "",
                                        contentScale = ContentScale.FillBounds
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
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    if (horizontalScrollState.canScrollBackward)
                        Box(
                            modifier = Modifier
                                .wrapContentSize()
                                .shadow(5.dp, shape = CircleShape)
                                .background(Color.White, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                contentDescription = "Scroll to left"
                            )
                        }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (horizontalScrollState.canScrollForward)
                        Box(
                            modifier = Modifier
                                .wrapContentSize()
                                .shadow(5.dp, shape = CircleShape)
                                .background(Color.White, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = "Scroll to right"
                            )
                        }
                }
            }

        }

        Spacer(modifier = Modifier.size(10.dp))

        Text("Tool name", fontWeight = FontWeight.W600, modifier = Modifier.padding(5.dp))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = ownerToolDetailUi.name,
            onValueChange = {
                if (it != ownerToolDetailUi.name)
                    toolDetailViewModel.onToolNameChanged(it)
            }

        )

        Spacer(modifier = Modifier.size(10.dp))
        Text("Tool description", fontWeight = FontWeight.W600, modifier = Modifier.padding(5.dp))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = ownerToolDetailUi.description?: "",
            placeholder = {
                Text("Please describe the tool")
            },
            onValueChange = {
                if (it != ownerToolDetailUi.description)
                    toolDetailViewModel.onToolDescriptionChanged(it)
            }
        )
        Spacer(modifier = Modifier.height(11.dp))
        Text("Tool instruction", fontWeight = FontWeight.W600, modifier = Modifier.padding(5.dp))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = ownerToolDetailUi.instruction?:"",
            placeholder = {
                Text("Add instruction on how to use this tool")
            },
            onValueChange = {
                if (it != ownerToolDetailUi.instruction)
                    toolDetailViewModel.onToolInstructionChanged(it)
            }
        )

        Spacer(modifier = Modifier.height(11.dp))
        Text("Tags", fontWeight = FontWeight.W600, modifier = Modifier.padding(5.dp))
        TextFieldContent(
            listOfChips = ownerToolDetailUi.tags,
            toolDetailViewModel
        ) { tags ->
            toolDetailViewModel.onToolTagChanged(tags)
        }



        Spacer(modifier = Modifier.height(32.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CustomButton(
                modifier = Modifier.fillMaxWidth(),
                "Save changes",
                onClick = {
                    toolDetailViewModel.saveChangesInToolDetail()
                },
                filled = true
            )

            CustomButton(
                modifier = Modifier.fillMaxWidth(),
                "Discard changes",
                onClick = {
                    toolDetailViewModel.discardChangesInToolDetail(chosenTool)
                },
                color = Color.Gray
            )

            WarningButton(
                modifier = Modifier.fillMaxWidth(),
                "Delete this tool",
                onClick = {
                    deletingTool = true
                }
            )
        }
        Spacer(Modifier.size(10.dp))
    }
    zoomIn?.let {
        ZoomInImage(it) {
            zoomIn = null
        }
    }
    deletingAPic?.let {
        GenericWarningDialog(message = "Are you sure you want to delete this picture?",
            positiveText = stringResource(string.yes_i_am_sure),
            onPositiveClick = {
            toolDetailViewModel.onToolImageDeleted(it)
            deletingAPic = null
        }, onNegativeClick = { deletingAPic = null })
    }

    if (takingPics)
        TakePictureOfTool(toolDetailViewModel)
    if (deletingTool)
        GenericWarningDialog(
            message = "Are you sure you want to delete this tool?",
            positiveText = stringResource(string.yes_i_am_sure),
            onPositiveClick = {
                toolDetailViewModel.deleteTool(ownerToolDetailUi)
                deletingTool = false
            },
            onNegativeClick = {
                deletingTool = false
            }
        )
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


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolDetailContent(modifier: Modifier = Modifier, fetchedTool: ToolDetailUiState, user: User?, toolDetailViewModel: ToolDetailViewModel, showAllDetail: Boolean = true, zoomCallback: (String) -> Unit = {}) {
    val favorites = remember {
        mutableStateListOf<String>().also {
            it.addAll(user?.favoriteTools ?: emptyList())
        }
    }
    val userOwnsThisTool = fetchedTool.owner.id == user?.id
    val toolOwner = fetchedTool.owner
    val toolAvailability: Boolean =
        userOwnsThisTool || (toolOwner.availableAtTheMoment && fetchedTool.isAvailable)
    val toolAlpha: Float = if (toolAvailability) 1f else 0.5f
    Column(
        modifier
            .padding(5.dp)
            .fillMaxWidth()
    ) {
        if (showAllDetail)
            Text(text = "Tool_ID: ${fetchedTool.id} ", fontStyle = FontStyle.Italic, color = Color.LightGray, fontSize = 9.sp)
        val horizontalScrollState = rememberLazyListState()
        AnimatedVisibility(true) {
            Box(Modifier
                .fillMaxWidth(),
                contentAlignment = Alignment.Center) {
                LazyRow(
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            LocalContext.current.primaryColor.copy(alpha = 0.1f),
                            RoundedCornerShape(5.dp)
                        )
                        .padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.Center,
                    state = horizontalScrollState
                ) {
                    items(fetchedTool.imageUrlsRefMap.keys.toList(),
                        key = {
                            it
                        }) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = it, // This should be changed to a UiState data class as in ToolDetailScreen
                                modifier = Modifier
                                    .alpha(toolAlpha)
                                    .clickable(showAllDetail) {
                                        zoomCallback(it)
                                    }
                                    .border(1.dp, Color.LightGray),
                                contentDescription = "",
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .offset {
                            IntOffset(x = -30, y = 0)
                        },
                    horizontalArrangement = Arrangement.Start
                ) {
                    if (horizontalScrollState.canScrollBackward)
                        Box(
                            modifier = Modifier
                                .wrapContentSize()
                                .shadow(5.dp, shape = CircleShape)
                                .background(Color.White, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                contentDescription = "Scroll to left"
                            )
                        }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .offset {
                            IntOffset(x = 30, y = 0)
                        },
                    horizontalArrangement = Arrangement.End
                ) {
                    if (horizontalScrollState.canScrollForward)
                        Box(
                            modifier = Modifier
                                .wrapContentSize()
                                .shadow(5.dp, shape = CircleShape)
                                .background(Color.White, CircleShape)
                        ) {
                           Icon(
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = "Scroll to right"
                            )
                        }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Spacer(Modifier.size(10.dp))
                Text(text = "Tool name", fontWeight = FontWeight.Bold)
                Text(text = fetchedTool.name, modifier = Modifier.padding(5.dp))
            }
            user?.let { loggedInUser ->
                if (fetchedTool.owner.id != loggedInUser.id)
                    Image(
                        painterResource(if (favorites.contains(fetchedTool.id)) drawable.baseline_bookmark_24 else drawable.baseline_bookmark_border_24),
                        contentDescription = "",
                        modifier = Modifier
                            .clickable {
                                if (favorites.contains(fetchedTool.id)) { // This should be revised and use the single source of the truth.
                                    loggedInUser.favoriteTools.remove(fetchedTool.id)
                                    toolDetailViewModel.onAddToolToUserFavorites(loggedInUser)
                                    favorites.remove(fetchedTool.id)
                                } else {
                                    loggedInUser.favoriteTools.add(fetchedTool.id)
                                    toolDetailViewModel.onAddToolToUserFavorites(loggedInUser)
                                    favorites.add(fetchedTool.id)
                                }
                            }
                    )
                }
        }

        Spacer(Modifier.size(5.dp))
        Text(text = "Description", fontWeight = FontWeight.Bold)
        Text(
            text = fetchedTool.description?: LocalContext.current.getString(string.no_description),
            modifier = Modifier.padding(5.dp),
            textAlign = TextAlign.Justify
        )
        if(showAllDetail) {
            Spacer(Modifier.size(5.dp))
            Text(text = "Instruction", fontWeight = FontWeight.Bold)
            Text(text = fetchedTool.instruction?: LocalContext.current.getString(string.no_instruction),
                modifier = Modifier.padding(5.dp),
            textAlign = TextAlign.Justify
            )
        }
        if (fetchedTool.tags.isNotEmpty())
            Spacer(modifier = Modifier.height(11.dp))
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(top = 5.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            fetchedTool.tags.forEach { tag ->
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .background(
                            color = LocalContext.current.secondaryColor,
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
                        color = LocalContext.current.primaryColor
                    )
                }
            }
        }

        if (user != null)
            UserBorrowRequestButtonAndFavoritesView(user, toolDetailViewModel, fetchedTool)

    }
}

@Composable
fun UserBorrowRequestButtonAndFavoritesView(
    borrowerUser: User,
    toolDetailViewModel: ToolDetailViewModel,
    chosenTool: ToolDetailUiState
) {
    var tool_tmp: ToolDetailUiState by remember {
        mutableStateOf(chosenTool)
    }

    val requestSentForThisTool by toolDetailViewModel.requestsReceivedForThisTool.collectAsState()

    val userOwnsThisTool = tool_tmp.owner.id == borrowerUser.id
    val toolOwner = tool_tmp.owner
    val toolAvailability: Boolean =
        userOwnsThisTool || toolOwner.availableAtTheMoment && tool_tmp.isAvailable
    val toolAlpha: Float = if (toolAvailability) 1f else 0.5f
    if (borrowerUser.id != tool_tmp.owner.id) {
        val borrowRequestAvailability =
            requestSentForThisTool.any { it.requesterId == borrowerUser.id }.not()
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                enabled = borrowRequestAvailability && toolAvailability,
                modifier = Modifier.alpha(toolAlpha),
                onClick = {
                    tool_tmp = tool_tmp.copy(somethingIsChanging = true)
                    toolDetailViewModel.onRequestToBorrow(
                        borrowerUser,
                        tool_tmp
                    ) { // defaultTool is the ToolInApp instance while the rest is the UI states
                        tool_tmp = tool_tmp.copy(somethingIsChanging = false)
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
                    Text(if (borrowRequestAvailability) "May I borrow this item?" else "Request pending")

                    if (tool_tmp.somethingIsChanging)
                        CircularProgressIndicator(
                            modifier = Modifier
                                .height(20.dp)
                                .aspectRatio(1f),
                            color = Color.White
                        )
                    else if (borrowRequestAvailability)
                        Icon(
                            painter = painterResource(R.drawable.send_icon),
                            contentDescription = "",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )

                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextFieldContent(
    listOfChips: List<String>,
    toolDetailViewModel: ToolDetailViewModel,
    onChipClick: (List<String>) -> Unit
) {
    LogCompositions("Ehsan", "TextFieldContent")
    var input: String? by remember {
        mutableStateOf("")
    }
    val focusRequester = remember { FocusRequester() }
    val keyboardManager = LocalSoftwareKeyboardController.current

    Box(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .border(1.dp, Color.Gray, RoundedCornerShape(5.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clickable(
                indication = null ,
                interactionSource = remember { MutableInteractionSource() },
            ) {
                focusRequester.requestFocus()
                keyboardManager?.show()
            }
            ,
        contentAlignment = Alignment.CenterStart) {
        FlowRow(
            modifier = Modifier
                .wrapContentHeight()
                .wrapContentWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            var removeLastChipIndex by remember {
                mutableStateOf(false)
            }
            repeat(times = listOfChips.size) { index ->
                InputChip(
                    onClick = {
                        val tmpTagList = listOfChips.toMutableList()
                        tmpTagList.removeAt(index)
                        onChipClick(tmpTagList)
                    },
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(horizontal = 2.dp),
                    trailingIcon = {
                            Icon(
                                painter = rememberVectorPainter(image = Icons.Default.Close),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                    },
                    label = {
                        Text(listOfChips[index])
                    },
                    colors = InputChipDefaults
                        .inputChipColors(
                            labelColor = LocalContext.current.primaryColor,
                            containerColor = LocalContext.current.secondaryColor
                        ),
                    selected = false
                )
            }
            BasicTextField(
                modifier = Modifier
                    .padding(start = 5.dp)
                    .width(IntrinsicSize.Min)
                    .fillMaxHeight()
                    .focusRequester(focusRequester)
                    .onKeyEvent {
                        if (it.key == Key.Backspace) {
                            if(input == "" && !removeLastChipIndex && listOfChips.isNotEmpty()) {
                                removeLastChipIndex = true
                            } else if(input == "") {
                                removeLastChipIndex = false
                                val tmpTagList = listOfChips.toMutableList()
                                toolDetailViewModel.onToolTagChanged(tmpTagList.dropLast(1))
                            }
                            true
                        } else {
                            false
                        }
                                },
                value = input ?: "",
                singleLine = false,
                onValueChange = {
                    input = it
                },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .wrapContentWidth()
                            .defaultMinSize(minHeight = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 20.dp)
                                    .wrapContentWidth(),
                            ) {
                                innerTextField()
                            }
                        }
                    }
                },
                keyboardActions = KeyboardActions(
                    onDone = {
                        if(input?.isEmpty() != true && input != null) {
                            val tmpTagList = listOfChips.toMutableList()
                            tmpTagList.add(input!!)
                            toolDetailViewModel.onToolTagChanged(tmpTagList)
                            input = ""
                        }
                    }
                )
            )
        }
    }
}