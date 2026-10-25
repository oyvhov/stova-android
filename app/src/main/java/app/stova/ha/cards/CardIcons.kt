package app.stova.ha.cards

import app.stova.core.ui.StovaIcons
import app.stova.ha.HaEntity
import app.stova.ha.text

fun cardIcon(type:CardType,entity:HaEntity?=null,override:String?=null) = when(override) {
    "light" -> StovaIcons.Light; "sun" -> StovaIcons.Sun; "home" -> StovaIcons.Home
    "climate" -> StovaIcons.Climate; "sensor" -> StovaIcons.Sensor; "lock" -> StovaIcons.Lock
    else -> when(type) {
        CardType.LIGHT -> StovaIcons.Light; CardType.TOGGLE -> StovaIcons.Toggle
        CardType.CLIMATE -> StovaIcons.Climate; CardType.ENERGY_PRICE -> StovaIcons.Lightning
        CardType.SENSOR -> when(entity?.attributes?.text("device_class")) {
            "temperature" -> StovaIcons.Climate; "power","energy" -> StovaIcons.Lightning
            "door","window","garage_door","opening" -> StovaIcons.Home
            "lock" -> StovaIcons.Lock; else -> StovaIcons.Sensor
        }
        CardType.WEATHER -> weatherIcon(entity?.state.orEmpty()); CardType.MEDIA_PLAYER -> StovaIcons.Play
        CardType.SCENE -> StovaIcons.Sun; CardType.CAMERA -> StovaIcons.Camera
        CardType.COVER -> StovaIcons.Home; CardType.LOCK -> StovaIcons.Lock
    }
}
