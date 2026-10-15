package com.quran.mobile.feature.audiobar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A combined numeric input field and +/- stepper allowing direct keyboard input
 * as well as fine-grained step increments/decrements.
 */
@Composable
fun NumberInputOrStepper(
  label: String,
  value: Int,
  minValue: Int,
  maxValue: Int,
  onValueChange: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  var textValue by remember(value) { mutableStateOf(value.toString()) }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
  ) {
    Text(
      text = "$label ($minValue–$maxValue)",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(4.dp))

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      FilledTonalButton(
        onClick = {
          if (value > minValue) {
            val next = value - 1
            textValue = next.toString()
            onValueChange(next)
          }
        },
        enabled = value > minValue,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
      ) {
        Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }

      OutlinedTextField(
        value = textValue,
        onValueChange = { input ->
          val digits = input.filter { it.isDigit() }
          if (digits.isEmpty()) {
            textValue = ""
          } else {
            val num = digits.toIntOrNull() ?: 0
            if (num > maxValue) {
              textValue = maxValue.toString()
              onValueChange(maxValue)
            } else {
              textValue = digits
              if (num >= minValue) {
                onValueChange(num)
              }
            }
          }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
          textAlign = TextAlign.Center,
          fontWeight = FontWeight.Bold
        ),
        modifier = Modifier
          .width(76.dp)
          .padding(horizontal = 4.dp)
      )

      FilledTonalButton(
        onClick = {
          if (value < maxValue) {
            val next = value + 1
            textValue = next.toString()
            onValueChange(next)
          }
        },
        enabled = value < maxValue,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
      ) {
        Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}
