package com.quran.labs.androidquran.ui.modern

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.components.ModernBadgeChip

/**
 * Modern Tag Color Palette for distinct visual identification of collections.
 */
val ModernTagColors = listOf(
  Color(0xFF10B981), // Emerald
  Color(0xFF06B6D4), // Cyan
  Color(0xFFF59E0B), // Amber
  Color(0xFF8B5CF6), // Purple
  Color(0xFFEC4899), // Pink
  Color(0xFF14B8A6), // Teal
  Color(0xFF3B82F6), // Blue
  Color(0xFFF97316)  // Orange
)

fun getModernTagColor(tagId: String): Color {
  val hash = kotlin.math.abs(tagId.hashCode())
  return ModernTagColors[hash % ModernTagColors.size]
}

/**
 * Sleek Modern Material 3 Bottom Sheet for assigning and managing bookmark tags.
 */
@Composable
fun ModernTagSheet(
  visible: Boolean,
  targetTitle: String,
  tags: List<TagItemModel>,
  selectedTagIds: Set<String>,
  onToggleTag: (tagId: String) -> Unit,
  onCreateTag: (tagName: String) -> Unit,
  onDeleteTag: ((tagId: String) -> Unit)? = null,
  onSave: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  var newTagName by remember { mutableStateOf("") }
  var validationError by remember { mutableStateOf<Int?>(null) }

  val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
  val bottomPadding = if (navBarBottom > 0.dp) navBarBottom + 12.dp else 20.dp

  if (visible) {
    Box(
      modifier = modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.55f))
        .clickable(onClick = onDismiss)
    ) {
      AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter)
      ) {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}
            .shadow(elevation = 24.dp, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
          shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f),
          border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
          )
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 12.dp)
              .padding(bottom = bottomPadding),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            // Drag Handle
            Box(
              modifier = Modifier
                .width(38.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Header Row: Title + Target Badge + Close Button
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = stringResource(R.string.tag_sheet_title),
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                if (targetTitle.isNotBlank()) {
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = targetTitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }

              IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(36.dp)
              ) {
                Icon(
                  imageVector = QuranIcons.Close,
                  contentDescription = stringResource(R.string.hifz_stats_close),
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(20.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Inline "Create New Tag" Input Bar
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                .border(
                  width = 0.5.dp,
                  color = if (validationError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                  shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = QuranIcons.Add,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box(modifier = Modifier.weight(1f)) {
                  if (newTagName.isEmpty()) {
                    Text(
                      text = stringResource(R.string.tag_new_input_hint),
                      fontSize = 14.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                  }

                  val submitNewTag = {
                    val trimmed = newTagName.trim()
                    if (trimmed.isEmpty()) {
                      validationError = R.string.tag_err_empty
                    } else if (tags.any { it.name.equals(trimmed, ignoreCase = true) }) {
                      validationError = R.string.tag_err_exists
                    } else {
                      validationError = null
                      onCreateTag(trimmed)
                      newTagName = ""
                    }
                  }

                  BasicTextField(
                    value = newTagName,
                    onValueChange = {
                      newTagName = it
                      if (validationError != null) validationError = null
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                      fontSize = 14.sp,
                      fontWeight = FontWeight.Medium,
                      color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submitNewTag() }),
                    modifier = Modifier.fillMaxWidth()
                  )
                }

                if (newTagName.isNotEmpty()) {
                  IconButton(
                    onClick = {
                      newTagName = ""
                      validationError = null
                    },
                    modifier = Modifier.size(24.dp)
                  ) {
                    Icon(
                      imageVector = QuranIcons.Close,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(16.dp)
                    )
                  }

                  Spacer(modifier = Modifier.width(6.dp))

                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .background(MaterialTheme.colorScheme.primary)
                      .clickable {
                        val trimmed = newTagName.trim()
                        if (trimmed.isEmpty()) {
                          validationError = R.string.tag_err_empty
                        } else if (tags.any { it.name.equals(trimmed, ignoreCase = true) }) {
                          validationError = R.string.tag_err_exists
                        } else {
                          validationError = null
                          onCreateTag(trimmed)
                          newTagName = ""
                        }
                      }
                      .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = stringResource(R.string.tag_btn_add),
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onPrimary
                    )
                  }
                }
              }
            }

            // Validation Error text if present
            if (validationError != null) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = stringResource(validationError!!),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(start = 12.dp)
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tags List
            Text(
              text = stringResource(R.string.tag_collections_header, tags.size),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              letterSpacing = 1.sp,
              modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, bottom = 8.dp)
            )

            if (tags.isEmpty()) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = stringResource(R.string.tag_empty_desc),
                  fontSize = 13.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                  textAlign = TextAlign.Center
                )
              }
            } else {
              LazyColumn(
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                items(tags, key = { it.id }) { tag ->
                  val isSelected = selectedTagIds.contains(tag.id)
                  val tagColor = getModernTagColor(tag.id)

                  TagSelectionRow(
                    tag = tag,
                    isSelected = isSelected,
                    tagColor = tagColor,
                    onToggle = { onToggleTag(tag.id) },
                    onDelete = if (!tag.isDefault && onDeleteTag != null) {
                      { onDeleteTag(tag.id) }
                    } else null
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Done / Save Button
            Button(
              onClick = onSave,
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
              )
            ) {
              Text(
                text = stringResource(R.string.tag_btn_save_apply),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TagSelectionRow(
  tag: TagItemModel,
  isSelected: Boolean,
  tagColor: Color,
  onToggle: () -> Unit,
  onDelete: (() -> Unit)?,
  modifier: Modifier = Modifier
) {
  val shape = RoundedCornerShape(14.dp)
  val backgroundColor = if (isSelected) {
    tagColor.copy(alpha = 0.14f)
  } else {
    MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
  }
  val borderColor = if (isSelected) {
    tagColor.copy(alpha = 0.45f)
  } else {
    MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
  }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .background(backgroundColor)
      .border(0.5.dp, borderColor, shape)
      .clickable(onClick = onToggle)
      .padding(horizontal = 14.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Color Dot Pill
      Box(
        modifier = Modifier
          .size(10.dp)
          .clip(CircleShape)
          .background(tagColor)
      )

      Spacer(modifier = Modifier.width(12.dp))

      Text(
        text = tag.name,
        fontSize = 14.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      if (tag.isDefault) {
        Spacer(modifier = Modifier.width(8.dp))
        ModernBadgeChip(
          text = stringResource(R.string.tag_badge_default),
          backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
      if (onDelete != null) {
        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = QuranIcons.Delete,
            contentDescription = stringResource(R.string.tag_btn_delete),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.size(15.dp)
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
      }

      // Check Indicator
      Box(
        modifier = Modifier
          .size(22.dp)
          .clip(CircleShape)
          .background(if (isSelected) tagColor else Color.Transparent)
          .border(
            width = 1.5.dp,
            color = if (isSelected) tagColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            shape = CircleShape
          ),
        contentAlignment = Alignment.Center
      ) {
        if (isSelected) {
          Icon(
            imageVector = QuranIcons.Check,
            contentDescription = "Selected",
            tint = Color.White,
            modifier = Modifier.size(14.dp)
          )
        }
      }
    }
  }
}
