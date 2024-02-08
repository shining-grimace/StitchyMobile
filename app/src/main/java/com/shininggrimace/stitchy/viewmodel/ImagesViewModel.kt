package com.shininggrimace.stitchy.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class ImagesViewModel: ViewModel() {

    enum class ProcessingState {
        Empty,
        Loading,
        Completed,
        Failed
    }

    data class OutputUpdate(
        val state: ProcessingState,
        val data: Any
    )

    val imageSelections: MutableStateFlow<List<Uri>> = MutableStateFlow(listOf())
    val outputState: MutableStateFlow<OutputUpdate> = MutableStateFlow(
        OutputUpdate(ProcessingState.Empty, Unit))

    suspend fun emitOutputState(state: ProcessingState, data: Any) {
        outputState.emit(
            OutputUpdate(state, data)
        )
    }

    suspend fun emitOutputState(state: ProcessingState) {
        outputState.emit(
            OutputUpdate(state, Unit)
        )
    }
}