package br.upe.horaDeTomar.ui.homePage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import br.upe.horaDeTomar.ui.components.EmptyStateCard
import br.upe.horaDeTomar.ui.components.MedicineHomePageCard
import br.upe.horaDeTomar.ui.components.SectionHeader
import br.upe.horaDeTomar.ui.components.StatCard
import br.upe.horaDeTomar.ui.components.UserCard
import br.upe.horaDeTomar.ui.medications.MedicationsViewModel
import br.upe.horaDeTomar.ui.users.UsersViewModel
import br.upe.horaDeTomar.ui.themes.*
import br.upe.horaDeTomar.util.helpers.TimeFilter
import kotlinx.coroutines.launch

import java.time.LocalTime

@Composable
fun HomePageScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    userViewModel: UsersViewModel = hiltViewModel(),
    medicationViewModel: MedicationsViewModel = hiltViewModel(),
    homeViewModel: HomePageViewModel = hiltViewModel()
) {
    val usersState by userViewModel.users.collectAsState()
    val medicationsState by medicationViewModel.medications.collectAsState()
    val alarmListState by medicationViewModel.alarmListState.collectAsState()

    val pendingPrescription by homeViewModel.pendingPrescription.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var selectedFilter by rememberSaveable { mutableStateOf(TimeFilter.All) }

    val alarmByMedicationId = remember(alarmListState) {
        alarmListState
            .groupBy { it.medicationId }
    }

    val filteredMeds = remember(medicationsState, alarmByMedicationId, selectedFilter) {
        medicationsState.filter { med ->
            val alarms = alarmByMedicationId[med.id].orEmpty()
            when (selectedFilter) {
                TimeFilter.All -> true
                TimeFilter.Morning   -> alarms.any { it.hour.toIntOrNull()?.let { h -> h in 5..11 } == true }
                TimeFilter.Afternoon -> alarms.any { it.hour.toIntOrNull()?.let { h -> h in 12..17 } == true }
                TimeFilter.Night     -> alarms.any { it.hour.toIntOrNull()?.let { h -> h in 18..23 || h in 0..4 } == true }
            }
        }
    }

    LaunchedEffect(Unit) {
        homeViewModel.searchPrescriptions()
    }

    if (pendingPrescription.isNotEmpty()) {
        val newPrescription = pendingPrescription.first()

        AlertDialog(
            onDismissRequest = { },
            title = { Text(text = "Nova Prescrição Médica!") },
            text = {
                Text(text = "O seu médico prescreveu:\n\n${newPrescription.medicationName}\n${newPrescription.dosageInstruction}\n\nDeseja configurar o alarme para este medicamento agora?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            homeViewModel.markAsProcessed(newPrescription)
                            navController.navigate("registerMedication?prescriptionId=${newPrescription.id}")
                        }
                    }
                ) {
                    Text("Configurar Alarme")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            homeViewModel.markAsProcessed(newPrescription)
                        }
                    }
                ) {
                    Text("Ignorar")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            val start = green_secondary
            val mid   = md_theme_light_primaryContainer
            val end   = white_background

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(Brush.verticalGradient(listOf(start, mid, end)))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Hora de Tomar",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = text_balck // fica elegante no fundo claro
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            label = "Usuários",
                            value = usersState.size.toString()
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            label = "Remédios",
                            value = medicationsState.size.toString()
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            label = "Alarmes",
                            value = alarmListState.size.toString()
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Usuários",
                actionLabel = "Adicionar",
                onAction = { navController.navigate("registerUser") }
            )
        }

        if (usersState.isEmpty()) {
            item { EmptyStateCard("Nenhum usuário cadastrado ainda.") }
        } else {
            items(usersState) { user ->
                UserCard(
                    userName = user.name,
                    age = user.birthDate,
                    imageUri = user.imageUri,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        item { Divider(color = md_theme_light_outline) }

        item {
            Column {
                SectionHeader(
                    title = "Remédios",
                    actionLabel = "Adicionar",
                    onAction = { navController.navigate("registerMedication") }
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeFilter.values().forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = md_theme_light_primaryContainer,
                                selectedLabelColor = md_theme_light_onPrimaryContainer,
                                containerColor = green_secondary,
                                labelColor = text_balck
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == filter,
                                borderColor = md_theme_light_outline
                            )
                        )
                    }
                }
            }
        }

        if (filteredMeds.isEmpty()) {
            item {
                val msg =
                    if (medicationsState.isEmpty()) "Você ainda não cadastrou medicamentos."
                    else "Nenhum medicamento para o filtro selecionado."
                EmptyStateCard(msg)
            }
        } else {
            items(filteredMeds, key = { it.id.hashCode() }) { medication ->
                val times = alarmByMedicationId[medication.id]
                    .orEmpty()
                    .sortedBy { a ->
                        val h = a.hour.toIntOrNull() ?: 0
                        val m = a.minute.toIntOrNull() ?: 0
                        LocalTime.of(h, m)
                    }
                    .map { a -> "${a.hour.twoDigitsOrDash()}:${a.minute.twoDigitsOrDash()}" }
                val timesText = times.joinToString(", ")
                MedicineHomePageCard(
                    medicineName = medication.name,
                    dose = "${medication.dose}",
                    time = timesText,
                    imageUri = medication.imageUri
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private fun String.twoDigitsOrDash(): String = when {
    isBlank() -> "—"
    length >= 2 -> this
    else -> padStart(2, '0')
}
