package lend.borrow.tool

import androidx.compose.ui.Modifier

enum class BorrowLendAppScreen(val title: String, modifier: Modifier = Modifier) {
    LOGIN(title = "Log in"),
    TOOLS(title = "Tools to borrow"),
    USER(title = "User profile"),
    TOOL_DETAIL(title = "Tool's detail")
}