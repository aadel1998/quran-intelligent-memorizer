package com.quran.labs.androidquran.common.ui.core.icons

import androidx.compose.ui.graphics.vector.ImageVector

val BookmarkFilled: ImageVector by lazy {
  materialIcon(name = "Filled.Bookmark") {
    materialPath {
      moveTo(17.0f, 3.0f)
      horizontalLineTo(7.0f)
      curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
      verticalLineToRelative(16.0f)
      lineToRelative(7.0f, -3.0f)
      lineToRelative(7.0f, 3.0f)
      verticalLineTo(5.0f)
      curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
      close()
    }
  }
}
