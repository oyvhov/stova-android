package app.stova.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.ui.*

@Composable internal fun BackgroundAgeSetting(age:Int?,onChange:(Int?)->Unit) {
    var open by remember {mutableStateOf(false)}
    var restore by remember {mutableStateOf(false)}
    val focus=remember {FocusRequester()}
    SettingsNavRow(stringResource(R.string.background_max_age),Modifier.focusRequester(focus),ageLabel(age)) {restore=true;open=true}
    LaunchedEffect(open,restore) {if(!open && restore) {withFrameNanos {};focus.requestFocus();restore=false}}
    if(open) BackgroundAgeDialog(age,{onChange(it);open=false},{open=false})
}

@Composable internal fun ageLabel(age:Int?):String = age?.let {stringResource(R.string.background_age_years,it)}
    ?: stringResource(R.string.background_age_unlimited)

@Composable internal fun BackgroundAgeDialog(age:Int?,onSelect:(Int?)->Unit,onDismiss:()->Unit) {
    val focus=remember {FocusRequester()}
    val density=LocalDensity.current
    StovaDialog(onDismiss) {CompositionLocalProvider(LocalDensity provides density) {
        Column(Modifier.widthIn(max=500.dp).fillMaxWidth().heightIn(max=440.dp)
            .background(MaterialTheme.colorScheme.surface,MaterialTheme.shapes.large).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            PageHeader(stringResource(R.string.background_max_age),StovaIcons.Show)
            Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                listOf(0,6,9,12,15,16,18,null).forEach {value->
                    StovaButton(ageLabel(value),{onSelect(value)},Modifier.fillMaxWidth()
                        .semantics {selected=value==age}.then(if(value==age) Modifier.focusRequester(focus) else Modifier),
                        icon=if(value==age) StovaIcons.Check else null,compact=true,quiet=value!=age)
                }
            }
            StovaButton(stringResource(R.string.profile_cancel),onDismiss,compact=true,quiet=true)
            LaunchedEffect(Unit) {focus.requestFocus()}
        }
    }}
}
