package app.stova.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.stova.R
import app.stova.core.ui.PinDialog
import kotlinx.coroutines.launch

/** Everything before a profile is active: first-run setup, the picker and the PIN prompt. */
@Composable
fun ProfileGate(session: ProfileSession, repo: ProfileRepository) {
    val start by session.start.collectAsStateWithLifecycle()
    val file by repo.file.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    var pinFor by remember { mutableStateOf<Profile?>(null) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val decision = start) {
            null -> Unit
            StartDecision.CreateFirst -> FirstRun { name, avatar ->
                scope.launch { session.enter(repo.create(name, avatar, kid = false).id) }
            }
            is StartDecision.Enter -> LaunchedEffect(decision) { session.enter(decision.profile.id) }
            is StartDecision.NeedsPin, StartDecision.Pick -> {
                LaunchedEffect(decision) { if (decision is StartDecision.NeedsPin) pinFor = decision.profile }
                ProfilePicker(
                    profiles = file?.profiles.orEmpty(),
                    onPick = { profile -> if (profile.pin != null) pinFor = profile else scope.launch { session.enter(profile.id) } },
                    onAdd = null,
                )
            }
        }
    }

    pinFor?.let { profile ->
        PinDialog(
            title = stringResource(R.string.pin_for_profile, profile.name),
            onSubmit = { pin -> repo.verifyPin(profile.id, pin) },
            onAccepted = {
                pinFor = null
                scope.launch { session.enter(profile.id) }
            },
            onDismiss = { pinFor = null },
        )
    }
}

@Composable
private fun FirstRun(onSave: (String, app.stova.core.theme.AvatarColor) -> Unit) {
    Column(Modifier.widthIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
        Text(stringResource(R.string.welcome_body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ProfileEditor(initial = null, allowKid = false, onSave = { name, avatar, _ -> onSave(name, avatar) }, onCancel = null)
    }
}
