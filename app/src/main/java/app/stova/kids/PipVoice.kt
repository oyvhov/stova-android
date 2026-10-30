package app.stova.kids

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Pip's voice: the device's own text-to-speech, a little higher than usual. Norwegian when the
 * app is, so a Nynorsk greeting is not read in English. Says nothing until the engine is ready,
 * and a new name always cuts off the one before it.
 */
class PipVoice(context: Context) {
    private var ready = false
    private val norwegian = context.resources.configuration.locales[0].language != "en"
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) ready = true
        configure()
    }

    private fun configure() {
        if (!ready) return
        val wanted = if (norwegian) listOf(Locale.forLanguageTag("nb-NO"), Locale.forLanguageTag("no")) else listOf(Locale.UK, Locale.US)
        wanted.firstOrNull { tts.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE }?.let { tts.language = it }
        tts.setPitch(1.3f)
        tts.setSpeechRate(0.95f)
    }

    fun say(text: String) {
        if (ready) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "pip")
    }

    fun close() {
        tts.stop()
        tts.shutdown()
    }
}

/** Pip's voice while this screen lives, or null when the parents have turned it off. */
@Composable
fun rememberPipVoice(enabled: Boolean): ((String) -> Unit)? {
    if (!enabled) return null
    val context = LocalContext.current
    val voice = remember(context) { PipVoice(context) }
    DisposableEffect(voice) { onDispose { voice.close() } }
    return remember(voice) { { text: String -> voice.say(text) } }
}
