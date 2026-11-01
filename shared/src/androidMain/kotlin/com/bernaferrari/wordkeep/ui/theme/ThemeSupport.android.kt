package com.bernaferrari.wordkeep.ui.theme

import android.os.Build

actual fun supportsDynamicTheme(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S