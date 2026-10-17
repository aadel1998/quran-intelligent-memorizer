package com.quran.mobile.feature.qarilist.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.quran.labs.androidquran.common.audio.model.QariItem
import com.quran.mobile.feature.qarilist.model.QariUiModel
import kotlinx.collections.immutable.ImmutableList

@Composable
fun QariList(
  qaris: ImmutableList<QariUiModel>,
  selectedQariId: Int,
  onQariSelected: ((QariItem) -> Unit),
  isModernUi: Boolean = false,
  modifier: Modifier = Modifier
) {
  val qarisBySection = qaris.groupBy { it.sectionHeader }
  LazyColumn(modifier = modifier) {
    qarisBySection.forEach { (section, sectionQaris) ->
      stickyHeader {
        QariSection(section, isModernUi = isModernUi)
      }

      items(sectionQaris) { qariUiModel ->
        QariRow(
          qariItem = qariUiModel.qariItem,
          isSelected = selectedQariId == qariUiModel.qariItem.id,
          onItemSelected = onQariSelected,
          isModernUi = isModernUi
        )
      }
    }
  }
}
