package app.stova.ha.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import app.stova.core.ui.focusOutline

/** One focus stop per row. Arrows are consumed only by this row's focus target. */
@Composable internal fun PanelSlider(id:String,label:String,value:Double,display:String,range:ClosedFloatingPointRange<Double>,
    enabled:Boolean,step:Double=1.0,accelerate:Boolean=true,onChange:(Double)->Unit) {
    val source=remember {MutableInteractionSource()}
    Column(Modifier.fillMaxWidth().testTag("slider-$id").focusOutline(source,MaterialTheme.shapes.medium)
        .background(MaterialTheme.colorScheme.surfaceVariant,MaterialTheme.shapes.medium)
        .semantics {
            contentDescription=label
            progressBarRangeInfo=ProgressBarRangeInfo(value.toFloat(),range.start.toFloat()..range.endInclusive.toFloat())
            if(!enabled) disabled()
            setProgress {if(enabled) {onChange(it.toDouble().coerceIn(range));true} else false}
        }.onPreviewKeyEvent {event ->
            if(event.key in listOf(Key.DirectionLeft,Key.DirectionRight)) {
                if(enabled && event.type==KeyEventType.KeyDown) {
                    val delta=step*(if(accelerate) percentageStep(event.nativeKeyEvent.repeatCount) else 1)
                    onChange((value+if(event.key==Key.DirectionRight) delta else -delta).coerceIn(range))
                }
                true
            } else false
        }.focusable(interactionSource=source).padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text(label,style=MaterialTheme.typography.labelLarge)
            Text(display,style=MaterialTheme.typography.titleMedium)
        }
        LinearProgressIndicator(progress={((value-range.start)/(range.endInclusive-range.start).coerceAtLeast(0.001)).toFloat().coerceIn(0f,1f)},
            modifier=Modifier.fillMaxWidth().height(4.dp),color=MaterialTheme.colorScheme.primary,trackColor=MaterialTheme.colorScheme.outlineVariant,drawStopIndicator={})
    }
}
