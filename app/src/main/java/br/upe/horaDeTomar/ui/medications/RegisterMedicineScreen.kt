package br.upe.horaDeTomar.ui.medications

import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import br.upe.horaDeTomar.ui.components.CardTextField
import br.upe.horaDeTomar.ui.components.CardSelectField
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.upe.horaDeTomar.R
import br.upe.horaDeTomar.data.entities.Medication
import br.upe.horaDeTomar.ui.components.CardActionField
import br.upe.horaDeTomar.ui.components.DropDownMenu
import br.upe.horaDeTomar.ui.components.FieldTextOutlined
import br.upe.horaDeTomar.ui.components.RegisterButton
import br.upe.horaDeTomar.ui.components.SelectPhotoButton
import br.upe.horaDeTomar.ui.components.TakePhotoButton
import br.upe.horaDeTomar.ui.themes.*
import br.upe.horaDeTomar.util.createTempPictureUri
import br.upe.horaDeTomar.util.persistImage
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.util.Calendar
import androidx.compose.foundation.lazy.items

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterMedicineScreen(
    viewModel: MedicationsViewModel = hiltViewModel(),
    navControler: NavController,
) {
    val medicationState = viewModel.medicationCreationState
    val searchResults = viewModel.searchResults

    var medicineName by remember { mutableStateOf("") }
    var via by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    val viaList = listOf("Oral", "Tópico", "Sublingual")

    val currentTime = Calendar.getInstance() // mantém se você precisar depois

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempPhotoUri by remember { mutableStateOf(Uri.EMPTY) }

    val singlePhotoSelectContract = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )
    val singlePhotoTakeContract = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success -> if (success) selectedImageUri = tempPhotoUri }
    )
    val cameraPermissionState = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                tempPhotoUri = context.createTempPictureUri()
                singlePhotoTakeContract.launch(tempPhotoUri)
            } else {
                Toast.makeText(context, "Permissão da câmera negada", Toast.LENGTH_SHORT).show()
            }
        }
    )

    var isErrorOnMedicineName by remember { mutableStateOf(false) }
    var isErrorOnVia by remember { mutableStateOf(false) }
    var isErrorOnDose by remember { mutableStateOf(false) }

    val timesLabel = viewModel.pendingAlarms
        .mapNotNull { a ->
            val h = a.hour.toIntOrNull()
            val m = a.minute.toIntOrNull()
            if (h == null || m == null) null else "%02d:%02d".format(h, m)
        }
        .distinct()
        .sorted()
        .joinToString(", ")

    val scrollState = rememberScrollState()

    var showAlarmSettingsDialog by remember { mutableStateOf(false) }

    BoxWithConstraints (
        modifier = Modifier
            .fillMaxSize()
            .background(green_background_register_medicine)
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .align(Alignment.Center)
                .verticalScroll(scrollState)
                .heightIn(min = maxHeight)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(green_secondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_pill),
                    contentDescription = "Ícone de medicamento",
                    tint = text_balck
                )
            }

            Spacer(Modifier.height(16.dp))

            CardTextField(
                label = "Nome do medicamento",
                value = medicationState.name,
                onValueChange = { viewModel.onMedicationNameChange(it) },
                placeholder = "Ex: Dipirona",
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Text,
                isError = isErrorOnMedicineName,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )

            AnimatedVisibility(visible = searchResults.isNotEmpty()) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
                ) {
                    LazyColumn {
                        items(searchResults) { result ->
                            ListItem(
                                headlineContent = { Text(result.name, fontWeight = FontWeight.Medium) },
                                supportingContent = {
                                    if(result.code != null) Text("Cód: ${result.code}", fontSize = 12.sp, color = Color.Gray)
                                },
                                modifier = Modifier.clickable {
                                    viewModel.onMedicationSelected(result)
                                    focusManager.clearFocus()
                                }
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                        }
                    }
                }
            }

            CardSelectField(
                label = "Via",
                value = via.ifBlank { null },
                onValueChange = {
                    via = it
                    isErrorOnVia = it.isBlank()
                },
                placeholder = "Selecione a via de administração",
                options = listOf("Oral", "Tópico", "Sublingual"),
                isError = isErrorOnVia,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )

            CardTextField(
                label = "Dose",
                value = dose,
                onValueChange = {
                    dose = it
                    isErrorOnDose = it.isBlank()
                },
                placeholder = "Ex: 500mg",
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Number,
                isError = isErrorOnDose,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )



            Spacer(Modifier.height(8.dp))

            CardActionField(
                label = "Horário e dias da semana",
                valueText = timesLabel.ifBlank { null },
                placeholder = "Selecionar",
                onClick = { showAlarmSettingsDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )

            if (showAlarmSettingsDialog) {
                CreateAlarmDialog(
                    alarmCreationState = viewModel.alarmCreationState,
                    alarmActions = viewModel,
                    navigateToAlarmList = {
                        showAlarmSettingsDialog = false
                    },
                    onDismissRequest = {
                        showAlarmSettingsDialog = false
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth()) {
                SelectPhotoButton(
                    onClick = {
                        singlePhotoSelectContract.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                )
                TakePhotoButton(
                    onClick = {
                        focusManager.clearFocus()
                        cameraPermissionState.launch(android.Manifest.permission.CAMERA)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                )
            }

            if (selectedImageUri != null) {
                Spacer(Modifier.height(8.dp))
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = green_card)
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            RegisterButton(
                onClick = {
                    if (medicationState.name.isNotEmpty() && via.isNotBlank() && dose.isNotBlank() && selectedImageUri != null) {
                        coroutineScope.launch {
                            val persistedPath = context.persistImage(selectedImageUri!!)
                            val medication = Medication(
                                name = medicationState.name,
                                via = via,
                                dose = dose,
                                userId = 1,
                                imageUri = persistedPath
                            )
                            viewModel.updateMedicationCreationState(medication)
                            viewModel.createMedication()
                            navControler.popBackStack()
                        }
                    } else {
                        isErrorOnMedicineName = medicationState.name.isBlank()
                        isErrorOnVia = via.isBlank()
                        isErrorOnDose = dose.isBlank()
                        if (selectedImageUri == null) {
                            Toast.makeText(context, "Selecione uma imagem", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                label = "Cadastrar Medicamento",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(vertical = 6.dp)
            )

            Spacer(Modifier.height(8.dp))

        }
    }
}
