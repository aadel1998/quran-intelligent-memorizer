package com.quran.labs.androidquran.ui.modern

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.components.ModernCard
import kotlin.random.Random

/**
 * Easy Quran Memorizer Pro - Self-Testing & Recall Mode (Next Ayah Recitation Quiz).
 *
 * Prompts user with Ayah N, challenges them to recite Ayah N+1 from memory,
 * provides tap-to-reveal scaffolding, and updates Spaced Repetition (SRS) scores.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernRecallQuizSheet(
  sura: SuraItemModel,
  onQuizResult: (passed: Boolean) -> Unit,
  onDismissRequest: () -> Unit,
  onPlayAyah: ((sura: Int, ayah: Int) -> Unit)? = null,
  sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
  val totalAyahs = sura.numberOfAyahs
  val maxPromptAyah = if (totalAyahs > 1) totalAyahs - 1 else 1

  var promptAyah by remember { mutableIntStateOf(Random.nextInt(1, maxPromptAyah + 1)) }
  var isRevealed by remember { mutableStateOf(false) }
  var questionsAnswered by remember { mutableIntStateOf(0) }
  var correctCount by remember { mutableIntStateOf(0) }

  val targetAyah = if (totalAyahs > 1) promptAyah + 1 else 1

  fun nextQuestion() {
    isRevealed = false
    promptAyah = if (maxPromptAyah > 1) {
      val next = Random.nextInt(1, maxPromptAyah + 1)
      if (next == promptAyah) (next % maxPromptAyah) + 1 else next
    } else {
      1
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismissRequest,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 36.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = QuranIcons.Speed,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "SELF-TESTING RECALL QUIZ",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              letterSpacing = 1.sp
            )
            Text(
              text = "${sura.englishName} (${sura.arabicName})",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        if (questionsAnswered > 0) {
          Box(
            modifier = Modifier
              .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
              .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Text(
              text = "$correctCount / $questionsAnswered Correct",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // 1. Given Prompt Ayah Card
      ModernCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        contentPadding = 16.dp
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "PROMPT AYAH",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              letterSpacing = 1.sp
            )

            if (onPlayAyah != null) {
              IconButton(
                onClick = { onPlayAyah(sura.number, promptAyah) },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = QuranIcons.PlayArrow,
                  contentDescription = "Listen to Prompt Ayah",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "Ayah $promptAyah of ${sura.numberOfAyahs}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "Recite this verse aloud to anchor your memory, then recall the next verse.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 2. Target Masked/Revealed Next Ayah Card
      ModernCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = if (isRevealed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                         else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
        borderColor = if (isRevealed) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                      else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
        borderWidth = 1.dp,
        contentPadding = 16.dp
      ) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "NEXT AYAH (TARGET)",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = if (isRevealed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
              letterSpacing = 1.sp
            )

            Text(
              text = "Ayah $targetAyah",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          if (!isRevealed) {
            // Masked state: tap-to-reveal
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                .clickable { isRevealed = true }
                .padding(vertical = 24.dp, horizontal = 16.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = "••••••••••••••••••••••••••••••••••••••",
                  fontSize = 18.sp,
                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                  letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "Recite Ayah $targetAyah from memory",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Tap to Reveal & Verify Recitation",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
              onClick = { isRevealed = true },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
              )
            ) {
              Icon(
                imageVector = QuranIcons.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Reveal Ayah $targetAyah", fontWeight = FontWeight.Bold)
            }
          } else {
            // Revealed state: user self-verification
            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                  .padding(12.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "Ayah $targetAyah Revealed",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
              }

              Spacer(modifier = Modifier.height(14.dp))

              Text(
                text = "Did you recite Ayah $targetAyah correctly?",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
              )

              Spacer(modifier = Modifier.height(12.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                // Correct Button
                Button(
                  onClick = {
                    questionsAnswered++
                    correctCount++
                    onQuizResult(true)
                    nextQuestion()
                  },
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.White
                  )
                ) {
                  Icon(
                    imageVector = QuranIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Correct", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // Needs Practice Button
                OutlinedButton(
                  onClick = {
                    questionsAnswered++
                    onQuizResult(false)
                    nextQuestion()
                  },
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFF59E0B)
                  )
                ) {
                  Text("Needs Practice", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Bottom Row: Skip / Next Ayah
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = onDismissRequest,
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Finish Quiz")
        }

        Button(
          onClick = { nextQuestion() },
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
          )
        ) {
          Text("Skip to Next Ayah")
        }
      }
    }
  }
}
