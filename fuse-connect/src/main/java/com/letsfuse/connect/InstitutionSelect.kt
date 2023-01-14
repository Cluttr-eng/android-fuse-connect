package com.letsfuse.connect

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class InstitutionSelect(
    val institution_id: String,
) : Parcelable
