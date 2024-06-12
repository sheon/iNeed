package lend.borrow.tool

import ToolDownloadedFromFireBase
import ToolsRepository
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.runBlocking

class ToolsViewModel(application: Application): AndroidViewModel(application) {
    val toolsRepo by lazy {
        ToolsRepository()
    }

    fun getToolsFromRemote(callback: (List<ToolDownloadedFromFireBase>)-> Unit) = runBlocking {
        toolsRepo.getAvailableTools(callback)
    }
}