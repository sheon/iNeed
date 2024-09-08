package lend.borrow.tool

import User
import android.Manifest
import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getColor
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import dev.gitlive.firebase.firestore.GeoPoint
import kotlinx.serialization.json.Json
import lend.borrow.tool.shared.R
import lend.borrow.tool.utility.hasLocationPermission
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
    val toolsViewModel: ToolsViewModel = viewModel{
        ToolsViewModel(application, userId = userId)
    }

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


    ToolsList(toolsViewModel, navController)

}


@Composable
fun ToolsList(toolsViewModel: ToolsViewModel, navController: NavController) {
    val loggedInUser by toolsViewModel.loggedInUser.collectAsState()
    val anythingInProgress by toolsViewModel.anythingInProgress.collectAsState(false)

    Column(
        Modifier
            .fillMaxSize()
    ) {
        if (loggedInUser == null)
            Box(modifier = Modifier
                .padding(10.dp)
                .background(Color.Yellow)
                .padding(10.dp)
            ) {
                Text(
                    text = stringResource(lend.borrow.tool.R.string.guest_warning_message),
                    modifier = Modifier
                        .padding(10.dp),
                    textAlign = TextAlign.Justify,
                    fontStyle = FontStyle.Italic
                )
            }

        TabScreen(toolsViewModel, navController)

    }

    if (anythingInProgress)
        Box(modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier
                    .wrapContentSize(),
                color = Color(getColor(LocalContext.current, R.color.primary))
            )
        }
}

fun Context.getResponseFromAI(question: String, callBack: (List<String>) -> Unit) {
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
            val body = response.body?.string()
            val jsonObject = JSONObject(body)
            val jsonArray: JSONArray = jsonObject.getJSONArray("choices")
            val textResult =
                jsonArray.getJSONObject(0).getJSONObject("message").getString("content")
                    .replace("\\", "")
            val list: List<String> = Json.decodeFromString(textResult)
            callBack(list)
        }

    })

}

@OptIn(ExperimentalLayoutApi::class)
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

    val primaryColor = Color(
        getColor(
            LocalContext.current, lend.borrow.tool.shared.R.color.primary
        )
    )

    SideEffect {
        toolDetailViewModel.initiateViewModel()
    }

    val tool by toolDetailViewModel.tool.collectAsState()
    tool?.let { tool_tmp ->
        var favorites = remember {
            mutableStateListOf<String>()
        }

        favorites.addAll(user?.favoriteTools ?: emptyList())

        val numberOfRequests by toolDetailViewModel.requestsReceivedForThisTool.collectAsState()
        val userOwnsThisTool = tool_tmp.owner.id == user?.id
        val toolOwner = tool_tmp.owner
        val toolAvailability: Boolean = userOwnsThisTool || (toolOwner.availableAtTheMoment && tool_tmp.isAvailable)
        val toolAlpha: Float = if (toolAvailability) 1f else 0.5f

        val iconSize = 30.dp
        val offsetInPx = LocalDensity.current.run { (iconSize / 2).roundToPx() }
        Box(modifier = Modifier
            .padding(iconSize / 2)
            .background(Color.Transparent),
            contentAlignment = Alignment.TopEnd
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clickable(user != null) {
                        if (user != null) {
                            navController.navigate("${BorrowLendAppScreen.TOOL_DETAIL.name}/${tool_tmp.id}")
                        }
                    },
                colors = CardDefaults.cardColors(Color.White),
                elevation = CardDefaults.cardElevation(if (tool_tmp.isAvailable) 5.dp else 0.dp)
            ) {
                Column(
                    Modifier
                        .padding(5.dp)
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    AnimatedVisibility(true) {
                        LazyRow(
                            Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            items(tool_tmp.imageUrlsRefMap.keys.toList(),
                                key = {
                                    it
                                }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = it, // This should be changed to a UiState data class as in ToolDetailScreen
                                        modifier = Modifier
                                            .alpha(toolAlpha)
                                            .border(1.dp, Color.LightGray),
                                        contentDescription = "",
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }

                        }
                    }
                    Text(text = "Tool name: ", fontWeight = FontWeight.Bold)
                    Text(text = tool_tmp.name, modifier = Modifier.padding(5.dp))
                    Text(text = "Description: ", fontWeight = FontWeight.Bold)
                    Text(text = tool_tmp.description,  maxLines = 3,modifier = Modifier.padding(5.dp), overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Justify)
                    if (tool_tmp.tags?.isNotEmpty() == true)
                        Spacer(modifier = Modifier.height(11.dp))
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(top = 5.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        tool_tmp.tags?.replace(" ", "")?.split(",")?.forEach { tag ->
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

                    UserBorrowRequestButtonAndFavoritesView(user, toolDetailViewModel, tool_tmp)

                }
            }
            if (numberOfRequests.isNotEmpty())
                Box(modifier = Modifier
                    .offset {
                        IntOffset(x = -offsetInPx, y = -offsetInPx)
                    }
                    .shadow(4.dp, shape = CircleShape)
                    .background(Color.Yellow, CircleShape)
//                    .border(
//                        BorderStroke(0.5.dp, Color(getColor(LocalContext.current, R.color.primary))),
//                        CircleShape
//                    )
                    .size(iconSize),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = numberOfRequests.size.toString(), color = primaryColor, fontWeight = FontWeight.Bold)
                }
        }
    }




}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TabScreen(toolsViewModel: ToolsViewModel, navController: NavController)   {
    val data by toolsViewModel.toolListAroundUser.collectAsState()
    val loggedInUser by toolsViewModel.loggedInUser.collectAsState()
    val fetchingToolsInProgress by toolsViewModel.fetchingToolsInProgress.collectAsState()

    var iNeedInput by rememberSaveable { mutableStateOf("") }

    var tabIndex by rememberSaveable { mutableStateOf(0) }

    val pullRefreshState = rememberPullRefreshState(fetchingToolsInProgress, {
        toolsViewModel.refreshData(tabIndex == 1)
    })

    val tabs = listOf("Others", "Yours")

    Column(modifier = Modifier.fillMaxWidth()) {
        loggedInUser?.let {
            TabRow(
                selectedTabIndex = tabIndex,
                backgroundColor = Color.White,
                indicator = { tabPositions ->
                    if (tabIndex < tabPositions.size) {
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                            color = Color(getColor(LocalContext.current, R.color.primary))
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(text = { Text(title) },
                        selected = tabIndex == index,
                        selectedContentColor = Color.Yellow,
                        onClick = {
                            if(tabIndex != index) {
                                toolsViewModel.getToolsFromRemote(isOwnerOfTools = index == 1)
                                tabIndex = index
                            }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.size(10.dp))
        }

        val backgroundColor = Color(LocalContext.current.resources.getColor(R.color.primary))


        OutlinedTextField(
            value = iNeedInput, onValueChange = {
                iNeedInput = it
                if (it.isBlank())
                    toolsViewModel.filterData(iNeedInput)
            }, modifier = Modifier
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
                unfocusedIndicatorColor = backgroundColor.copy(alpha = 0.3f),
                unfocusedContainerColor = backgroundColor.copy(alpha = 0.2f),
                focusedContainerColor = backgroundColor.copy(alpha = 0.2f),
                unfocusedTextColor = backgroundColor,
                focusedTextColor = backgroundColor,
                focusedLabelColor = backgroundColor,
                unfocusedLabelColor = backgroundColor,
                unfocusedTrailingIconColor = backgroundColor,
                focusedTrailingIconColor = backgroundColor
            )
        )

        Spacer(modifier = Modifier.size(10.dp))

        Spacer(modifier = Modifier
            .height(1.dp)
            .fillMaxWidth()
            .background(backgroundColor))

        LazyColumn(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .pullRefresh(pullRefreshState)
                .background(backgroundColor.copy(alpha = 0.2f)),
            contentPadding = PaddingValues(top = 10.dp)
        ) {
            items(data,
                key = {
                    it.id
                }) {item ->
                ToolInfoCard(item.id, loggedInUser, navController)
            }
        }
    }
}



