package com.epn.bloqueadorspam.ui

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.epn.bloqueadorspam.data.AppDatabase
import com.epn.bloqueadorspam.data.LlamadaBloqueada
import com.epn.bloqueadorspam.data.NumeroBloqueado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ListaNegraViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val dao = db.numeroBloqueadoDao()
    private val historialDao = db.llamadaBloqueadaDao()

    val numerosBloqueados: StateFlow<List<NumeroBloqueado>> = dao.obtenerTodos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val historial: StateFlow<List<LlamadaBloqueada>> = historialDao.obtenerHistorial()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalBloqueos: StateFlow<Int> = historialDao.contarBloqueos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun agregarNumero(numero: String) {
        val numeroLimpio = com.epn.bloqueadorspam.utils.normalizarNumeroTelefono(numero)
        if (numeroLimpio.isNotEmpty()) {
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
    
    fun desbloquearNumero(numero: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.eliminarPorNumero(numero)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal(
    permisosConcedidos: Boolean,
    rolConcedido: Boolean,
    onSolicitarPermisos: () -> Unit,
    onSolicitarRol: () -> Unit,
    viewModel: ListaNegraViewModel = viewModel()
) {
    val numeros by viewModel.numerosBloqueados.collectAsState()
    val historial by viewModel.historial.collectAsState()
    val totalBloqueos by viewModel.totalBloqueos.collectAsState()
    
    var nuevoNumero by remember { mutableStateOf("") }
    
    // Estado para el diálogo de confirmación
    var showDialog by remember { mutableStateOf(false) }
    var numeroADesbloquear by remember { mutableStateOf("") }
    
    // Seleccionar entre pestaña de lista negra y de historial
    var currentTab by remember { mutableStateOf(0) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("¿Desbloquear número?") },
            text = { Text("¿Seguro que quieres desbloquear este número y quitarlo de la lista negra?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.desbloquearNumero(numeroADesbloquear)
                    showDialog = false
                }) {
                    Text("Confirmar", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.DarkGray
        )
    }

    val backgroundColor = Color(0xFFF5F5F5)

    Scaffold(
        topBar = {
            // Header estilo Budget App
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.Black,
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    )
                    .padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
            ) {
                Column {
                    Text(
                        text = "Bloqueador Spam",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$totalBloqueos llamadas bloqueadas",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }
            }
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            
            // Advertencia de Permisos
            if (!permisosConcedidos || !rolConcedido) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Atención: Faltan accesos vitales",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Para que la app intercepte llamadas, debes conceder los permisos y el rol.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            if (!permisosConcedidos) {
                                Button(
                                    onClick = onSolicitarPermisos,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                                ) {
                                    Text("Solicitar Permisos", color = Color.White)
                                }
                            }
                            if (permisosConcedidos && !rolConcedido) {
                                Button(
                                    onClick = onSolicitarRol,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                                ) {
                                    Text("Solicitar Rol de Filtrado", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Main "Spending" Card style
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF1C1C1C), Color(0xFF3A3A3A))
                                )
                            )
                            .padding(24.dp)
                    ) {
                        // Icon top left
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock", tint = Color.White)
                        }
                        
                        // Badge top right
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "Activo",
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Big stat bottom
                        Column(modifier = Modifier.align(Alignment.BottomStart)) {
                            Text(text = "Total Bloqueadas", color = Color.LightGray, fontSize = 14.sp)
                            Text(text = "$totalBloqueos", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Input Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp).fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Phone",
                            tint = Color.Gray,
                            modifier = Modifier.padding(start = 8.dp, end = 8.dp)
                        )
                        OutlinedTextField(
                            value = nuevoNumero,
                            onValueChange = { nuevoNumero = it },
                            placeholder = { Text("Agregar a lista negra...", color = Color.Gray) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = Color.Black,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            )
                        )
                        IconButton(
                            onClick = {
                                viewModel.agregarNumero(nuevoNumero)
                                nuevoNumero = ""
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black, CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Agregar", tint = Color.White)
                        }
                    }
                }
            }

            // Tabs for Lists
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TabRow(
                        selectedTabIndex = currentTab,
                        containerColor = Color.Transparent,
                        contentColor = Color.Black,
                        divider = {},
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[currentTab]),
                                color = Color.Black
                            )
                        }
                    ) {
                        Tab(
                            selected = currentTab == 0,
                            onClick = { currentTab = 0 },
                            text = { Text("Lista Negra", fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal) }
                        )
                        Tab(
                            selected = currentTab == 1,
                            onClick = { currentTab = 1 },
                            text = { Text("Historial", fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }

            if (currentTab == 0) {
                if (numeros.isEmpty()) {
                    item {
                        EmptyState("La lista negra está vacía")
                    }
                } else {
                    items(numeros, key = { "ln_${it.numero}" }) { item ->
                        ListaNegraItem(item = item) {
                            numeroADesbloquear = item.numero
                            showDialog = true
                        }
                    }
                }
            } else {
                if (historial.isEmpty()) {
                    item {
                        EmptyState("No hay llamadas bloqueadas aún")
                    }
                } else {
                    items(historial, key = { "h_${it.id}" }) { item ->
                        HistorialItem(item = item) {
                            numeroADesbloquear = item.numero
                            showDialog = true
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Info, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, color = Color.Gray, fontSize = 16.sp)
        }
    }
}

@Composable
fun ListaNegraItem(item: NumeroBloqueado, onEliminar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(Color(0xFFF5F5F5), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(item.numero, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    val fecha = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(item.fechaAgregado))
                    Text("Agregado: $fecha", fontSize = 12.sp, color = Color.Gray)
                }
            }
            IconButton(onClick = onEliminar) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.DarkGray)
            }
        }
    }
}

@Composable
fun HistorialItem(item: LlamadaBloqueada, onDesbloquear: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(Color(0xFFF5F5F5), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(item.numero, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    val fecha = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(item.fecha))
                    Text("Bloqueado: $fecha", fontSize = 12.sp, color = Color.Gray)
                }
            }
            TextButton(
                onClick = onDesbloquear,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Black)
            ) {
                Text("Desbloquear")
            }
        }
    }
}
