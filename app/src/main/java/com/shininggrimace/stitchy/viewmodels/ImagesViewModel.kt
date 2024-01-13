package com.shininggrimace.stitchy.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class ImagesViewModel: ViewModel() {
    val imageSelections: MutableStateFlow<List<Uri>> = MutableStateFlow(listOf())
}