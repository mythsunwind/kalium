package com.wire.kalium.logger

import android.content.Context
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity

actual class KaliumFileWriter : LogWriter() {
    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        TODO("Not yet implemented")
    }

    actual fun init(context: Context?) {
    }
}
