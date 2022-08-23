import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.tkuenneth.nativeparameterstoreaccess.NativeParameterStoreAccess
import com.github.tkuenneth.nativeparameterstoreaccess.WindowsRegistry
import handler.IntervalHandler
import handler.QuestionHandler
import handler.UserHandler
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import javax.swing.JOptionPane
import kotlin.concurrent.timer
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

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

    var leaderBoard by remember { mutableStateOf("No leaderboard available yet") }
    var currentQuestion by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        val timer = timer(
            period = 0.5.seconds.inWholeMilliseconds,
            daemon = true
        ) {
            if(UserHandler.getTop3Users().isNotEmpty()){
                UserHandler.getTop3Users().run {
                    leaderBoard = "First: ${this[0]?.name}\nSecond: ${this[1]?.name ?: "No one"}\nThird: ${this[2]?.name ?: "No one"}"
                }
            } else {
                leaderBoard = "No leaderboard available yet"
            }

            val questionText = QuestionHandler.instance?.currentQuestion?.value?.questionText
            val timeLeftDisplay = IntervalHandler.instance?.timestampNextAction?.value?.minus(Clock.System.now())
            currentQuestion = "Current Question: $questionText\n" +
                    "Answer: ${QuestionHandler.instance?.currentQuestion?.value?.answer}\n" +
                    "Time left until " +
                    if(questionText != QuestionHandler.instance?.emptyQuestion?.questionText){
                        "answering ends: "
                    } else {
                        "next question: "
                    } +
                    if(timeLeftDisplay == null || timeLeftDisplay.inWholeMilliseconds < 0) {
                        "No Timer Running"
                    } else {
                        timeLeftDisplay.toString(DurationUnit.SECONDS, 0)
                    }
        }

        onDispose {
            timer.cancel()
        }
    }

    MaterialTheme(
        colors = if (isInDarkMode) {
            darkColorPalette
        } else {
            lightColorPalette
        }
    ) {
        Scaffold {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize()
            ) {
                Row() {
                    Text(
                        text = currentQuestion
                    )
                }

                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                ) {
                    Text(
                        text = leaderBoard
                    )
                }

                Row(
                    modifier = Modifier
                        .padding(top = 8.dp)
                ) {
                    Button(
                        onClick = {
                            val intervalHandlerInstance = IntervalHandler.instance ?: run {
                                JOptionPane.showMessageDialog(null, "Error with starting the interval. Check the log for more infos!", "InfoBox: File Debugger", JOptionPane.INFORMATION_MESSAGE)
                                logger.error("Error with starting the interval. Check the log for more infos!")
                                exitProcess(0)
                            }
                            if(intervalHandlerInstance.intervalRunning.value) {
                                intervalHandlerInstance.stopInterval()
                            } else {
                                intervalHandlerInstance.startInterval()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(
                            if (IntervalHandler.instance?.intervalRunning?.value == true) {
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
}