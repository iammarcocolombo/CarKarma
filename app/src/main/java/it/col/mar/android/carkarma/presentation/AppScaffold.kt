package it.col.mar.android.carkarma.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import it.col.mar.android.carkarma.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    navController: NavController,
    title: String = stringResource(R.string.carkarma),
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) }
            )
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}

@Preview(showBackground = true)
@Composable
fun AppScaffoldPreview() {
    val navController = rememberNavController()
    AppScaffold(navController = navController) { paddingValues ->
        Text(
            text = "Contenuto di test",
            modifier = Modifier.padding(paddingValues)
        )
    }
}
