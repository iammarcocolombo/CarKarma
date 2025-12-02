package it.col.mar.android.carkarma.presentation.login

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(
    state: LoginState,
    onSignInClick: () -> Unit, // Callback per Google
    viewModel: LoginViewModel = viewModel() // Iniettiamo il VM per chiamare le funzioni email
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Mostra errori (Toast) se qualcosa va storto
    LaunchedEffect(key1 = state.signInError) {
        state.signInError?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()), // Permette di scrollare se la tastiera copre i campi
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // --- LOGO E TITOLO ---
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Benvenuto su CarKarma",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- CAMPI EMAIL / PASSWORD ---
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- BOTTONI REGISTRATI / ACCEDI ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = { viewModel.signUpWithEmail(email, password) },
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Text("Registrati")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = { viewModel.signInWithEmail(email, password) },
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Text("Accedi")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- DIVISORE "OPPURE" ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(" oppure ", color = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp))
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- BOTTONE GOOGLE ---
        Button(
            onClick = onSignInClick,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, Color.LightGray)
        ) {
            Text("Continua con Google", color = MaterialTheme.colorScheme.onSurface)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}