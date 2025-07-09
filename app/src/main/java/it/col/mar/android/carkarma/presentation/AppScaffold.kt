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
import it.col.mar.android.carkarma.ui.theme.CarKarmaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    navController: NavController? = null,  // opzionale per le preview
    title: String = stringResource(R.string.app_name),
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}

@Preview(showBackground = true)
@Composable
fun AppScaffoldPreview() {
    CarKarmaTheme {
        val navController = rememberNavController()
        AppScaffold(navController = navController) { paddingValues ->
            Text(
                text = "Contenuto di test",
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}
