package com.shininggrimace.stitchy.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

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

    private val imageSelections: MutableLiveData<List<Uri>> = MutableLiveData(listOf())

    private val outputState: MutableLiveData<OutputUpdate> = MutableLiveData(
        OutputUpdate(ProcessingState.Empty, Unit))

    fun getImageSelections(): LiveData<List<Uri>> = imageSelections

    fun postImageSelections(uris: List<Uri>) {
        imageSelections.postValue(uris)
    }

    fun getOutputState(): LiveData<OutputUpdate> = outputState

    fun postOutputState(state: ProcessingState, data: Any) {
        outputState.postValue(
            OutputUpdate(state, data)
        )
    }

    fun postOutputState(state: ProcessingState) {
        outputState.postValue(
            OutputUpdate(state, Unit)
        )
    }
}