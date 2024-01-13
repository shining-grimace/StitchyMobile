package com.shininggrimace.stitchy.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class ImagesViewModel: ViewModel() {
    val imageSelections: MutableStateFlow<List<Uri>> = MutableStateFlow(listOf())
}