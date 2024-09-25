package lend.borrow.tool.requests

import User
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getColor

@Composable
fun RequestsScreen(toolId: String?, loggedInUser: User, isOwnerOfTool: Boolean?) {
    val application = (LocalContext.current as Activity).application
    val requestsViewModel: RequestsViewModel =
        if (isOwnerOfTool == true)
            RequestsViewModel(application, owner = loggedInUser, toolId = toolId)
        else
            RequestsViewModel(application, requester = loggedInUser, toolId = toolId)

    RequestsScreenContent(requestsViewModel)
}

@Composable
fun RequestsScreenContent(requestsViewModel: RequestsViewModel) {

    val requests by requestsViewModel.requestsForThisUser.collectAsState()


    LazyColumn {
        items(requests.values.toList(),
            key = { item ->
                item.initialRequest.requestId
            }) { item ->

            when {
                requestsViewModel.owner != null -> ReceivedRequestView(item, requestsViewModel)
                requestsViewModel.requester != null -> SentRequestView(item)
            }
        }
    }

}


@Composable
fun ReceivedRequestView(request: BorrowRequestUiState, requestsViewModel: RequestsViewModel) {
    val primaryColor = Color(
        getColor(
            LocalContext.current, lend.borrow.tool.shared.R.color.primary
        )
    )
    val backgroundColor = if (request.isRead) Color.White else primaryColor.copy(alpha = 0.2f)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.size(10.dp))
        Text(text = "${request.borrower.name} wants to borrow your ${request.tool.name}")
        Spacer(modifier = Modifier.size(10.dp))
        when (request.isAccepted) {
            null -> {
                Button(
                    onClick = {
                        requestsViewModel.onRequestAccepted(true, request)
                    },
                    colors = ButtonDefaults.buttonColors(
                        primaryColor, Color.White
                    )
                ) {
                    Text(text = "Accept")
                }
                Button(
                    onClick = {
                        requestsViewModel.onRequestAccepted(false, request)
                    },
                    colors = ButtonDefaults.buttonColors(Color.Red, Color.White)
                ) {
                    Text(text = "Reject")
                }
            }

            false -> {
                Button(
                    enabled = false,
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(Color.Red, Color.White)
                ) {
                    Text(text = "Rejected")
                }
            }

            true -> {
                Button(
                    onClick = {
                    },
                    colors = ButtonDefaults.buttonColors(
                        primaryColor, Color.White
                    )
                ) {
                    Text(text = "Go to conversation")
                }
            }
        }
        Spacer(modifier = Modifier.size(10.dp))
    }
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(primaryColor)
    )
    if (!request.isRead)
        requestsViewModel.onRequestReadUpdated(request)
}

@Composable
fun SentRequestView(request: BorrowRequestUiState) {
    val primaryColor = Color(
        getColor(
            LocalContext.current, lend.borrow.tool.shared.R.color.primary
        )
    )

    Box(
        Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp
            )
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.size(10.dp))
            Text(text = "Request sent to ${request.tool.owner.name}")
            Spacer(modifier = Modifier.size(10.dp))
            Text(text = "to borrow his ${request.tool.name}")
            Spacer(modifier = Modifier.size(10.dp))
            Button(
                enabled = request.isAccepted == true,
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    primaryColor, Color.White
                )
            ) {
                when (request.isAccepted) {
                    null -> {
                        if (request.isRead)
                            Text(text = "Seen")
                        else
                            Text(text = "Not seen")
                    }

                    false -> {
                        Text(text = "Rejected")
                    }

                    true -> {
                        Text(text = "Go to conversation")
                    }
                }
            }
            Spacer(modifier = Modifier.size(10.dp))
        }
    }

    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(primaryColor)
    )

}