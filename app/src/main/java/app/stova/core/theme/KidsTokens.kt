package app.stova.core.theme

import androidx.compose.ui.graphics.Color

/**
 * The children's Home: Pip, the little robot, and the world he floats in. Bright, warm and clear
 * for a four-year-old, but never neon: every colour sits a step below full saturation, and text
 * always stands on a solid label, never straight on the sky.
 */
object KidsColors {
    // Pip's shell: warm white, with two shades for his roundness.
    val Shell = Color(0xFFF6F2EA)
    val ShellShade = Color(0xFFDDD5C7)
    val ShellDeep = Color(0xFFB8AE9D)
    /** His ears, hands and tummy panel. */
    val Mint = Color(0xFF5CC9B5)
    val MintDeep = Color(0xFF2E8F80)
    /** The screen that is his face. */
    val Visor = Color(0xFF17202E)
    val VisorRim = Color(0xFF2B3A52)
    val Eye = Color(0xFF86F0FF)
    val EyeCore = Color(0xFFE9FDFF)
    val Cheek = Color(0xFFFF8FA3)
    val Bulb = Color(0xFFFF7A59)
    val Heart = Color(0xFFFF6B7D)
    val Metal = Color(0xFF8C95A5)
    val Jet = Color(0xFFFFC86B)
    val Shadow = Color(0xFF0B1020)

    // Pip as a real little machine: brushed steel, painted panels worn at the edges, glass and rubber.
    val SteelLight = Color(0xFFF3F6F9)
    val Steel = Color(0xFFC6CED8)
    val SteelMid = Color(0xFF97A2B1)
    val SteelDark = Color(0xFF5F6979)
    val SteelDeep = Color(0xFF363D4A)
    val Paint = Color(0xFF4DB0A2)
    val PaintLight = Color(0xFF86D8CA)
    val PaintDark = Color(0xFF256F66)
    val Glass = Color(0xFF0A111C)
    val GlassEdge = Color(0xFF223047)
    val Iris = Color(0xFF5ED6FF)
    val IrisDeep = Color(0xFF176FA8)
    val IrisCore = Color(0xFFE8FCFF)
    val Pupil = Color(0xFF050C16)
    val Rubber = Color(0xFF292E37)
    val RubberLight = Color(0xFF48505D)

    // What Pip wears through the year.
    val SantaRed = Color(0xFFE0393E)
    val Fur = Color(0xFFF7F3EE)
    val WitchPurple = Color(0xFF5B3A8C)
    val WitchBand = Color(0xFFFF8A1F)
    val Beanie = Color(0xFF3D7DD8)
    val BeanieStripe = Color(0xFFEAF2FF)
    val Petal = Color(0xFFFFD84D)
    val PetalPink = Color(0xFFFF8FB8)
    val FlowerHeart = Color(0xFFFF9F2E)
    val Leaf = Color(0xFF6CBF4F)
    val Lens = Color(0xFF1F2A3A)
    val LensShine = Color(0xFF6FD6FF)
    val Frame = Color(0xFF23262D)
    val Scarf = Color(0xFFE8742C)
    val ScarfStripe = Color(0xFFFFD08A)
    val BunnyPink = Color(0xFFFFB3C7)
    val FlagRed = Color(0xFFBA0C2F)
    val FlagBlue = Color(0xFF00205B)
    val FlagWhite = Color(0xFFFFFFFF)
    val Nightcap = Color(0xFF3B4A8C)
    val Star = Color(0xFFFFE07A)

    // The sky by the hour: top, then horizon.
    val MorningTop = Color(0xFF8EC5FF)
    val MorningLow = Color(0xFFFFE1C4)
    val DayTop = Color(0xFF4FA3F7)
    val DayLow = Color(0xFFC4E8FF)
    val EveningTop = Color(0xFF3E428F)
    val EveningLow = Color(0xFFFF9E7A)
    val NightTop = Color(0xFF0B1230)
    val NightLow = Color(0xFF24335F)
    val Sun = Color(0xFFFFD45C)
    val SunGlow = Color(0xFFFFE9A8)
    val Moon = Color(0xFFF4F0DC)
    val MoonShade = Color(0xFFD9D3B8)
    val Cloud = Color(0xFFFFFFFF)
    /** Laid over the hills after dark. */
    val NightVeil = Color(0xFF141B45)
    /** Moonlight or sunlight along the top of each hill. */
    val Ridge = Color(0xFFFFFFFF)

    // Hills, back to front, by season.
    val SpringHills = listOf(Color(0xFF8AD47A), Color(0xFF62BA5C), Color(0xFF43984A))
    val SummerHills = listOf(Color(0xFF9BDC6E), Color(0xFF6CC457), Color(0xFF45A64A))
    val AutumnHills = listOf(Color(0xFFE3AE5C), Color(0xFFCF7E3E), Color(0xFFA35A33))
    val SnowHills = listOf(Color(0xFFF2F7FF), Color(0xFFDCE7F7), Color(0xFFC3D4EC))
    val DuskHills = listOf(Color(0xFF55437A), Color(0xFF3E3060), Color(0xFF2A2045))

    // Things in the world.
    val Pumpkin = Color(0xFFFF8A1F)
    val PumpkinDeep = Color(0xFFD9650E)
    val Stem = Color(0xFF4E7A2E)
    val Trunk = Color(0xFF7A5232)
    val Firefly = Color(0xFFFFF2A0)
    val Egg = listOf(Color(0xFFFF9FC0), Color(0xFF8FD3FF), Color(0xFFFFE07A), Color(0xFFB7F0A0))

    // The page itself.
    /** Behind every word: row names, the greeting and Pip's speech. */
    val Label = Color(0xEB14203A)
    val LabelText = Color(0xFFFFFFFF)
    val Bubble = Color(0xFFFFFFFF)
    val BubbleText = Color(0xFF17202E)
    /** The frame around the card in focus. */
    val Focus = Color(0xFFFFFFFF)
    val CardBack = Color(0xFF22304D)
    val NewBadge = Color(0xFFFFC93C)
    val NewBadgeText = Color(0xFF3A2600)
    val Progress = Color(0xFF5CC9B5)
}
