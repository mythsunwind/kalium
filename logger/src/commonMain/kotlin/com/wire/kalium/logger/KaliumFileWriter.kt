package com.wire.kalium.logger

import co.touchlab.kermit.LogWriter

expect class KaliumFileWriter(path: String) : LogWriter {
    actual fun init()
}
