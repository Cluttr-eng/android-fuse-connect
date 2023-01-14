package com.letsfuse.connect

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract


class OpenFuseLink : ActivityResultContract<SecretConfiguration, String>() {
    override fun createIntent(context: Context, input: SecretConfiguration) =
        Intent(context, FuseConnectActivity::class.java).apply {
            putExtra(FuseConnectActivity.SECRET_CONFIGURATION_EXTRA, input)
        }

    override fun parseResult(resultCode: Int, result: Intent?) : String {
        if (resultCode != Activity.RESULT_OK) {
            return "";
        }
        return ""
    }
}
