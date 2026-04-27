package br.upe.horaDeTomar.ui.users

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import br.upe.horaDeTomar.R
import br.upe.horaDeTomar.ui.AccountViewModel
import br.upe.horaDeTomar.ui.components.CardActionField
import br.upe.horaDeTomar.ui.components.CardSelectField
import br.upe.horaDeTomar.ui.components.CardTextField
import br.upe.horaDeTomar.ui.components.DatePickerModal
import br.upe.horaDeTomar.ui.components.RegisterButton
import br.upe.horaDeTomar.ui.components.SelectPhotoButton
import br.upe.horaDeTomar.ui.components.TakePhotoButton
import br.upe.horaDeTomar.ui.themes.*
import br.upe.horaDeTomar.util.createTempPictureUri
import br.upe.horaDeTomar.util.persistImage
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun UserRegisterScreen(
    onUserRegistered: () -> Unit,
    userViewModel: UsersViewModel = hiltViewModel(),
    accountViewModel: AccountViewModel = hiltViewModel(),
    navController: NavController,
    isFirstTime: Boolean
) {
    var userName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var selectedDate: String? by remember { mutableStateOf(null) }
    var showModal by remember { mutableStateOf(false) }

    var isErrorOnUserName by remember { mutableStateOf(false) }
    var isErrorOnAddress by remember { mutableStateOf(false) }
    var isErrorOnDate by remember { mutableStateOf(false) }
    var isErrorOnGender by remember { mutableStateOf(false) }
    var hasTriedSubmit by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var tempPhotoUri by remember { mutableStateOf(Uri.EMPTY) }

    val singlePhotoSelectContract = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedPhotoUri = uri }
    )

    val singlePhotoTakeContract = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) selectedPhotoUri = tempPhotoUri
        }
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

    val scroll = rememberScrollState()

    // Validação local do CPF
    val cpfDigits = userViewModel.cpf.filter { it.isDigit() }
    val isCpfInvalid = cpfDigits.length != 11
    val cpfErrorMessage = when {
        cpfDigits.isBlank() -> "CPF é obrigatório"
        cpfDigits.length < 11 -> "CPF incompleto"
        else -> null
    }

    if (userViewModel.showRemotePatientDialog) {
        AlertDialog(
            onDismissRequest = { userViewModel.dismissRemotePatientDialog() },
            title = { Text("CPF já cadastrado") },
            text = {
                Text("Já existe um indivíduo cadastrado no servidor com esse CPF. Deseja carregar as informações dele?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val remoteUser = userViewModel.consumeRemotePatient()
                        if (remoteUser != null) {
                            userName = remoteUser.name
                            address = remoteUser.address
                            gender = remoteUser.gender
                            selectedDate = remoteUser.birthDate
                        }
                    }
                ) {
                    Text("Carregar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { userViewModel.dismissRemotePatientDialog() }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(green_background_register_medicine)
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .align(Alignment.Center)
                .verticalScroll(scroll)
                .heightIn(min = maxHeight)
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(green_secondary)
                    .border(
                        BorderStroke(1.dp, md_theme_light_outline),
                        RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_user),
                    contentDescription = "Ícone de usuário",
                    tint = text_balck
                )
            }

            Spacer(Modifier.height(16.dp))

            CardTextField(
                label = "Nome do usuário",
                value = userName,
                onValueChange = {
                    userName = it
                    isErrorOnUserName = it.isBlank()
                },
                placeholder = "Ex: Maria Silva",
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Text,
                isError = isErrorOnUserName,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )

            CardTextField(
                label = "Cpf do usuário",
                value = userViewModel.cpf,
                onValueChange = { value ->
                    // Mantém apenas dígitos e limita a 11
                    val filtered = value.filter { it.isDigit() }.take(11)
                    userViewModel.onCpfChange(filtered)
                },
                placeholder = "Ex: xxx.xxx.xxx-xx",
                keyboardType = KeyboardType.Number,
                visualTransformation = CpfMaskTransformation(),
                isError = hasTriedSubmit && isCpfInvalid,
                errorMessage = if (hasTriedSubmit) cpfErrorMessage else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )

            CardSelectField(
                label = "Sexo",
                value = gender.ifBlank { null },
                onValueChange = {
                    gender = it
                    isErrorOnGender = it.isBlank()
                },
                placeholder = "Selecione o seu sexo",
                options = listOf("Masculino", "Feminino", "Outro"),
                isError = isErrorOnGender,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )

            CardActionField(
                label = "Data de nascimento",
                valueText = selectedDate,
                placeholder = "Selecionar",
                onClick = { showModal = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )

            if (showModal) {
                DatePickerModal(
                    onDateSelected = { millis ->
                        millis?.let {
                            selectedDate = millis.toBrazilianDateFormat()
                            isErrorOnDate = selectedDate.isNullOrBlank()
                        }
                    },
                    onDismiss = { showModal = false }
                )
            }

            CardTextField(
                label = "Endereço",
                value = address,
                onValueChange = {
                    address = it
                    isErrorOnAddress = it.isBlank()
                },
                placeholder = "Ex: Rua das Flores, 123",
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Text,
                isError = isErrorOnAddress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                SelectPhotoButton(
                    onClick = {
                        singlePhotoSelectContract.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                )

                TakePhotoButton(
                    onClick = {
                        focusManager.clearFocus()
                        cameraPermissionState.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp)
                )
            }

            if (selectedPhotoUri != null) {
                Spacer(Modifier.height(8.dp))
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = green_card)
                ) {
                    AsyncImage(
                        model = selectedPhotoUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            RegisterButton(
                onClick = {
                    hasTriedSubmit = true

                    isErrorOnUserName = userName.isBlank()
                    isErrorOnAddress = address.isBlank()
                    isErrorOnDate = selectedDate.isNullOrBlank()
                    isErrorOnGender = gender.isBlank()

                    val isFormValid =
                        userName.isNotBlank() &&
                                address.isNotBlank() &&
                                !selectedDate.isNullOrBlank() &&
                                gender.isNotBlank() &&
                                selectedPhotoUri != null &&
                                !isCpfInvalid

                    if (isFormValid) {
                        coroutineScope.launch {
                            val persistedPath = context.persistImage(selectedPhotoUri!!)

                            if (isFirstTime) {
                                accountViewModel.createAccount(userName)
                            }

                            userViewModel.createUser(
                                userName,
                                address,
                                selectedDate!!,
                                persistedPath,
                                cpfDigits,
                                gender
                            )

                            onUserRegistered()
                        }
                    } else {
                        when {
                            isCpfInvalid -> {
                                Toast.makeText(
                                    context,
                                    cpfErrorMessage ?: "CPF inválido",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            selectedPhotoUri == null -> {
                                Toast.makeText(
                                    context,
                                    "Selecione uma imagem",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            gender.isBlank() -> {
                                Toast.makeText(
                                    context,
                                    "Selecione o sexo",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                },
                label = "Cadastrar-se",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(vertical = 6.dp)
            )
        }
    }
}

class CpfMaskTransformation : VisualTransformation {
    // ###.###.###-##
    override fun filter(text: AnnotatedString): TransformedText {
        val cpfMask = text.text.mapIndexed { index, c ->
            when (index) {
                2 -> "$c."
                5 -> "$c."
                8 -> "$c-"
                else -> c
            }
        }.joinToString(separator = "")

        return TransformedText(AnnotatedString(cpfMask), CpfOffsetMapping)
    }
}

object CpfOffsetMapping : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        return when {
            offset <= 2 -> offset
            offset <= 5 -> offset + 1
            offset <= 8 -> offset + 2
            else -> offset + 3
        }
    }

    override fun transformedToOriginal(offset: Int): Int {
        return when {
            offset <= 2 -> offset
            offset <= 5 -> offset - 1
            offset <= 8 -> offset - 2
            else -> offset - 3
        }
    }
}

fun Long.toBrazilianDateFormat(
    pattern: String = "dd/MM/yyyy"
): String {
    val date = Date(this)
    val formatter = SimpleDateFormat(pattern, Locale("pt", "BR")).apply {
        timeZone = TimeZone.getTimeZone("GMT")
    }
    return formatter.format(date)
}