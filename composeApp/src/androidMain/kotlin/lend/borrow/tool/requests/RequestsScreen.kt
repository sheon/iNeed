package lend.borrow.tool.requests

import User
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RequestsScreen(toolId: String?, loggedInUser: User) {
    val application = (LocalContext.current as Activity).application
    val requestsViewModel: RequestsViewModel = viewModel {
        RequestsViewModel(application, loggedInUser.id, toolId)
    }

    val requests by requestsViewModel.requests.collectAsState()

    LazyColumn {
        items(requests,
            key = { item ->
                item.borrower.name
            }) {item ->

            val primaryColor = Color(
                getColor(
                    LocalContext.current, lend.borrow.tool.shared.R.color.primary
                )
            )
            val backgroundColor = if (item.isRead) Color.White else primaryColor.copy(alpha = 0.2f)

            if (item.isAccepted != true)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Spacer(modifier = Modifier.size(10.dp))
                    Text(text = "${item.borrower.name} wants to borrow ${item.tool.name}")
                    Spacer(modifier = Modifier.size(10.dp))
                    if (item.isAccepted != false)
                        Button(
                            onClick = {
                                requestsViewModel.onRequestAccepted(true, item.initialRequest)
                            },
                            colors = ButtonDefaults.buttonColors(
                                primaryColor, Color.White
                            )
                        ) {
                            Text(text = "Accept")
                        }
                    Button(
                        enabled = item.isAccepted != false,
                        onClick = {
                            requestsViewModel.onRequestAccepted(false, item.initialRequest)
                        },
                        colors = ButtonDefaults.buttonColors(Color.Red, Color.White)
                    ) {
                        Text(text = if (item.isAccepted != false) "Reject" else "Rejected")
                    }
                    Spacer(modifier = Modifier.size(10.dp))
                }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(primaryColor)
            )
            requestsViewModel.onRequestReadUpdated(item.initialRequest)
        }
    }

}