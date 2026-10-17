package com.quran.labs.androidquran.ui.modern

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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.labs.androidquran.BuildConfig
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.components.ModernBadgeChip

/**
 * Material 3 Modern About & Data Sources screen.
 * Follows the sleek Intelligent Memorizer design: dark surface, emerald accents, and grouped cards.
 */
@Composable
fun ModernAboutScreen(
  onBackClick: () -> Unit,
  onOpenUrl: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
  val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(
        top = statusBarTop + 12.dp,
        bottom = navBarBottom + 24.dp
      ),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Top Navigation Bar
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(40.dp)
          ) {
            Icon(
              imageVector = QuranIcons.ArrowBack,
              contentDescription = "Back",
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(24.dp)
            )
          }

          Text(
            text = stringResource(R.string.menu_about),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )

          ModernBadgeChip(
            text = "Open Source",
            backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            contentColor = MaterialTheme.colorScheme.primary
          )
        }
      }

      // Hero App Card
      item {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(22.dp)),
          shape = RoundedCornerShape(22.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
          )
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = QuranIcons.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp)
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
              text = stringResource(R.string.app_name),
              fontSize = 22.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              ModernBadgeChip(
                text = "v${BuildConfig.VERSION_NAME}",
                backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                contentColor = MaterialTheme.colorScheme.primary
              )

              ModernBadgeChip(
                text = BuildConfig.FLAVOR.replaceFirstChar { it.uppercase() },
                backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
              text = stringResource(R.string.about_description),
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 20.sp,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
      }

      // Section 1: Data Sources
      item {
        Text(
          text = stringResource(R.string.about_data_sources),
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
        )
      }

      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
          )
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            AboutSourceRow(
              title = stringResource(R.string.about_images),
              subtitle = stringResource(R.string.about_madani_images),
              url = stringResource(R.string.about_madani_images_url),
              icon = QuranIcons.MenuBook,
              onOpenUrl = onOpenUrl
            )

            ModernDivider()

            AboutSourceRow(
              title = stringResource(R.string.about_quranicaudio),
              subtitle = stringResource(R.string.about_quranicaudio_summary),
              url = stringResource(R.string.about_quranicaudio_url),
              icon = QuranIcons.PlayArrow,
              onOpenUrl = onOpenUrl
            )

            ModernDivider()

            AboutSourceRow(
              title = stringResource(R.string.about_furqan),
              subtitle = stringResource(R.string.about_furqan_summary),
              url = stringResource(R.string.about_furqan_url),
              icon = QuranIcons.Translate,
              onOpenUrl = onOpenUrl
            )

            ModernDivider()

            AboutSourceRow(
              title = stringResource(R.string.about_quran_ksu),
              subtitle = stringResource(R.string.about_quran_ksu_summary),
              url = stringResource(R.string.about_quran_ksu_url),
              icon = QuranIcons.MenuBook,
              onOpenUrl = onOpenUrl
            )

            ModernDivider()

            AboutSourceRow(
              title = stringResource(R.string.about_quran_enc),
              subtitle = stringResource(R.string.about_quran_enc_summary),
              url = stringResource(R.string.about_quran_enc_url),
              icon = QuranIcons.Translate,
              onOpenUrl = onOpenUrl
            )

            ModernDivider()

            AboutSourceRow(
              title = stringResource(R.string.about_tanzil),
              subtitle = stringResource(R.string.about_tanzil_summary),
              url = stringResource(R.string.about_tanzil_url),
              icon = QuranIcons.Translate,
              onOpenUrl = onOpenUrl
            )

            ModernDivider()

            AboutSourceRow(
              title = stringResource(R.string.about_noorhidayat),
              subtitle = stringResource(R.string.about_noorhidayat_summary),
              url = stringResource(R.string.about_noorhidayat_url),
              icon = QuranIcons.MenuBook,
              onOpenUrl = onOpenUrl
            )
          }
        }
      }

      // Section 2: Community & Open Source
      item {
        Text(
          text = stringResource(R.string.about_open_source),
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
        )
      }

      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
          )
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            AboutSourceRow(
              title = stringResource(R.string.about_contributors),
              subtitle = stringResource(R.string.about_contributors_summary),
              url = stringResource(R.string.about_contributors_url),
              icon = QuranIcons.Person,
              onOpenUrl = onOpenUrl
            )

            ModernDivider()

            AboutSourceRow(
              title = "GitHub Project",
              subtitle = "Free and open-source Quran app developed by community contributors worldwide.",
              url = "https://github.com/quran/quran_android",
              icon = QuranIcons.MenuBook,
              onOpenUrl = onOpenUrl
            )

            ModernDivider()

            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
            ) {
              Text(
                text = "Open Source Libraries",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "AndroidX, Jetpack Compose, Kotlin, Metro DI, RxJava, OkHttp, Moshi, Timber.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AboutSourceRow(
  title: String,
  subtitle: String,
  url: String,
  icon: ImageVector,
  onOpenUrl: (String) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onOpenUrl(url) }
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          fontSize = 15.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = subtitle,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 17.sp
        )
      }
    }

    Spacer(modifier = Modifier.width(8.dp))

    Icon(
      imageVector = QuranIcons.ChevronRight,
      contentDescription = "Open Link",
      tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
      modifier = Modifier.size(18.dp)
    )
  }
}

@Composable
private fun ModernDivider() {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(0.5.dp)
      .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
  )
}
