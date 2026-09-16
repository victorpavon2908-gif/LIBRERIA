package com.libreriapro.ultra.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libreriapro.ultra.ui.theme.Card as CardColor
import com.libreriapro.ultra.ui.theme.CardElevated
import com.libreriapro.ultra.ui.theme.Purple
import com.libreriapro.ultra.ui.theme.Red
import com.libreriapro.ultra.ui.theme.Soft

/** True on phones, false on tablets: used to adapt paddings and column counts. */
val LocalCompactLayout = staticCompositionLocalOf { true }

private val CardShape = RoundedCornerShape(18.dp)

@Composable
fun MetricCard(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = Purple,
    detail: String? = null,
) {
    Card(
        modifier = modifier,
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = CardColor),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, color = Soft, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = accent)
            if (detail != null) Text(detail, color = Soft, fontSize = 11.sp)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (trailing != null) Text(trailing, color = Soft, fontSize = 12.sp)
    }
}

@Composable
fun AlertCard(title: String, detail: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = CardColor),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(34.dp)
                    .background(Red.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Warning, contentDescription = null, tint = Red, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(detail, color = Soft, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, detail: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = Soft, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(detail, color = Soft, fontSize = 12.sp)
    }
}

/** Text field used by every form so the look stays consistent. */
@Composable
fun DarkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = trailing,
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Purple,
            unfocusedBorderColor = CardElevated,
            focusedLabelColor = Purple,
            unfocusedLabelColor = Soft,
        ),
    )
}

/** Compact shortcut card used by the dashboard. */
@Composable
fun QuickAction(
    icon: ImageVector,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardElevated),
    ) {
        Column(Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** -/+ control shared by the cart and the goods receipt. */
@Composable
fun QuantityStepper(
    qty: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onDecrease, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Filled.Remove, contentDescription = "Quitar una unidad", tint = Soft)
        }
        Text(
            qty.toString(),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        IconButton(onClick = onIncrease, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Filled.Add, contentDescription = "Añadir una unidad", tint = Purple)
        }
    }
}

/** Simple label/value line used by the reports and cash screens. */
@Composable
fun KeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White,
    emphasis: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = if (emphasis) Color.White else Soft, fontSize = if (emphasis) 14.sp else 13.sp)
        Text(
            value,
            color = valueColor,
            fontWeight = if (emphasis) FontWeight.ExtraBold else FontWeight.SemiBold,
            fontSize = if (emphasis) 18.sp else 14.sp,
        )
    }
}
