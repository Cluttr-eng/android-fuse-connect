package com.letsfuse.connect

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SecretConfiguration(
    val clientSecret: String,
) : Parcelable