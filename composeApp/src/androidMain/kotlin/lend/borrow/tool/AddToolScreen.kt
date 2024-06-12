package lend.borrow.tool

import ToolToBeUploadedToFireBase
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import lend.borrow.tool.shared.R

@Composable
fun CustomDialogWithResult(
    onDismiss: () -> Unit,
    onNegativeClick: () -> Unit,
    onPositiveClick: (ToolToBeUploadedToFireBase, List<Int>) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        var toolName by rememberSaveable { mutableStateOf("") }
        var toolDescription by rememberSaveable { mutableStateOf("") }
        var toolTags by rememberSaveable { mutableStateOf("") }
        var toolValue by rememberSaveable { mutableStateOf(0) }

        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp)
        ) {

            Column(modifier = Modifier.padding(8.dp)) {

                Text(
                    text = "Select Color",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Color Selection
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {


                    Column {
                        Text(text = "Tool name:oInt()}")
                        TextField(
                            value = toolName,
                            onValueChange = {
                                toolName = it
                            },
                            modifier = Modifier
                                .fillMaxWidth(),
                            colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(text = "Description:")
                        TextField(
                            value = toolDescription,
                            onValueChange = {
                                toolDescription = it
                            },
                            modifier = Modifier
                                .fillMaxWidth(),
                            colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(text = "Tags:")
                        TextField(
                            value = toolTags,
                            onValueChange = {
                                toolTags = it
                            },
                            modifier = Modifier
                                .fillMaxWidth(),
                            colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Value:")
                        TextField(
                            value = toolValue.toString(),
                            onValueChange = {
                                if(it.isNotBlank())
                                    toolValue = it.toInt()
                            },
                            modifier = Modifier
                                .fillMaxWidth(),
                            colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent)
                        )
                    }
                }


                // Buttons
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    TextButton(onClick = onNegativeClick) {
                        Text(text = "CANCEL")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = {
                        onPositiveClick(ToolToBeUploadedToFireBase(toolName, toolDescription,tags = toolTags.split(",")), listOf(R.drawable.saw))
                    }) {
                        Text(text = "OK")
                    }
                }
            }
        }
    }
}