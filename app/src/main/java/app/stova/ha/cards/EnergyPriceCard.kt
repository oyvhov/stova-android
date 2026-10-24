package app.stova.ha.cards
import androidx.compose.runtime.Composable
import app.stova.ha.HaEntity
import java.time.LocalTime
@Composable fun EnergyPriceCard(entity:HaEntity) { StateValue(entity); Sparkline(priceCurve(entity),current=LocalTime.now().hour) }
