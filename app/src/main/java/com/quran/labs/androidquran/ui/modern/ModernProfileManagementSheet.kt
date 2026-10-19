package com.quran.labs.androidquran.ui.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.labs.androidquran.common.audio.memorization.MemorizationPreferences
import com.quran.labs.androidquran.common.audio.memorization.MemorizationProfile
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.components.ModernCard
import com.quran.mobile.common.ui.core.R as CoreR

/**
 * Bottom sheet for managing multiple user profiles (Easy Quran Memorizer Pro).
 * Enables independent memorization tracking for family members, students, and teachers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernProfileManagementSheet(
  profiles: List<MemorizationProfile>,
  activeProfile: MemorizationProfile,
  onSwitchProfile: (String) -> Unit,
  onCreateProfile: (String) -> Unit,
  onDeleteProfile: (String) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var newProfileName by remember { mutableStateOf("") }
  var isAddingProfile by remember { mutableStateOf(false) }

  ModalBottomSheet(
    onDismissRequest = onDismissRequest,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp)
        .padding(bottom = 28.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = QuranIcons.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = stringResource(CoreR.string.profile_title),
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = stringResource(CoreR.string.profile_subtitle),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        IconButton(onClick = onDismissRequest) {
          Icon(
            imageVector = QuranIcons.Close,
            contentDescription = stringResource(CoreR.string.cancel),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Profiles List
      LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(profiles, key = { it.id }) { profile ->
          val isActive = profile.id == activeProfile.id
          ModernCard(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                if (!isActive) {
                  onSwitchProfile(profile.id)
                  onDismissRequest()
                }
              },
            shape = RoundedCornerShape(14.dp),
            backgroundColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            borderColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
            borderWidth = 1.dp,
            contentPadding = 12.dp
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                  selected = isActive,
                  onClick = {
                    if (!isActive) {
                      onSwitchProfile(profile.id)
                      onDismissRequest()
                    }
                  },
                  colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                  )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                  Text(
                    text = profile.name,
                    fontSize = 15.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = if (profile.id == MemorizationPreferences.DEFAULT_PROFILE_ID) {
                      stringResource(CoreR.string.profile_primary)
                    } else {
                      stringResource(CoreR.string.profile_student_family)
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              if (isActive) {
                Box(
                  modifier = Modifier
                    .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = QuranIcons.Check,
                      contentDescription = null,
                      tint = Color(0xFF10B981),
                      modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                      text = stringResource(CoreR.string.profile_active_badge).uppercase(),
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF10B981)
                    )
                  }
                }
              } else if (profile.id != MemorizationPreferences.DEFAULT_PROFILE_ID) {
                IconButton(
                  onClick = { onDeleteProfile(profile.id) },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(
                    imageVector = QuranIcons.Delete,
                    contentDescription = stringResource(CoreR.string.profile_delete_title),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Add New Profile Section
      if (isAddingProfile) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(14.dp)
        ) {
          Text(
            text = stringResource(CoreR.string.profile_create_new),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = newProfileName,
            onValueChange = { newProfileName = it },
            placeholder = { Text(stringResource(CoreR.string.profile_name_hint), fontSize = 13.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
              onDone = {
                val clean = newProfileName.trim()
                if (clean.isNotBlank()) {
                  onCreateProfile(clean)
                  newProfileName = ""
                  isAddingProfile = false
                  onDismissRequest()
                }
              }
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
          )

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                val clean = newProfileName.trim()
                if (clean.isNotBlank()) {
                  onCreateProfile(clean)
                  newProfileName = ""
                  isAddingProfile = false
                  onDismissRequest()
                }
              },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
              ),
              enabled = newProfileName.isNotBlank()
            ) {
              Text(stringResource(CoreR.string.profile_save_btn), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
              onClick = { isAddingProfile = false },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
              )
            ) {
              Text(stringResource(CoreR.string.cancel), fontSize = 13.sp)
            }
          }
        }
      } else {
        Button(
          onClick = { isAddingProfile = true },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            contentColor = MaterialTheme.colorScheme.primary
          )
        ) {
          Text(stringResource(CoreR.string.profile_add_new_btn), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
      }
    }
  }
}
