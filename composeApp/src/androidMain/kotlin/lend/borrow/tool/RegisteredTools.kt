package lend.borrow.tool

import ToolInApp
import User
import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getColor
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.serialization.json.Json
import lend.borrow.tool.shared.R
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
    user: User?
) {
    val application = (LocalContext.current as Activity).application
    val toolsViewModel: ToolsViewModel = viewModel{
        ToolsViewModel(application)
    }

    val data by toolsViewModel.data.collectAsState()

    val fetchingToolsInProgress by toolsViewModel.inProgress.collectAsState()

    var iNeedInput by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "I need",
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .padding(horizontal = 10.dp)
        )
        OutlinedTextField(
            value = iNeedInput,
            onValueChange = {
                iNeedInput = it
                if (it.isBlank())
                    toolsViewModel.filterData(iNeedInput)
            },
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(300.dp),
            leadingIcon = {
                IconButton(
                    onClick = {
                        toolsViewModel.filterData(iNeedInput)
                    }
                ) {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = "search")
                }
            }
        )
        if (fetchingToolsInProgress)
            Box(modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .wrapContentSize(),
                    color = Color(getColor(LocalContext.current, R.color.primary))
                )
            }
        else
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(Color.LightGray)
            ) {
                items(data,
                    key = {
                        it.id
                    }) {
                    ListItem(it, user, toolsViewModel)
                }
            }
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
            Log.v("Ehsan", "Request Failed" + e)
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
fun ListItem(
    tool: ToolInApp,
    user: User?,
    toolsViewModel: ToolsViewModel
) {
    var tool_tmp: ToolInApp by remember {
        mutableStateOf(tool)
    }
    var favorites = remember {
        mutableStateListOf<String>()
    }
    favorites.addAll(user?.favoriteTools ?: emptyList())

    val toolOwner = tool.owner
    val toolAvailability: Boolean =
        toolOwner == null || toolOwner.availableAtTheMoment && tool.available
    val toolAlpha: Float = if (toolAvailability) 1f else 0.5f
    Card(
        onClick = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                15.dp
            ),
        colors = CardDefaults.cardColors(Color.White),
        enabled = toolAvailability,
        elevation = CardDefaults.cardElevation(if (tool.available) 5.dp else 0.dp)
    ) {
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
                    items(tool_tmp.imageUrls) {
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
                                    .border(1.dp, Color.LightGray),
                                contentDescription = "",
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                }
            }
            Text(text = "Tool name: ", fontWeight = FontWeight.Bold)
            Text(text = tool.name, modifier = Modifier.padding(5.dp))
            Text(text = "Description: ", fontWeight = FontWeight.Bold)
            Text(text = tool.description, modifier = Modifier.padding(5.dp))
            if (tool.tags?.isNotEmpty() == true)
                Spacer(modifier = Modifier.height(11.dp))
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = 5.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                tool.tags?.forEach { tag ->
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
                        enabled = tool.available,
                        modifier = Modifier.alpha(toolAlpha),
                        onClick = {
                            if (toolAvailability) {
                                tool.available = false
                                tool_tmp = tool
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            Color(
                                LocalContext.current.getColor(
                                    lend.borrow.tool.shared.R.color.primary
                                )
                            ), Color.White
                        ),
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text("May I borrow this item?")
                    }
                    Image(
                        painterResource(if (favorites.contains(tool.id)) R.drawable.baseline_favorite_24 else R.drawable.baseline_favorite_border_24),
                        contentDescription = "",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .clickable {
                                if (favorites.contains(tool.id)) { // This should be revised and use the single source of the truth.
                                    user.favoriteTools.remove(tool.id)
                                    toolsViewModel.updateUserFavoriteTools(it)
                                    favorites.remove(tool.id)
                                } else {
                                    user.favoriteTools.add(tool.id)
                                    toolsViewModel.updateUserFavoriteTools(it)
                                    favorites.add(tool.id)

                                }
                            }
                            .align(Alignment.CenterVertically)
                    )

                }
            }
        }
    }
}

