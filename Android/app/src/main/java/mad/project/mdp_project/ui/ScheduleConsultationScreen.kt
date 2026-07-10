package mad.project.mdp_project.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mad.project.mdp_project.data.FacilityEntity
import mad.project.mdp_project.model.BookingUiState
import mad.project.mdp_project.model.ConsultationViewModel
import mad.project.mdp_project.model.TimeSlot
import mad.project.mdp_project.model.TimeSlotState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// ─── Color Palette ───
private val BrandPrimary = Color(0xFF004B4F)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF757575)
private val ScreenBg = Color(0xFFF8F9FA)
private val StarColor = Color(0xFFFFB300)
private val DividerColor = Color(0xFFEEEEEE)
private val CardBorder = Color(0xFFE8E8E8)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleConsultationScreen(
    doctorId: Int,
    doctorName: String,
    category: String,
    rating: Float,
    description: String,
    profileIcon: String,
    viewModel: ConsultationViewModel,
    onBackClick: () -> Unit,
    onConfirmed: () -> Unit
) {
    val context = LocalContext.current
    val bookingState by viewModel.bookingState.collectAsState()
    val facilities by viewModel.facilities.collectAsState()

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTimeSlot by remember { mutableStateOf<TimeSlot?>(null) }
    var selectedFacility by remember { mutableStateOf<FacilityEntity?>(null) }
    
    var timeSlots by remember { mutableStateOf<List<TimeSlot>>(emptyList()) }

    // Navigate back on success
    LaunchedEffect(bookingState) {
        if (bookingState is BookingUiState.Success) {
            viewModel.resetState()
            onConfirmed()
        }
    }

    // Load facilities and generate initial time slots
    LaunchedEffect(doctorId, selectedDate) {
        viewModel.loadFacilitiesForDoctor(doctorId)
        timeSlots = viewModel.generateTimeSlots(selectedDate, doctorId)
        
        // Reset selected time if it's no longer available on this new date
        if (selectedTimeSlot != null) {
            val stillAvailable = timeSlots.any { it.hour == selectedTimeSlot!!.hour && it.minute == selectedTimeSlot!!.minute && it.state == TimeSlotState.AVAILABLE }
            if (!stillAvailable) {
                selectedTimeSlot = null
            }
        }
    }

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
            Text(
                text = "Schedule Consultation",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ─── Doctor Info Card ───
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape).background(BrandPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconForDoctor(profileIcon),
                            contentDescription = category,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = doctorName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = getCategoryDisplayName(category), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BrandPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = "Rating", tint = StarColor, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = String.format("%.1f", rating), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─── Date Selection Strip ───
            Text(text = "Select Date", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(30) { offset ->
                    val date = LocalDate.now().plusDays(offset.toLong())
                    val isSunday = date.dayOfWeek == java.time.DayOfWeek.SUNDAY
                    val isSelected = date == selectedDate
                    
                    Card(
                        modifier = Modifier
                            .width(64.dp)
                            .height(80.dp)
                            .clickable(enabled = !isSunday) { 
                                selectedDate = date 
                                viewModel.resetState()
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) BrandPrimary else if (isSunday) Color(0xFFF0F0F0) else Color.White
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) BrandPrimary else CardBorder)
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = date.format(DateTimeFormatter.ofPattern("EEE")),
                                fontSize = 12.sp,
                                color = if (isSelected) Color.White else if (isSunday) TextSecondary else TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = date.dayOfMonth.toString(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else if (isSunday) TextSecondary else TextPrimary
                            )
                            if (isSunday) {
                                Text(text = "Closed", fontSize = 9.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─── Time Selection Grid ───
            Text(text = "Select Time", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))

            if (timeSlots.isNotEmpty()) {
                val morningSlots = timeSlots.filter { it.hour < 12 }
                val afternoonSlots = timeSlots.filter { it.hour >= 12 }
                
                Text(text = "Morning", fontSize = 14.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                TimeSlotGrid(
                    slots = morningSlots, 
                    selectedSlot = selectedTimeSlot, 
                    onSlotSelected = { 
                        selectedTimeSlot = it
                        viewModel.resetState()
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(text = "Afternoon", fontSize = 14.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                TimeSlotGrid(
                    slots = afternoonSlots, 
                    selectedSlot = selectedTimeSlot, 
                    onSlotSelected = { 
                        selectedTimeSlot = it
                        viewModel.resetState()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─── Facility Selection ───
            Text(text = "Healthcare Facility", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Available facilities for this doctor", fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(12.dp))

            if (facilities.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.LocalHospital, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "No facilities available. Please try syncing data.", fontSize = 13.sp, color = TextSecondary)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    facilities.forEach { facility ->
                        val isSelected = selectedFacility?.kodeSatusehat == facility.kodeSatusehat
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    selectedFacility = facility 
                                    viewModel.resetState()
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) BrandPrimary.copy(alpha = 0.05f) else Color.White),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) BrandPrimary else CardBorder))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalHospital,
                                    contentDescription = null,
                                    tint = if (isSelected) BrandPrimary else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = facility.nama,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) BrandPrimary else TextPrimary
                                    )
                                    if (facility.alamat.isNotBlank()) {
                                        Text(
                                            text = facility.alamat,
                                            fontSize = 12.sp,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ─── Booking Summary & Validation ───
            if (selectedTimeSlot != null && selectedFacility != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandPrimary.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Summary", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        val formattedTime = String.format("%02d:%02d", selectedTimeSlot!!.hour, selectedTimeSlot!!.minute)
                        Text(text = "Date: ${selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))} at $formattedTime", fontSize = 13.sp, color = TextPrimary)
                        Text(text = "Location: ${selectedFacility!!.nama}", fontSize = 13.sp, color = TextPrimary)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Inline Validation Error Card
            if (bookingState is BookingUiState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDEDED))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = (bookingState as BookingUiState.Error).message, color = Color(0xFFD32F2F), fontSize = 13.sp)
                    }
                }
            }

            // Confirm Button
            Button(
                onClick = {
                    if (selectedTimeSlot != null && selectedFacility != null) {
                        val consultationTime = LocalDateTime.of(selectedDate, LocalTime.of(selectedTimeSlot!!.hour, selectedTimeSlot!!.minute))
                        viewModel.validateAndBook(
                            doctorId = doctorId,
                            doctorName = doctorName,
                            category = category,
                            consultationTime = consultationTime,
                            facilityKodeSatusehat = selectedFacility!!.kodeSatusehat,
                            facilityName = selectedFacility!!.nama,
                            profileIcon = profileIcon
                        )
                    }
                },
                enabled = selectedTimeSlot != null && selectedFacility != null && bookingState !is BookingUiState.Saving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                Text(
                    text = if (bookingState is BookingUiState.Saving) "Confirming..." else "Confirm Consultation",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimeSlotGrid(
    slots: List<TimeSlot>,
    selectedSlot: TimeSlot?,
    onSlotSelected: (TimeSlot) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 4
    ) {
        slots.forEach { slot ->
            val isSelected = selectedSlot?.hour == slot.hour && selectedSlot?.minute == slot.minute
            val timeText = String.format("%02d:%02d", slot.hour, slot.minute)
            
            val (bgColor, contentColor, borderColor) = when {
                isSelected -> Triple(BrandPrimary, Color.White, BrandPrimary)
                slot.state == TimeSlotState.AVAILABLE -> Triple(Color.White, BrandPrimary, CardBorder)
                slot.state == TimeSlotState.BREAK -> Triple(Color(0xFFF5F5F5), TextSecondary, Color.Transparent)
                slot.state == TimeSlotState.TOO_SOON -> Triple(Color(0xFFF5F5F5), TextSecondary, Color.Transparent)
                slot.state == TimeSlotState.BOOKED -> Triple(Color(0xFFFDEDED), Color(0xFFD32F2F), Color.Transparent)
                else -> Triple(Color.White, BrandPrimary, CardBorder)
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minWidth = 72.dp)
                    .clickable(enabled = slot.state == TimeSlotState.AVAILABLE) { 
                        onSlotSelected(slot) 
                    },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor),
                border = BorderStroke(1.dp, borderColor),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = timeText,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor,
                        textDecoration = if (slot.state == TimeSlotState.BREAK) TextDecoration.LineThrough else null
                    )
                    
                    if (slot.state != TimeSlotState.AVAILABLE && !isSelected) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (slot.state) {
                                TimeSlotState.BREAK -> "☕ Break"
                                TimeSlotState.TOO_SOON -> "⏰ Soon"
                                TimeSlotState.BOOKED -> "📋 Booked"
                                else -> ""
                            },
                            fontSize = 9.sp,
                            color = contentColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
