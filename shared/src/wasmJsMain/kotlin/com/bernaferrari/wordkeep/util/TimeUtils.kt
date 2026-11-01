package com.bernaferrari.wordkeep.util

import kotlin.time.Clock

actual fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()