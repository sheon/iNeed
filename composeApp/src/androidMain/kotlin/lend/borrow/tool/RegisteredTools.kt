package lend.borrow.tool

import ToolDownloadedFromFireBase
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import user1
import java.io.IOException


@Composable
fun RegisteredToolsScreen(modifier: Modifier = Modifier) {

    val context = LocalContext.current
    val toolsViewModel = ToolsViewModel((context as Activity).application)

    var _data = mutableListOf<ToolDownloadedFromFireBase>()
    var data: List<ToolDownloadedFromFireBase> by remember {
        mutableStateOf(mutableListOf())
    }
    toolsViewModel.getToolsFromRemote() {
        _data.addAll(it)
        data = _data
    }
    var iNeedInput by rememberSaveable { mutableStateOf("") }
    Column(
        modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "I need",
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .padding(horizontal = 10.dp)
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .border(2.dp, Color.Gray, shape = RoundedCornerShape(300.dp))
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Row {
                Image(
                    painterResource(R.drawable.magnify),
                    contentDescription = "",
                    modifier = Modifier
                        .clickable {
                            if (iNeedInput.isNotBlank()) {
                                if (iNeedInput.split(" ").size == 1) {
                                    data = _data.filter {
                                        it.name.equals(iNeedInput, true)
                                    }
                                } else
                                    context.getResponseFromAI("what do I need " + iNeedInput + "? send the list of tools name in a kotlin list of strings in one line.") {
                                        Log.v("Ehsan", "Tools are: $it")
                                        val tempList = mutableListOf<ToolDownloadedFromFireBase>()
                                        it.forEach { requiredTool ->
                                            tempList.addAll(_data.filter { availableTool ->
                                                availableTool.name
                                                    .replace(" ", "")
                                                    .equals(requiredTool.replace(" ", ""), true)
                                            })
                                        }
                                        data = tempList
                                    }

                            } else
                                data = _data
                        }
                        .align(Alignment.CenterVertically)
                )
                TextField(
                    value = iNeedInput,
                    onValueChange = {
                        iNeedInput = it
                        if (it.isBlank())
                            data = _data
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent)
                )
            }

        }
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(Color.LightGray)
        ) {
            items(count = data.size,
                key = {
                    data[it].id
                },
                itemContent = { index ->
                    ListItem(data[index])
                })
        }
    }


}

fun Context.getResponseFromAI(question: String, callBack: (List<String>) -> Unit) {
    val apiKey = getString(lend.borrow.tool.R.string.api_key)
    val url = "https://api.openai.com/v1/chat/completions"
    val client = OkHttpClient()
    Log.v("Ehsan", "question: $question")
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
            Log.v("Ehsan", "response: ${body}")
            val jsonObject = JSONObject(body)
            val jsonArray: JSONArray = jsonObject.getJSONArray("choices")
            Log.v("Ehsan", "choices: ${jsonArray}")
            val textResult =
                jsonArray.getJSONObject(0).getJSONObject("message").getString("content")
                    .replace("\\", "")
            val list: List<String> = Json.decodeFromString(textResult)
            Log.v("Ehsan", "result: ${list}")
            callBack(list)
        }

    })

}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListItem(tool: ToolDownloadedFromFireBase, modifier: Modifier = Modifier) {
    var tool_tmp: ToolDownloadedFromFireBase by remember {
        mutableStateOf(tool)
    }

    val toolAvailability: Float by remember {
        mutableFloatStateOf(if (tool.available) 1f else 0.5f)
    }
    Card(
        modifier
            .fillMaxWidth()
            .padding(
                15.dp
            )
            .alpha(toolAvailability)
            .clickable(tool.available, onClick = {}),
        elevation = if (tool.available) 5.dp else 0.dp
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
                    items(tool_tmp.images) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = it,
                                modifier = Modifier.border(1.dp, Color.LightGray),
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
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(top = 5.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    tool.tags?.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .wrapContentWidth()
                                .background(
                                    color = Color.Black,
                                    shape = RoundedCornerShape(5.dp)
                                )
                                .alpha(toolAvailability),
                            Alignment.Center
                        ) {
                            Text(
                                text = tag.trim(),
                                Modifier.padding(horizontal = 5.dp),
                                color = Color.White
                            )
                        }
                    }
                }
            var isFavorite: Boolean by remember {
                mutableStateOf(user1.favoriteTools.contains(tool.id))
            }
            Row(
                modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(enabled = tool.available,
                    modifier = Modifier.alpha(toolAvailability),
                    onClick = {
                        tool.available = false
                        tool_tmp = tool
                    }) {
                    Text("May I borrow this item?")
                }
                Image(
                    painterResource(if (isFavorite) R.drawable.baseline_favorite_24 else R.drawable.baseline_favorite_border_24),
                    contentDescription = "",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .clickable {
                            if (isFavorite) user1.favoriteTools.remove(tool.id) else user1.favoriteTools.add(
                                tool.id
                            )
                        }
                        .align(Alignment.CenterVertically)
                        .alpha(toolAvailability),
                )

            }

        }

    }
}

