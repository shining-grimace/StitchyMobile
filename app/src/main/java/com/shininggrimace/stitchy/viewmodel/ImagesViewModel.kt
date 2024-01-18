package com.shininggrimace.stitchy.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class ImagesViewModel: ViewModel() {

    enum class OutputState {
        Empty,
        Loading,
        Completed,
        Failed
    }

    val imageSelections: MutableStateFlow<List<Uri>> = MutableStateFlow(listOf())
    val outputState: MutableStateFlow<Pair<OutputState, Any>> = MutableStateFlow(
        Pair(OutputState.Empty, Unit))
}