package com.epn.bloqueadorspam.ui

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.epn.bloqueadorspam.data.AppDatabase
import com.epn.bloqueadorspam.data.NumeroBloqueado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ==========================================
// ViewModel
// ==========================================
class ListaNegraViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).numeroBloqueadoDao()

    // Convierte el Flow de Room en un StateFlow, ideal para Compose
    val numerosBloqueados: StateFlow<List<NumeroBloqueado>> = dao.obtenerTodos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Mantiene la DB activa 5s tras desaparecer la UI
            initialValue = emptyList()
        )

    fun agregarNumero(numero: String) {
        val numeroLimpio = numero.trim()
        if (numeroLimpio.isNotEmpty()) {
            // Dispatchers.IO es la mejor práctica para inserciones a BD local
            viewModelScope.launch(Dispatchers.IO) {
                dao.insertar(NumeroBloqueado(numero = numeroLimpio))
            }
        }
    }

    fun eliminarNumero(numero: NumeroBloqueado) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.eliminar(numero)
        }
    }
}

// ==========================================
// Interfaz Gráfica (Compose)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal(
    permisosConcedidos: Boolean,
    rolConcedido: Boolean,
    onSolicitarPermisos: () -> Unit,
    onSolicitarRol: () -> Unit,
    viewModel: ListaNegraViewModel = viewModel()
) {
    // Recolectamos el estado reactivo desde el ViewModel
    val numeros by viewModel.numerosBloqueados.collectAsState()
    var nuevoNumero by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bloqueador de Spam") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // 1. Advertencia de Permisos
            if (!permisosConcedidos || !rolConcedido) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Atención: Faltan accesos vitales",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Para que la app intercepte y bloquee llamadas, debes conceder los permisos y el rol de bloqueo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Botón para Permisos si faltan
                        if (!permisosConcedidos) {
                            Button(onClick = onSolicitarPermisos) {
                                Text("Solicitar Permisos")
                            }
                        }
                        
                        // Si ya tiene permisos pero no el rol, pide el rol
                        if (permisosConcedidos && !rolConcedido) {
                            Button(onClick = onSolicitarRol) {
                                Text("Solicitar Rol de Filtrado")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Este dispositivo usa MIUI/HyperOS. Si el bloqueo no funciona a pesar de tener permisos y rol concedidos, ve manualmente a Ajustes del sistema > Apps > Bloqueador de Spam > Batería > Sin restricciones, y también revisa Seguridad > Autoinicio para permitir que la app se inicie en segundo plano.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // 2. Área para añadir números a la lista negra
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = nuevoNumero,
                    onValueChange = { nuevoNumero = it },
                    label = { Text("Número telefónico") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        viewModel.agregarNumero(nuevoNumero)
                        nuevoNumero = "" // Limpia el campo de texto
                    }
                ) {
                    Text("Agregar")
                }
            }

            // 3. Listado de números usando LazyColumn
            Text(
                text = "Lista Negra", 
                style = MaterialTheme.typography.titleLarge
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(numeros, key = { it.numero }) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = item.numero, 
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                // Formateo legible de la fecha en que se insertó a BD
                                val fechaFormateada = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                    .format(Date(item.fechaAgregado))
                                Text(
                                    text = "Agregado: $fechaFormateada", 
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = { viewModel.eliminarNumero(item) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar de la lista negra",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
