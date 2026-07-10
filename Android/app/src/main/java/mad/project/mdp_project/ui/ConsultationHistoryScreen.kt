package mad.project.mdp_project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import mad.project.mdp_project.data.ConsultationEntity
import mad.project.mdp_project.model.ConsultationHistoryViewModel
import java.time.format.DateTimeFormatter

// Reuses color palette from DoctorConsultScreen
private val BrandPrimary = Color(0xFF004B4F)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF757575)
private val ScreenBg = Color(0xFFF8F9FA)
private val StarColor = Color(0xFFFFB300)
private val CardBorder = Color(0xFFE8E8E8)
private val DividerColor = Color(0xFFEEEEEE)
private val CompletedBadge = Color(0xFF4CAF50)
private val UpcomingBadge = Color(0xFF2196F3)

@Composable
fun ConsultationHistoryScreen(
    viewModel: ConsultationHistoryViewModel,
    onBackClick: () -> Unit
) {
    val consultations by viewModel.allConsultations.collectAsState()
    val scope = rememberCoroutineScope()
    var showReviewDialog by remember { mutableStateOf<ConsultationEntity?>(null) }

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
            Column {
                Text(
                    text = "Consultation History",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Your past and upcoming consultations.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }

        if (consultations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No consultations yet.",
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
                items(consultations, key = { it.id }) { consultation ->
                    ConsultationHistoryCard(
                        consultation = consultation,
                        onReviewClick = {
                            scope.launch {
                                if (!viewModel.hasReview(consultation.id)) {
                                    showReviewDialog = consultation
                                }
                            }
                        },
                        viewModel = viewModel
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }

    // Review Dialog
    showReviewDialog?.let { consultation ->
        ReviewDialog(
            doctorName = consultation.doctorName,
            onDismiss = { showReviewDialog = null },
            onSubmit = { rating, comment ->
                viewModel.submitReview(
                    consultationId = consultation.id,
                    doctorId = consultation.doctorId,
                    rating = rating,
                    comment = comment
                )
                showReviewDialog = null
            }
        )
    }
}

@Composable
private fun ConsultationHistoryCard(
    consultation: ConsultationEntity,
    onReviewClick: () -> Unit,
    viewModel: ConsultationHistoryViewModel
) {
    val isCompleted = consultation.status == "Completed"
    val scope = rememberCoroutineScope()
    var hasReview by remember { mutableStateOf(true) } // default true to hide button until checked

    // Check if review exists
    if (isCompleted) {
        androidx.compose.runtime.LaunchedEffect(consultation.id) {
            hasReview = viewModel.hasReview(consultation.id)
        }
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Doctor Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconForDoctor(consultation.profileIcon),
                        contentDescription = consultation.category,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = consultation.doctorName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = getCategoryDisplayName(consultation.category),
                        fontSize = 13.sp,
                        color = BrandPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Status Badge
                Text(
                    text = consultation.status,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) CompletedBadge else UpcomingBadge,
                    modifier = Modifier
                        .background(
                            color = if (isCompleted) CompletedBadge.copy(alpha = 0.1f) else UpcomingBadge.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date & Facility
            val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy • h:mm a")
            Text(
                text = consultation.consultationTime.format(dateFormatter),
                fontSize = 13.sp,
                color = TextSecondary
            )

            if (consultation.facilityName.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = consultation.facilityName,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Review button for completed consultations without a review
            if (isCompleted && !hasReview) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = DividerColor)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onReviewClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BrandPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RateReview,
                        contentDescription = "Review",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Rate & Review",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
