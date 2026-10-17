package com.quran.mobile.feature.qarilist

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Surface
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quran.data.model.SuraAyah
import com.quran.labs.androidquran.common.audio.model.QariItem
import com.quran.labs.androidquran.common.audio.repository.CurrentQariManager
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.QuranTheme
import com.quran.mobile.feature.qarilist.di.QariListWrapperInjector
import com.quran.mobile.feature.qarilist.presenter.QariListPresenter
import com.quran.mobile.feature.qarilist.ui.QariList
import dev.zacsweers.metro.Inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class QariListWrapper(
  context: Context,
  private val startAyah: SuraAyah,
  private val endAyah: SuraAyah,
): FrameLayout(context) {

  @Inject
  lateinit var qariListPresenter: QariListPresenter

  @Inject
  lateinit var currentQariManager: CurrentQariManager

  init {
    (context as? QariListWrapperInjector)?.injectQariListWrapper(this)

    val composeView = ComposeView(context).apply {
      setContent {
        QuranTheme {
          QariListBottomSheet()
        }
      }
    }
    addView(composeView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
  }

  @Composable
  fun QariListBottomSheet() {
    val state = rememberBottomSheetScaffoldState(
      bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false
      )
    )
    val qariListFlow =
      qariListPresenter.qariList(startAyah, endAyah) { QariItem.fromQari(context, it) }
    val qariListState = qariListFlow.collectAsState(persistentListOf())
    val currentQariFlow = currentQariManager.flow()
    val currentQariState = currentQariFlow.collectAsState(null)

    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val closeDialog = {
      focusManager.clearFocus()
      coroutineScope.launch {
        state.bottomSheetState.hide()
      }
    }

    val onQariSelected = { qari: QariItem ->
      closeDialog()
      currentQariManager.setCurrentQari(qari.id)
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredQaris by remember(qariListState.value) {
      derivedStateOf {
        val query = searchQuery
        if (query.isBlank()) {
          qariListState.value
        } else {
          qariListState.value
            .filter { it.qariItem.name.contains(query, ignoreCase = true) }
            .toImmutableList()
        }
      }
    }

    val isModernUi = remember {
      try {
        val prefs = context.getSharedPreferences("${context.packageName}_preferences", android.content.Context.MODE_PRIVATE)
        prefs.getBoolean("pref_use_modern_ui", true)
      } catch (_: Exception) {
        true
      }
    }

    Box(modifier = Modifier
      .fillMaxSize()
      .displayCutoutPadding()
    ) {
      BottomSheetScaffold(
        scaffoldState = state,
        sheetPeekHeight = 0.dp,
        sheetDragHandle = null,
        sheetShape = if (isModernUi) RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp) else RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = Color.Transparent,
        sheetContainerColor = if (isModernUi) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f) else MaterialTheme.colorScheme.surface,
        sheetContentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.background),
        sheetContent = {
          Column(
            modifier = Modifier
              .fillMaxHeight()
              .imePadding()
          ) {
            if (isModernUi) {
              // Modern Drag Handle
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
              ) {
                Box(
                  modifier = Modifier
                    .width(38.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                )
              }

              // Modern Header
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                  horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
                ) {
                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    modifier = Modifier.size(36.dp)
                  ) {
                    Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                      Icon(
                        imageVector = QuranIcons.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                      )
                    }
                  }
                  Text(
                    text = stringResource(R.string.qarilist_select_qari),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }

                IconButton(
                  onClick = { closeDialog() },
                  modifier = Modifier.size(34.dp)
                ) {
                  Icon(
                    imageVector = QuranIcons.Close,
                    contentDescription = stringResource(R.string.qarilist_dismiss),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            } else {
              TopAppBar(
                title = {
                  Text(
                    stringResource(R.string.qarilist_select_qari),
                    style = MaterialTheme.typography.titleLarge
                  )
                },
                navigationIcon = {
                  IconButton(onClick = { closeDialog() }) {
                    Icon(
                      imageVector = QuranIcons.Close,
                      contentDescription = stringResource(R.string.qarilist_dismiss)
                    )
                  }
                }
              )
            }

            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              shape = if (isModernUi) RoundedCornerShape(16.dp) else OutlinedTextFieldDefaults.shape,
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
              placeholder = { Text(stringResource(R.string.qarilist_search_hint)) },
              leadingIcon = {
                Icon(imageVector = QuranIcons.Search, contentDescription = null)
              },
              trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                  IconButton(onClick = { searchQuery = "" }) {
                    Icon(imageVector = QuranIcons.Close, contentDescription = null)
                  }
                }
              },
              singleLine = true
            )

            QariList(
              filteredQaris,
              selectedQariId = currentQariState.value?.id ?: -1,
              onQariSelected = onQariSelected,
              isModernUi = isModernUi,
              modifier = Modifier.weight(1f)
            )
          }
        }
      ) {
        val hasExpanded = remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
          // delay so sheet animates smoothly, otherwise it just snaps open
          delay(50)
          state.bottomSheetState.expand()
          hasExpanded.value = true
        }

        if (hasExpanded.value && state.bottomSheetState.currentValue == SheetValue.PartiallyExpanded) {
          closeDialog()
        }
      }
    }
  }
}
