package app.stova.ha.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import app.stova.core.ui.focusOutline
import app.stova.core.theme.stovaMotion

/** One focus stop per row. Arrows are consumed only by this row's focus target. */
@Composable internal fun PanelSlider(id:String,label:String,value:Double,display:String,range:ClosedFloatingPointRange<Double>,
    enabled:Boolean,step:Double=1.0,accelerate:Boolean=true,modifier:Modifier=Modifier,onChange:(Double)->Unit) {
    val source=remember {MutableInteractionSource()}
    val fraction=animateFloatAsState(((value-range.start)/(range.endInclusive-range.start).coerceAtLeast(0.001)).toFloat().coerceIn(0f,1f),
        stovaMotion.fade(),label="panel-slider")
    val active=if(enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val track=MaterialTheme.colorScheme.outlineVariant
    Column(modifier.fillMaxWidth().testTag("slider-$id").focusOutline(source,MaterialTheme.shapes.medium)
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
        }.focusable(interactionSource=source).padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            Text(label,Modifier.weight(1f),style=MaterialTheme.typography.labelLarge)
            Text(display,style=MaterialTheme.typography.titleLarge)
        }
        Canvas(Modifier.fillMaxWidth().height(16.dp)) {
            val radius=6.dp.toPx()
            val start=Offset(radius,size.height/2)
            val end=Offset(size.width-radius,size.height/2)
            val thumb=Offset(start.x+(end.x-start.x)*fraction.value,start.y)
            drawLine(track,start,end,3.dp.toPx(),StrokeCap.Round)
            drawLine(active,start,thumb,3.dp.toPx(),StrokeCap.Round)
            drawCircle(active,radius,thumb)
        }
    }
}
