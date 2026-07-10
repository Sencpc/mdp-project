package mad.project.mdp_project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mad.project.mdp_project.data.DoctorEntity
import mad.project.mdp_project.model.DoctorViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ─── Color Palette ───
private val BrandPrimary = Color(0xFF004B4F)
private val BrandGreen = Color(0xFF2E7D32)
private val BrandGreenLight = Color(0xFFE8F5E9)
private val ChipSelectedBg = Color(0xFF004B4F)
private val ChipUnselectedBg = Color.White
private val ChipBorder = Color(0xFFE0E0E0)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF757575)
private val CardBorder = Color(0xFFE8E8E8)
private val ScreenBg = Color(0xFFF8F9FA)
private val StarColor = Color(0xFFFFB300)
private val DividerColor = Color(0xFFEEEEEE)
private val SearchBg = Color(0xFFF5F5F5)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DoctorConsultScreen(
    viewModel: DoctorViewModel,
    onBackClick: () -> Unit,
    onConsultClick: (DoctorEntity) -> Unit,
    onHistoryClick: () -> Unit = {}
) {
    val doctors by viewModel.doctors.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    val categories = listOf("All", "General Practice", "Therapy", "Nutrition")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
    ) {
        // ─── Header ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Doctor Consult",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Find and connect with available health professionals.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
            IconButton(onClick = onHistoryClick) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Consultation History",
                    tint = TextPrimary
                )
            }
        }

        // ─── Search Bar ───
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = {
                Text(
                    "Search by name or specialty...",
                    color = Color(0xFFADB5BD),
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextSecondary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SearchBg,
                unfocusedContainerColor = SearchBg,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = BrandPrimary
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ─── Filter Chips ───
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setFilter(category) },
                    label = {
                        Text(
                            text = category,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ChipSelectedBg,
                        selectedLabelColor = Color.White,
                        containerColor = ChipUnselectedBg,
                        labelColor = TextPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = ChipBorder,
                        selectedBorderColor = ChipSelectedBg,
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ─── Doctor List ───
        if (doctors.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No available doctors found.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(doctors, key = { it.id }) { doctor ->
                    DoctorCard(
                        doctor = doctor,
                        onConsultClick = { onConsultClick(doctor) }
                    )
                }
                // Bottom spacing
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
fun DoctorCard(
    doctor: DoctorEntity,
    onConsultClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CardBorder)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ─── Top row: Avatar + Info ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Doctor Icon Circle
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconForDoctor(doctor.profileIcon),
                        contentDescription = doctor.category,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Doctor Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = doctor.doctorName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = getCategoryDisplayName(doctor.category),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Rating
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = StarColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${String.format("%.1f", doctor.rating)} (${100 + (doctor.id * 23) % 200} reviews)",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── Description ───
            Text(
                text = doctor.description,
                fontSize = 13.sp,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ─── Divider ───
            HorizontalDivider(
                color = DividerColor,
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ─── Bottom: Next Available + Button ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Next Available",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatAvailableTime(doctor.availableTime),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                OutlinedButton(
                    onClick = onConsultClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = BrandPrimary,
                        contentColor = Color.White
                    ),
                    border = null,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Consult Now",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ─── Helper Functions ───

@Composable
fun getIconForDoctor(iconName: String): ImageVector {
    return when (iconName) {
        "medical_services" -> Icons.Default.MedicalServices
        "stethoscope" -> Icons.Default.MedicalServices // Fallback — stethoscope not in default set
        "local_hospital" -> Icons.Default.LocalHospital
        "psychology" -> Icons.Default.Psychology
        "self_improvement" -> Icons.Default.SelfImprovement
        "spa" -> Icons.Default.Spa
        "nutrition" -> Icons.Default.Restaurant // nutrition maps to restaurant icon
        "restaurant" -> Icons.Default.Restaurant
        "eco" -> Icons.Default.Eco
        else -> Icons.Default.MedicalServices
    }
}

fun getCategoryDisplayName(category: String): String {
    return when (category) {
        "General Practice" -> "General Practice"
        "Therapy" -> "Clinical Psychologist"
        "Nutrition" -> "Registered Dietitian"
        else -> category
    }
}

fun formatAvailableTime(dateTime: LocalDateTime): String {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val date = dateTime.toLocalDate()
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    val formattedTime = dateTime.format(timeFormatter)

    return when (date) {
        today -> "Today, $formattedTime"
        tomorrow -> "Tomorrow, $formattedTime"
        else -> {
            val dayFormatter = DateTimeFormatter.ofPattern("EEE, h:mm a")
            dateTime.format(dayFormatter)
        }
    }
}
