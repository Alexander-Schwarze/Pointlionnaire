import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.tkuenneth.nativeparameterstoreaccess.NativeParameterStoreAccess
import com.github.tkuenneth.nativeparameterstoreaccess.WindowsRegistry
import handler.QuestionHandler
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

val lightColorPalette = lightColors(
    primary = Color(0xff4466ff),
    onPrimary = Color.White,
    secondary = Color(0xff0b5b8e),
    background = Color.White,
    onBackground = Color.Black,
)

val darkColorPalette = darkColors(
    primary = Color(0xff2244bb),
    onPrimary = Color.White,
    secondary = Color(0xff5bbbfe),
    background = Color.DarkGray,
    onBackground = Color.White,
)
@Composable
@Preview
fun App() {
    var isInDarkMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            isInDarkMode = if (NativeParameterStoreAccess.IS_WINDOWS) {
                WindowsRegistry.getWindowsRegistryEntry("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize", "AppsUseLightTheme") == 0x0
            } else {
                false
            }

            delay(1.seconds)
        }
    }

    MaterialTheme(
        colors = if (isInDarkMode) {
            darkColorPalette
        } else {
            lightColorPalette
        }
    ) {
        // val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxSize()
        ) {
            Row() {
                Text(
                    text = "Current Question: None\nTime left until end/next: ?"
                )
            }

            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = "Current Leaderboard:\nFirst:\nSecond:\nThird:"
                )
            }

            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
            ) {
                Button(
                    onClick = {
                        startOrStopInterval()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        if (intervalRunning.value) {
                            "Stop"
                        } else {
                            "Start"
                        }
                    )
                }
            }
        }
    }
}