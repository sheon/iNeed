package lend.borrow.tool.utility

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

@Composable
fun CustomButton(modifier: Modifier = Modifier, text: String, color: Color = LocalContext.current.primaryColor, filled: Boolean = false, onClick: () -> Unit) {
    OutlinedButton(
        modifier = modifier,
        border = BorderStroke(1.dp, color),
        shape = RoundedCornerShape(10.dp),
        onClick = {
            onClick()
        }, colors = if (filled)
            ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White,
                containerColor = color
            )
        else
            ButtonDefaults.outlinedButtonColors(
                contentColor = color
            )
    ) {
        Text(text = AnnotatedString(text))
    }
}


@Composable
fun WarningButton(modifier: Modifier = Modifier, text: String, color: Color = LocalContext.current.warningColor, filled: Boolean = true, onClick: () -> Unit) {
    CustomButton(modifier, text, color, filled, onClick)
}