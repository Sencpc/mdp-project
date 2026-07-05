package mad.project.mdp_project.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import mad.project.mdp_project.R
import mad.project.mdp_project.data.AppDatabase
import mad.project.mdp_project.data.SessionManager
import mad.project.mdp_project.model.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val context = LocalContext.current
                val sessionManager = remember { SessionManager(context) }
                val userId = sessionManager.getUserId()
                val db = remember { AppDatabase.getDatabase(context) }
                val viewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModel.Factory(db.userDao(), userId)
                )

                ProfileScreen(
                    viewModel = viewModel,
                    onBackClick = { findNavController().popBackStack() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBackClick: () -> Unit
) {
    val user by viewModel.user.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = Color(0xFFD1E8D1)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.padding(4.dp),
                                tint = Color(0xFF004B4F)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "MindfulLife",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF004B4F)
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            // Replicating the Bottom Nav visual from the screenshot
            Surface(
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Column {
                    Button(
                        onClick = { /* Save handled by individual edits or a final sync if needed */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003333)),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    // Mock Bottom Nav Icons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        BottomNavItem(Icons.Default.Home, "Dashboard")
                        BottomNavItem(Icons.Default.CheckCircle, "Habits")
                        BottomNavItem(Icons.Default.Timer, "Screen")
                        BottomNavItem(Icons.Default.NightsStay, "Sleep")
                    }
                }
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            Text(
                "Personal Data",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF004B4F)
                )
            )
            Text(
                "Manage your health metrics and profile information.",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            ProfileMetricCard(
                label = "Height",
                value = "${user?.height ?: "--"}",
                unit = "cm",
                icon = Icons.Default.Height,
                onEditClick = { /* Show Dialog */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProfileMetricCard(
                label = "Weight",
                value = "${user?.weight ?: "--"}",
                unit = "kg",
                icon = Icons.Default.Scale,
                onEditClick = { /* Show Dialog */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            val age = viewModel.calculateAge(user?.birthDate)
            val dobStr = user?.birthDate?.let {
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))
            } ?: "Not set"

            ProfileMetricCard(
                label = "Age",
                value = "$age",
                unit = "years",
                subtitle = "DOB: $dobStr",
                icon = Icons.Default.CalendarToday,
                onEditClick = { /* Show Date Picker */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProfileMetricCard(
                label = "Blood Type",
                value = user?.bloodType ?: "--",
                unit = "",
                icon = Icons.Default.WaterDrop,
                onEditClick = { /* Show Dropdown */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ConditionsCard(
                conditions = user?.conditions?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
                emergencyName = user?.emergencyContactName ?: "Not set",
                emergencyPhone = user?.emergencyContactPhone ?: "",
                onAddCondition = { /* Show Dialog */ },
                onEditEmergency = { /* Show Dialog */ }
            )
            
            Spacer(modifier = Modifier.height(100.dp)) // Extra space for FAB/Bottom button
        }
    }
}

@Composable
fun ProfileMetricCard(
    label: String,
    value: String,
    unit: String,
    subtitle: String? = null,
    icon: ImageVector,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.DarkGray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    if (unit.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(unit, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
                if (subtitle != null) {
                    Text(subtitle, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                }
            }
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp), tint = Color.DarkGray)
            }
        }
    }
}

@Composable
fun ConditionsCard(
    conditions: List<String>,
    emergencyName: String,
    emergencyPhone: String,
    onAddCondition: () -> Unit,
    onEditEmergency: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Conditions", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF003333))
                IconButton(onClick = onAddCondition) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.DarkGray)
                }
            }
            
            FlowRow(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                conditions.forEach { condition ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF0F0F0),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(condition, fontSize = 12.sp, color = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))

            Text("Emergency Contact", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(emergencyName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(emergencyPhone, fontSize = 12.sp, color = Color.Gray)
                    }
                    IconButton(onClick = onEditEmergency) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp), tint = Color(0xFF004B4F))
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavItem(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp), tint = Color.Gray)
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        content = { content() }
    )
}
