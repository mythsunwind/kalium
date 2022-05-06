package com.wire.kalium.logger

import android.content.Context
import co.touchlab.kermit.LogWriter

expect class KaliumFileWriter() : LogWriter {
    fun init(context: Context?)
}
