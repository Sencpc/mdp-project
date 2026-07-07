package mad.project.mdp_project.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import coil.compose.AsyncImage
import mad.project.mdp_project.R
import mad.project.mdp_project.data.AppDatabase
import mad.project.mdp_project.data.SessionManager
import mad.project.mdp_project.model.ProfileViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {
    //test
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
    // Menggunakan draftUser agar perubahan terlihat di UI sebelum di-save ke DB
    val user by viewModel.draftUser.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // Dialog & UI States
    var showNameDialog by remember { mutableStateOf(false) }
    var showPhotoOptions by remember { mutableStateOf(false) }
    var showHeightDialog by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var showBloodDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var showConditionsLibrary by remember { mutableStateOf(false) }

    // Image Pickers Logic
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { selectedUri -> viewModel.updateDraft { it.copy(profilePicturePath = selectedUri.toString()) } }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempImageUri?.let { capturedUri -> viewModel.updateDraft { it.copy(profilePicturePath = capturedUri.toString()) } }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = Color(0xFF004B4F)
                        ) {
                            Icon(Icons.Default.Person, null, Modifier.padding(8.dp), Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("MindfulLife", style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold, color = Color(0xFF004B4F), fontSize = 20.sp
                        ))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(painter = painterResource(id = R.drawable.ic_dash_notification),
                            contentDescription = "Notifications", modifier = Modifier.size(24.dp), tint = Color(0xFF004B4F))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            // PROFILE HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.clickable { showPhotoOptions = true }) {
                    AsyncImage(
                        model = user?.profilePicturePath ?: R.drawable.bg_circle_green,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0E0E0)),
                        contentScale = ContentScale.Crop
                    )
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = CircleShape,
                        color = Color(0xFF004B4F),
                        border = BorderStroke(2.dp, Color.White)
                    ) {
                        Icon(Icons.Default.CameraAlt, null, Modifier.padding(4.dp), Color.White)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showNameDialog = true }
                    ) {
                        Text(
                            text = user?.fullName?.ifEmpty { "Add Name" } ?: "User",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF004B4F)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Name",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF004B4F)
                        )
                    }
                    Text(
                        text = user?.username?.let { "@$it" } ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Text("Personal Data", style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold, color = Color(0xFF004B4F)
            ))
            Text("Manage your health metrics and profile information.",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

            ProfileMetricCard("Height", "${user?.height?.toInt() ?: "--"}", "cm", Icons.Default.Straighten) { showHeightDialog = true }
            Spacer(Modifier.height(16.dp))
            ProfileMetricCard("Weight", "${user?.weight?.toInt() ?: "--"}", "kg", Icons.Default.MonitorWeight) { showWeightDialog = true }
            Spacer(Modifier.height(16.dp))

            val age = viewModel.calculateAge(user?.birthDate)
            val dobStr = user?.birthDate?.let { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it)) } ?: "Not set"
            ProfileMetricCard("Age", "$age", "years", Icons.Default.Event, "DOB: $dobStr") { showDatePicker = true }
            Spacer(Modifier.height(16.dp))
            ProfileMetricCard("Blood Type", user?.bloodType ?: "--", "", Icons.Default.Opacity) { showBloodDialog = true }
            Spacer(Modifier.height(16.dp))

            ConditionsCard(
                conditions = user?.conditions?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
                emergencyName = user?.emergencyContactName ?: "Not set",
                emergencyPhone = user?.emergencyContactPhone ?: "",
                onAddCondition = { showConditionsLibrary = true },
                onRemoveCondition = { conditionToRemove ->
                    val currentList = user?.conditions?.split(",")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
                    currentList.remove(conditionToRemove)
                    viewModel.updateDraft { it.copy(conditions = currentList.joinToString(",")) }
                },
                onEditEmergency = { showEmergencyDialog = true }
            )

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    viewModel.saveChanges(onComplete = onBackClick)
                },
                modifier = Modifier.align(Alignment.End).height(56.dp).width(180.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003333)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    // --- Dialog Implementations ---

    if (showConditionsLibrary) {
        ConditionsLibraryDialog(
            onDismiss = { showConditionsLibrary = false },
            onConditionSelected = { condition ->
                val currentList = user?.conditions?.split(",")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
                if (!currentList.contains(condition)) {
                    currentList.add(condition)
                    viewModel.updateDraft { it.copy(conditions = currentList.joinToString(",")) }
                }
                showConditionsLibrary = false
            }
        )
    }

    if (showPhotoOptions) {
        AlertDialog(
            onDismissRequest = { showPhotoOptions = false },
            title = { Text("Profile Photo") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Take Photo") },
                        leadingContent = { Icon(Icons.Default.Camera, null) },
                        modifier = Modifier.clickable {
                            showPhotoOptions = false
                            val file = File(context.filesDir, "profile_${System.currentTimeMillis()}.jpg")
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            tempImageUri = uri
                            takePicture.launch(uri)
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Choose from Gallery") },
                        leadingContent = { Icon(Icons.Default.PhotoLibrary, null) },
                        modifier = Modifier.clickable {
                            showPhotoOptions = false
                            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    )
                    if (user?.profilePicturePath != null) {
                        ListItem(
                            headlineContent = { Text("Remove Photo", color = Color.Red) },
                            leadingContent = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                            modifier = Modifier.clickable {
                                showPhotoOptions = false
                                viewModel.updateDraft { it.copy(profilePicturePath = null) }
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showNameDialog) {
        var nameText by remember { mutableStateOf(user?.fullName ?: "") }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Edit Name") },
            text = { OutlinedTextField(value = nameText, onValueChange = { nameText = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { Button(onClick = { viewModel.updateDraft { it.copy(fullName = nameText) }; showNameDialog = false }) { Text("Save") } }
        )
    }

    if (showHeightDialog) {
        EditNumberDialog("Edit Height", "Height (cm)", user?.height?.toInt()?.toString() ?: "", onDismiss = { showHeightDialog = false }) { newValue ->
            viewModel.updateDraft { it.copy(height = newValue.toFloatOrNull() ?: 0f) }
            showHeightDialog = false
        }
    }
    if (showWeightDialog) {
        EditNumberDialog("Edit Weight", "Weight (kg)", user?.weight?.toInt()?.toString() ?: "", onDismiss = { showWeightDialog = false }) { newValue ->
            viewModel.updateDraft { it.copy(weight = newValue.toFloatOrNull() ?: 0f) }
            showWeightDialog = false
        }
    }
    if (showBloodDialog) {
        BloodTypeDialog(onDismiss = { showBloodDialog = false }) { selectedType ->
            viewModel.updateDraft { it.copy(bloodType = selectedType) }
            showBloodDialog = false
        }
    }
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.updateDraft { u -> u.copy(birthDate = it) } }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }
    if (showEmergencyDialog) {
        EmergencyContactDialog(
            currentName = user?.emergencyContactName ?: "",
            currentPhone = user?.emergencyContactPhone ?: "",
            onDismiss = { showEmergencyDialog = false },
            onSave = { name, phone ->
                viewModel.updateDraft { it.copy(emergencyContactName = name, emergencyContactPhone = phone) }
                showEmergencyDialog = false
            }
        )
    }
}

@Composable
fun ConditionsLibraryDialog(onDismiss: () -> Unit, onConditionSelected: (String) -> Unit) {
    val library = mapOf(
        "Respiratory" to listOf("Asthma", "COPD", "Bronchitis", "Pneumonia"),
        "Cardiovascular" to listOf("Hypertension", "Coronary Artery Disease", "Arrhythmia", "Heart Failure"),
        "Neurological" to listOf("Migraine", "Epilepsy", "Multiple Sclerosis", "Parkinson's"),
        "Digestive" to listOf("GERD", "IBS", "Crohn's Disease", "Celiac Disease"),
        "Allergies" to listOf("Peanut Allergy", "Pollen Allergy", "Dust Mite Allergy", "Lactose Intolerance"),
        "Endocrine" to listOf("Diabetes Type 1", "Diabetes Type 2", "Hypothyroidism", "Hyperthyroidism"),
        "Mental Health" to listOf("Anxiety", "Depression", "Bipolar Disorder", "PTSD")
    )

    var searchQuery by remember { mutableStateOf("") }
    var customCondition by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxSize().background(Color.White),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                    Text("Medical Conditions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    placeholder = { Text("Search conditions...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = RoundedCornerShape(12.dp)
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    val filteredLibrary = library.mapValues { (_, conditions) ->
                        conditions.filter { it.contains(searchQuery, ignoreCase = true) }
                    }.filterValues { it.isNotEmpty() }

                    if (filteredLibrary.isEmpty() && searchQuery.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No matching condition found", color = Color.Gray)
                                Spacer(Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = customCondition,
                                    onValueChange = { customCondition = it },
                                    label = { Text("Add custom condition") }
                                )
                                Button(
                                    onClick = { if(customCondition.isNotBlank()) onConditionSelected(customCondition) },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) { Text("Add Custom") }
                            }
                        }
                    } else {
                        filteredLibrary.forEach { (category, conditions) ->
                            item {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF004B4F),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(conditions) { condition ->
                                ListItem(
                                    headlineContent = { Text(condition) },
                                    trailingContent = { Icon(Icons.Default.Add, null, tint = Color.Gray) },
                                    modifier = Modifier.clickable { onConditionSelected(condition) }
                                )
                                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFEEEEEE))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditNumberDialog(title: String, label: String, initialValue: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()) },
        confirmButton = { Button(onClick = { onSave(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun BloodTypeDialog(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val types = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Blood Type") },
        text = {
            Column {
                types.forEach { type ->
                    TextButton(onClick = { onSelect(type) }, modifier = Modifier.fillMaxWidth()) { Text(type, color = Color.Black) }
                }
            }
        }, confirmButton = {}
    )
}

@Composable
fun EmergencyContactDialog(currentName: String, currentPhone: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    var phone by remember { mutableStateOf(currentPhone) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Emergency Contact") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onSave(name, phone) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ProfileMetricCard(label: String, value: String, unit: String, icon: ImageVector, subtitle: String? = null, onEditClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, modifier = Modifier.size(16.dp), tint = Color.DarkGray)
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
                Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(20.dp), tint = Color.DarkGray)
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
    onRemoveCondition: (String) -> Unit,
    onEditEmergency: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Conditions", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF003333))
                IconButton(onClick = onAddCondition) { Icon(Icons.Default.Add, "Add", tint = Color.DarkGray) }
            }
            FlowRow(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                conditions.forEach { condition ->
                    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFF0F0F0), modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(condition, fontSize = 12.sp, color = Color.Black)
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Close,
                                null,
                                modifier = Modifier.size(14.dp).clickable { onRemoveCondition(condition) },
                                tint = Color.Gray
                            )
                        }
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))
            Text("Emergency Contact", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFEEEEEE))) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(emergencyName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(emergencyPhone, fontSize = 12.sp, color = Color.Gray)
                    }
                    IconButton(onClick = onEditEmergency) { Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(20.dp), tint = Color(0xFF004B4F)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(modifier: Modifier = Modifier, horizontalArrangement: Arrangement.Horizontal = Arrangement.Start, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(modifier = modifier, horizontalArrangement = horizontalArrangement, content = { content() })
}
