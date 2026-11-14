package com.example.goout.feature.report

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.goout.core.designsystem.GoOutColors
import com.example.goout.core.designsystem.containerColor
import com.example.goout.core.designsystem.onContainerColor
import com.example.goout.core.designsystem.statusColor
import com.example.goout.domain.model.DensityStatus
import com.example.goout.domain.usecase.ReportResult

/** Screen "Lapor" — one-tap traffic condition reporting (Figma: Report Traffic Condition). */
@Composable
fun ReportScreen(
    onBackToMap: () -> Unit,
    onLogin: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()

    // Result screens (Figma: Report Submission Success / Failure)
    when (val result = state.result) {
        is ReportResult.Delivered -> {
            ResultPanel(
                icon = { Icon(Icons.Filled.CheckCircle, null, tint = GoOutColors.Lancar, modifier = Modifier.size(72.dp)) },
                title = "Laporan Berhasil Terkirim",
                message = "Laporan berhasil dikirim. Terima kasih telah berkontribusi memperbarui kondisi lalu lintas warga Kota Malang.",
                primaryLabel = "Kembali ke Peta",
                onPrimary = { viewModel.clearResult(); onBackToMap() },
                secondaryLabel = "Kirim Laporan Lain",
                onSecondary = { viewModel.clearResult(); viewModel.refreshLocation() },
            )
            return
        }
        is ReportResult.SavedOffline -> {
            ResultPanel(
                icon = { Icon(Icons.Filled.CloudOff, null, tint = GoOutColors.OnMerayapContainer, modifier = Modifier.size(72.dp)) },
                title = "Laporan Menunggu Sinkronisasi",
                message = "Laporan disimpan dan menunggu sinkronisasi. Draf tersimpan di perangkat dan akan dikirim otomatis saat koneksi kembali.",
                primaryLabel = "Kembali ke Peta",
                onPrimary = { viewModel.clearResult(); onBackToMap() },
                secondaryLabel = "Kirim Laporan Lain",
                onSecondary = { viewModel.clearResult(); viewModel.refreshLocation() },
            )
            return
        }
        is ReportResult.InvalidLocation -> {
            ResultPanel(
                icon = { Icon(Icons.Filled.ErrorOutline, null, tint = GoOutColors.Macet, modifier = Modifier.size(72.dp)) },
                title = "Lokasi Tidak Valid",
                message = "Lokasi Anda berada di luar area koridor pemantauan." +
                    (result.distanceMeters?.let { " Jarak ke koridor terdekat ±${it.toInt()} m (maksimum 100 m)." } ?: ""),
                primaryLabel = "Coba Lagi",
                onPrimary = { viewModel.clearResult(); viewModel.refreshLocation() },
                secondaryLabel = "Kembali ke Peta",
                onSecondary = { viewModel.clearResult(); onBackToMap() },
            )
            return
        }
        ReportResult.LocationUnavailable -> {
            ResultPanel(
                icon = { Icon(Icons.Filled.ErrorOutline, null, tint = GoOutColors.Macet, modifier = Modifier.size(72.dp)) },
                title = "Lokasi Tidak Tersedia",
                message = "Tidak dapat memperoleh lokasi. Pastikan GPS aktif.",
                primaryLabel = "Coba Lagi",
                onPrimary = { viewModel.clearResult(); viewModel.refreshLocation() },
                secondaryLabel = "Kembali ke Peta",
                onSecondary = { viewModel.clearResult(); onBackToMap() },
            )
            return
        }
        ReportResult.NotAuthenticated -> {
            ResultPanel(
                icon = { Icon(Icons.Filled.ErrorOutline, null, tint = GoOutColors.Primary, modifier = Modifier.size(72.dp)) },
                title = "Login Diperlukan",
                message = "Silakan login untuk mengirim laporan.",
                primaryLabel = "Masuk",
                onPrimary = { viewModel.clearResult(); onLogin() },
                secondaryLabel = "Kembali ke Peta",
                onSecondary = { viewModel.clearResult(); onBackToMap() },
            )
            return
        }
        null -> Unit
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(GoOutColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Lapor Kondisi Jalan", style = MaterialTheme.typography.titleLarge, color = GoOutColors.TextPrimary)
        Text(
            "Pilih kondisi arus kendaraan saat ini. Data Anda memperbarui peta warga Malang secara real-time.",
            style = MaterialTheme.typography.bodySmall,
            color = GoOutColors.TextSecondary,
        )
        Spacer(Modifier.height(16.dp))

        LocationBanner(state.location, onRetry = viewModel::refreshLocation)

        if (!isAuthenticated) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(GoOutColors.PrimaryContainer, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Silakan login untuk mengirim laporan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GoOutColors.Primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onLogin) { Text("Masuk", color = GoOutColors.Primary) }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "PILIH TINGKAT KEPADATAN",
            style = MaterialTheme.typography.labelMedium,
            color = GoOutColors.TextPrimary,
        )
        Spacer(Modifier.height(10.dp))

        ReportOption(
            status = DensityStatus.LANCAR,
            speedLabel = "≥ 35 km/jam",
            description = "Arus lancar tanpa hambatan berarti",
            selected = state.selected == DensityStatus.LANCAR,
            enabled = !state.isSubmitting,
            onClick = { viewModel.select(DensityStatus.LANCAR) },
        )
        Spacer(Modifier.height(10.dp))
        ReportOption(
            status = DensityStatus.MERAYAP,
            speedLabel = "15 – 34.9 km/jam",
            description = "Kecepatan berkurang, antrean lampu merah / persimpangan",
            selected = state.selected == DensityStatus.MERAYAP,
            enabled = !state.isSubmitting,
            onClick = { viewModel.select(DensityStatus.MERAYAP) },
        )
        Spacer(Modifier.height(10.dp))
        ReportOption(
            status = DensityStatus.MACET,
            speedLabel = "< 15 km/jam",
            description = "Kendaraan berhenti lama, hambatan berat atau padat total",
            selected = state.selected == DensityStatus.MACET,
            enabled = !state.isSubmitting,
            onClick = { viewModel.select(DensityStatus.MACET) },
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = viewModel::submit,
            enabled = state.selected != null && !state.isSubmitting,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoOutColors.Primary),
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(Modifier.size(20.dp), color = GoOutColors.OnPrimary, strokeWidth = 2.dp)
                Text("  Mengirim…")
            } else {
                Text("Kirim Laporan Kondisi")
            }
        }
        TextButton(onClick = onBackToMap, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Batal", color = GoOutColors.TextSecondary)
        }
    }
}

@Composable
private fun LocationBanner(location: ReportLocationState, onRetry: () -> Unit) {
    val (bg, fg, text) = when (location) {
        ReportLocationState.Checking ->
            Triple(GoOutColors.SurfaceCard, GoOutColors.TextSecondary, "Memeriksa lokasi Anda…")
        is ReportLocationState.Valid ->
            Triple(
                GoOutColors.LancarContainer,
                GoOutColors.OnLancarContainer,
                "Lokasi Terverifikasi di Koridor ${location.corridorName} (±${location.distanceMeters.toInt()} m)",
            )
        is ReportLocationState.Invalid ->
            Triple(
                GoOutColors.MacetContainer,
                GoOutColors.OnMacetContainer,
                "Lokasi Anda berada di luar area koridor pemantauan.",
            )
        ReportLocationState.Unavailable ->
            Triple(GoOutColors.MacetContainer, GoOutColors.OnMacetContainer, "Tidak dapat memperoleh lokasi. Pastikan GPS aktif.")
        ReportLocationState.PermissionNeeded ->
            Triple(GoOutColors.MerayapContainer, GoOutColors.OnMerayapContainer, "Izin lokasi diperlukan untuk memvalidasi posisi Anda.")
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(12.dp))
            .clickable(onClick = onRetry)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (location == ReportLocationState.Checking) {
            CircularProgressIndicator(Modifier.size(16.dp), color = fg, strokeWidth = 2.dp)
        } else {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        }
        Text(
            "  $text",
            style = MaterialTheme.typography.bodySmall,
            color = fg,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ReportOption(
    status: DensityStatus,
    speedLabel: String,
    description: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) status.statusColor() else GoOutColors.Outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .background(status.containerColor(), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(18.dp).background(status.statusColor(), CircleShape))
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    status.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GoOutColors.TextPrimary,
                )
                Box(
                    Modifier
                        .padding(start = 8.dp)
                        .background(status.containerColor(), RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(speedLabel, fontSize = 10.sp, color = status.onContainerColor())
                }
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = GoOutColors.TextSecondary)
        }
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
    }
}

@Composable
private fun ResultPanel(
    icon: @Composable () -> Unit,
    title: String,
    message: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(GoOutColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon()
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = GoOutColors.TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = GoOutColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onPrimary,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoOutColors.Primary),
        ) { Text(primaryLabel) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onSecondary,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(999.dp),
        ) { Text(secondaryLabel, color = GoOutColors.TextPrimary) }
    }
}
