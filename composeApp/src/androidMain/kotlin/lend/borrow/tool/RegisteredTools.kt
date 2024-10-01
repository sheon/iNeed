package lend.borrow.tool

import User
import android.Manifest
import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getColor
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import dev.gitlive.firebase.firestore.GeoPoint
import kotlinx.serialization.json.Json
import lend.borrow.tool.shared.R
import lend.borrow.tool.utility.hasLocationPermission
import lend.borrow.tool.utility.primaryColor
import lend.borrow.tool.utility.secondaryColor
import lend.borrow.tool.utility.shimmerBrush
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException


@Composable
fun RegisteredToolsScreen(
    userId: String?,
    navController: NavController
) {
    val application = (LocalContext.current as Activity).application
    val toolsViewModel: ToolsViewModel = viewModel {
        ToolsViewModel(application, userId = userId)
    }



    RegisteredToolsContent(toolsViewModel) {
        TabScreen(toolsViewModel, navController)
    }

}


@Composable
fun RegisteredToolsContent(
    toolsViewModel: ToolsViewModel,
    tabView: @Composable () -> Unit
) {
    val application = (LocalContext.current as Activity).application
    val loggedInUser by toolsViewModel.loggedInUser.collectAsState()

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())
        { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted, update the location
                toolsViewModel.getAnonymousUserLocation(application) { lat, long ->
                    toolsViewModel.getToolsFromRemote(GeoPoint(lat, long))
                }
            }
        }

    if (loggedInUser == null && toolsViewModel.anonymousUserLocation == null )
        if (hasLocationPermission(application)) {
            toolsViewModel.getAnonymousUserLocation(application) { lat, long ->
                toolsViewModel.getToolsFromRemote(GeoPoint(lat, long))
            }
        } else {
            SideEffect {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

    Column(
        Modifier
            .fillMaxSize()
    ) {
        if (loggedInUser == null) {
            var warningIsShowing by remember {
                mutableStateOf(true)
            }
            val height = animateDpAsState(
                targetValue = if (warningIsShowing) 200.dp else 20.dp,
                animationSpec = tween(
                    500,
                    easing = LinearEasing
                ),
                label = "height"
            )
            Box(
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .sizeIn(minHeight = 0.dp, maxHeight = height.value)
                        .padding(10.dp)
                        .background(Color.Yellow)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(lend.borrow.tool.R.string.guest_warning_message),
                        modifier = Modifier
                            .padding(10.dp),
                        textAlign = TextAlign.Justify,
                        fontStyle = FontStyle.Italic
                    )
                }
                IconButton(onClick = {
                    warningIsShowing = !warningIsShowing
                }) {
                    Icon(
                        imageVector = if (warningIsShowing) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = ""
                    )
                }
            }
        }

        tabView()

    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TabScreen(toolsViewModel: ToolsViewModel, navController: NavController)   {
    val loggedInUser by toolsViewModel.loggedInUser.collectAsState()
    val fetchingToolsInProgress by toolsViewModel.fetchingToolsInProgress.collectAsState()
    val errorMessage: String? by toolsViewModel.latestErrorMessage.collectAsState(null)

    var iNeedInput by rememberSaveable { mutableStateOf("") }

    var tabIndex by rememberSaveable { mutableStateOf(0) }

    val pullRefreshState = rememberPullRefreshState(fetchingToolsInProgress, {
        toolsViewModel.refreshData()
    })


    val tabs = listOf("Others", "Yours")

    val toolsToShowForChosenTab by toolsViewModel.toolsToShowForChosenTab.collectAsState()
    Column(modifier = Modifier.fillMaxWidth()) {
        loggedInUser?.let {
            TabRow(
                selectedTabIndex = tabIndex,
                backgroundColor = Color.White,
                indicator = { tabPositions ->
                    if (tabIndex < tabPositions.size) {
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                            color = LocalContext.current.primaryColor
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(text = { Text(title) },
                        selected = tabIndex == index,
                        selectedContentColor = LocalContext.current.secondaryColor,
                        onClick = {
                            if(tabIndex != index) {
                                tabIndex = index
                                toolsViewModel.filterDataForTab(tabIndex = index)
                            }
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.size(10.dp))

        val backgroundColor = LocalContext.current.primaryColor


        OutlinedTextField(
            value = iNeedInput,
            onValueChange = {
                iNeedInput = it
                if (it.isBlank())
                    toolsViewModel.filterData(iNeedInput)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(300.dp),
            trailingIcon = {
                IconButton(
                    onClick = {
                        toolsViewModel.filterData(iNeedInput)
                    }
                ) {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = "search")
                }
            },
            label = {
                Text(text = "I need")
            },
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = backgroundColor,
                unfocusedIndicatorColor = backgroundColor.copy(alpha = 0.2f),
                unfocusedContainerColor = backgroundColor.copy(alpha = 0.1f),
                focusedContainerColor = backgroundColor.copy(alpha = 0.1f),
                unfocusedTextColor = backgroundColor,
                focusedTextColor = backgroundColor,
                focusedLabelColor = backgroundColor,
                unfocusedLabelColor = backgroundColor,
                unfocusedTrailingIconColor = backgroundColor,
                focusedTrailingIconColor = backgroundColor
            )
        )

        errorMessage?.let {
            Text(it, color = Color.Red, modifier = Modifier.padding(20.dp))
        }

        Spacer(modifier = Modifier.size(10.dp))

        Spacer(modifier = Modifier
            .height(1.dp)
            .fillMaxWidth()
            .background(Color.LightGray))

        Box(modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .pullRefresh(pullRefreshState)
            .background(backgroundColor.copy(alpha = 0.1f))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 5.dp)
            ) {
                items(toolsToShowForChosenTab,
                    key = {
                        it.id
                    }) {item ->
                    ToolInfoCard(item.id, loggedInUser, navController)
                }
            }
            PullRefreshIndicator(fetchingToolsInProgress, pullRefreshState, Modifier.align(Alignment.TopCenter))
        }

    }
}


@Composable
fun ToolInfoCard(
    toolId: String,
    user: User?,
    navController: NavController
) {

    val application = (LocalContext.current as Activity).application
    val toolDetailViewModel = viewModel(key = toolId) {
        ToolDetailViewModel(application, toolId = toolId, userId = user?.id)
    }
    val tool by toolDetailViewModel.tool.collectAsState()
    if (toolDetailViewModel.toolNeedUpdate)
        toolDetailViewModel.initiateViewModel()
    val primaryColor = Color(
        getColor(
            LocalContext.current, R.color.primary
        )
    )

    val inProgress by toolDetailViewModel.isProcessing.collectAsState()

    val numberOfRequests by toolDetailViewModel.requestsReceivedForThisTool.collectAsState()

    if (inProgress) {
        ShimmeringCard()
    } else
        tool?.let { fetchedTool ->
            val iconSize = 30.dp
            val offsetInPx = LocalDensity.current.run { (iconSize / 2).roundToPx() }
            Box(
                modifier = Modifier
                    .padding(iconSize / 2)
                    .background(Color.Transparent),
                contentAlignment = Alignment.TopEnd
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .shadow(
                            if (fetchedTool.isAvailable) 10.dp else 0.dp,
                            RoundedCornerShape(15.dp)
                        )
                        .clip(RoundedCornerShape(15.dp))
                        .clickable(user != null, onClick = {
                            if (user != null) {
                                navController.navigate("${BorrowLendAppScreen.TOOL_DETAIL.name}/${fetchedTool.id}")
                            }
                        }),
                    colors = CardDefaults.cardColors(Color.White),
                ) {
                    ToolDetailContent(Modifier.padding(20.dp), fetchedTool, user, toolDetailViewModel, false)
                }
                if (numberOfRequests.isNotEmpty())
                    Box(modifier = Modifier
                        .offset {
                            IntOffset(x = -offsetInPx, y = -offsetInPx)
                        }
                        .size(iconSize),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Image(
                            painter = painterResource(R.drawable.send_icon),
                            contentDescription = ""
                        )
                        Box(
                            modifier = Modifier
                                .shadow(4.dp, shape = CircleShape)
                                .background(
                                    Color(LocalContext.current.getColor(R.color.secondary)),
                                    CircleShape
                                )
                                .size(iconSize / 2),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = numberOfRequests.size.toString(),
                                color = primaryColor
                            )
                        }
                    }
            }
        }
}



@Composable
fun ShimmeringCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    shimmerBrush(targetValue = 1300f, showShimmer = true),
                    RoundedCornerShape(2.dp)
                )
        ) {
            Spacer(modifier = Modifier.size(50.dp))
        }
    }
}

fun Context.getResponseFromAI(question: String, onSuccessCallBack: (List<String>) -> Unit, onFailureCallback: () -> Unit) {
    val apiKey = getString(lend.borrow.tool.R.string.api_key)
    val url = "https://api.openai.com/v1/chat/completions"
    val client = OkHttpClient()
    val requestBody =
        """{"model":"gpt-3.5-turbo","messages":[{"role":"user","content":"$question"}]}"""

    val request = Request.Builder()
        .url(url)
        .addHeader("Content-Type", "application/json")
        .addHeader("Authorization", "Bearer $apiKey")
        .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.v(this@getResponseFromAI.javaClass.name, "Request Failed" + e)
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful)
                response.body?.string()?.let { body ->
                    val jsonObject = JSONObject(body)
                    val jsonArray: JSONArray = jsonObject.getJSONArray("choices")
                    val textResult =
                        jsonArray.getJSONObject(0).getJSONObject("message").getString("content")
                            .replace("\\", "")
                    val list: List<String> = Json.decodeFromString(textResult)
                    onSuccessCallBack(list)
                }
            else
                onFailureCallback()
        }

    })

}