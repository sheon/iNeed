package lend.borrow.tool

import BorrowLendApp
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    //val viewModel by viewModels<GlobalLoadingViewModel>() //This is for test for now
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BorrowLendApp()
        }
    }
}
