package app.stova.kids

import android.content.Context
import android.view.Surface
import android.view.TextureView
import com.google.android.filament.Camera
import com.google.android.filament.ColorGrading
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.Filament
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.Texture
import com.google.android.filament.ToneMapper
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** The camera looks straight at Pip from the front, so his feet land on a known line of the view. */
private const val CAMERA_Y = 0.68f
private const val CAMERA_DISTANCE = 4.2f
private const val FOV = 20.0
private val HALF_HEIGHT = CAMERA_DISTANCE * kotlin.math.tan(Math.toRadians(FOV / 2)).toFloat()
/** Width over height of Pip's 3D view: room for his arms up and a lean either way. */
const val PIP3D_ASPECT = 0.8f
/** Where his feet stand, as a share of the view's height from the top. */
val PIP3D_FEET = (CAMERA_Y + HALF_HEIGHT) / (2 * HALF_HEIGHT)
/** Pip's 2D pose units (his drawing is 200 wide) in metres of the 3D model. */
private const val UNIT = 0.005f

/**
 * Pip in real 3D, drawn by Filament into a see-through [TextureView]: a glTF model built by
 * tools/pip/make_pip.py, lit by a studio light from above left, a cool light from behind for his
 * outline and a soft sky. Every frame [render] poses his joints from [PipMotion] and draws once;
 * nothing is drawn between frames that change nothing.
 */
internal class PipScene(context: Context, textureView: TextureView) {
    companion object {
        private var ready: Boolean? = null
        /**
         * False where 3D cannot run, and Pip is drawn flat: no OpenGL ES 3 (the emulator's software
         * GPU, whose ES 2 makes the glTF materials abort the process), or no native renderer (unit tests).
         */
        fun available(context: Context): Boolean = ready ?: runCatching {
            val gles = context.getSystemService(android.app.ActivityManager::class.java).deviceConfigurationInfo.reqGlEsVersion
            check(gles >= 0x30000) { "OpenGL ES 3 needed" }
            Filament.init(); Gltfio.init(); com.google.android.filament.utils.Utils.init(); true
        }.getOrDefault(false).also { ready = it }
    }

    private val engine = Engine.create()
    private val renderer = engine.createRenderer()
    private val scene = engine.createScene()
    private val view = engine.createView()
    private val camera: Camera = engine.createCamera(EntityManager.get().create())
    private var swapChain: SwapChain? = null
    private val ui = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
    private val assetLoader = AssetLoader(engine, UbershaderProvider(engine), EntityManager.get())
    private val resourceLoader = ResourceLoader(engine)
    private val asset: FilamentAsset
    private val lights = IntArray(2)
    private var light: IndirectLight? = null
    private var reflections: Texture? = null
    private var lightFor: DayPart? = null
    private val nodes = HashMap<String, Int>()
    private val materials = HashMap<String, MaterialInstance>()
    private var outfitShown: PipOutfit? = null
    private val matrix = FloatArray(16)

    init {
        // A device that gives Filament only ES 2 cannot load the glTF materials; refuse before that aborts.
        check(engine.activeFeatureLevel != Engine.FeatureLevel.FEATURE_LEVEL_0) { engine.destroy(); "Filament feature level 0" }
        view.scene = scene
        view.camera = camera
        view.blendMode = View.BlendMode.TRANSLUCENT
        view.antiAliasing = View.AntiAliasing.FXAA
        view.multiSampleAntiAliasingOptions = view.multiSampleAntiAliasingOptions.apply { enabled = true; sampleCount = 4 }
        view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply { enabled = true; radius = 0.12f; intensity = 1.1f }
        // No bloom: in a see-through view the PowerVR GPU of the Google TV Streamer turns him white and
        // half transparent with it. Without it he looks the same everywhere, and his eyes still glow.
        view.bloomOptions = view.bloomOptions.apply { enabled = false }
        view.colorGrading = ColorGrading.Builder().toneMapper(ToneMapper.PBRNeutralToneMapper()).build(engine)
        renderer.clearOptions = renderer.clearOptions.apply { clear = true; clearColor = doubleArrayOf(0.0, 0.0, 0.0, 0.0) }
        camera.setExposure(16f, 1f / 125f, 100f)
        camera.lookAt(0.0, CAMERA_Y.toDouble(), CAMERA_DISTANCE.toDouble(), 0.0, CAMERA_Y.toDouble(), 0.0, 0.0, 1.0, 0.0)

        val bytes = context.assets.open("pip/pip.glb").use { it.readBytes() }
        val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).put(bytes).also { it.flip() }
        asset = checkNotNull(assetLoader.createAsset(buffer)) { "pip.glb did not load" }
        resourceLoader.loadResources(asset)
        asset.releaseSourceData()
        val rm = engine.renderableManager
        for (e in asset.entities) if (rm.hasComponent(e)) rm.getInstance(e).let { rm.setCastShadows(it, true); rm.setReceiveShadows(it, true) }
        scene.addEntities(asset.entities)
        for (name in NODE_NAMES) asset.getFirstEntityByName(name).takeIf { it != 0 }?.let { nodes[name] = it }
        for (mi in asset.instance.materialInstances) materials[mi.name] = mi

        ui.setOpaque(false)
        ui.renderCallback = object : UiHelper.RendererCallback {
            override fun onNativeWindowChanged(surface: Surface) {
                swapChain?.let { engine.destroySwapChain(it) }
                swapChain = engine.createSwapChain(surface, ui.swapChainFlags)
            }
            override fun onDetachedFromSurface() {
                swapChain?.let { engine.destroySwapChain(it); engine.flushAndWait() }
                swapChain = null
            }
            override fun onResized(width: Int, height: Int) {
                view.viewport = Viewport(0, 0, width, height)
                camera.setProjection(FOV, width.toDouble() / height.coerceAtLeast(1), 0.1, 20.0, Camera.Fov.VERTICAL)
            }
        }
        ui.attachTo(textureView)
    }

    fun destroy() {
        ui.detach()
        assetLoader.destroyAsset(asset)
        resourceLoader.destroy(); assetLoader.destroy()
        light?.let { engine.destroyIndirectLight(it) }; reflections?.let { engine.destroyTexture(it) }
        for (l in lights) if (l != 0) { engine.destroyEntity(l); EntityManager.get().destroy(l) }
        engine.destroyRenderer(renderer); engine.destroyView(view); engine.destroyScene(scene)
        engine.destroyCameraComponent(camera.entity); EntityManager.get().destroy(camera.entity)
        swapChain?.let { engine.destroySwapChain(it) }
        engine.destroy()
    }

    /** Poses him from [m] and draws one frame; does nothing while there is no surface to draw on. */
    fun render(m: PipMotion, mood: PipMood, outfit: PipOutfit, part: DayPart, frameNanos: Long) {
        val chain = swapChain ?: return
        if (lightFor != part) light(part)
        if (outfitShown != outfit) dress(outfit)
        pose(m, if (m.cheer > 0.05f && mood != PipMood.ASLEEP) PipMood.EXCITED else mood)
        if (renderer.beginFrame(chain, frameNanos)) { renderer.render(view); renderer.endFrame() }
    }

    // Light: a key light, a rim light and a soft studio, warmer by morning, cooler at night, never dark,
    // so his steel always has something bright to reflect.

    private fun light(part: DayPart) {
        lightFor = part
        val sky = when (part) {
            DayPart.MORNING -> Sky(floatArrayOf(1.0f, 0.88f, 0.76f), floatArrayOf(0.80f, 0.86f, 1.0f), floatArrayOf(0.55f, 0.50f, 0.45f), 1.25f)
            DayPart.DAY -> Sky(floatArrayOf(1.0f, 0.97f, 0.92f), floatArrayOf(0.78f, 0.86f, 1.0f), floatArrayOf(0.55f, 0.55f, 0.52f), 1.35f)
            DayPart.EVENING -> Sky(floatArrayOf(1.0f, 0.78f, 0.60f), floatArrayOf(0.74f, 0.68f, 0.95f), floatArrayOf(0.45f, 0.40f, 0.44f), 1.2f)
            DayPart.NIGHT -> Sky(floatArrayOf(0.86f, 0.90f, 1.0f), floatArrayOf(0.55f, 0.62f, 0.92f), floatArrayOf(0.32f, 0.33f, 0.42f), 1.1f)
        }
        for (l in lights) if (l != 0) { scene.removeEntity(l); engine.destroyEntity(l); EntityManager.get().destroy(l) }
        lights[0] = EntityManager.get().create().also {
            LightManager.Builder(LightManager.Type.SUN).color(sky.key[0], sky.key[1], sky.key[2]).intensity(95_000f * sky.strength)
                .direction(0.45f, -0.55f, -0.70f).sunAngularRadius(2.2f).castShadows(true).build(engine, it)
            scene.addEntity(it)
        }
        lights[1] = EntityManager.get().create().also {
            LightManager.Builder(LightManager.Type.DIRECTIONAL).color(sky.top[0], sky.top[1], sky.top[2]).intensity(55_000f * sky.strength)
                .direction(-0.55f, -0.25f, 0.80f).build(engine, it)
            scene.addEntity(it)
        }
        light?.let { engine.destroyIndirectLight(it) }; reflections?.let { engine.destroyTexture(it) }
        val cube = studioCubemap(sky)
        reflections = cube
        light = IndirectLight.Builder().reflections(cube).irradiance(3, irradiance(sky)).intensity(26_000f * sky.strength).build(engine)
        scene.indirectLight = light
    }

    private class Sky(val key: FloatArray, val top: FloatArray, val ground: FloatArray, val strength: Float)

    /** The light from any direction: a sky above, a dim floor and two soft boxes for his steel to shine in. */
    private fun environment(sky: Sky, x: Float, y: Float, z: Float, out: FloatArray) {
        val up = (y * 0.5f + 0.5f).coerceIn(0f, 1f)
        val t = up * up * (3 - 2 * up)
        for (c in 0..2) out[c] = sky.ground[c] + (sky.top[c] - sky.ground[c]) * t
        fun box(dx: Float, dy: Float, dz: Float, size: Float, power: Float, colour: FloatArray) {
            val len = sqrt(dx * dx + dy * dy + dz * dz)
            val d = (x * dx + y * dy + z * dz) / len
            val edge = cos(Math.toRadians(size.toDouble())).toFloat()
            if (d > edge) { val w = ((d - edge) / (1 - edge)).coerceIn(0f, 1f).let { sqrt(it) }; for (c in 0..2) out[c] += colour[c] * power * w }
        }
        box(-0.55f, 0.65f, 0.55f, 22f, 7f, sky.key)
        box(0.75f, 0.30f, -0.60f, 16f, 4f, sky.top)
        box(0.0f, 1.0f, 0.15f, 30f, 2.5f, sky.key)
    }

    private fun studioCubemap(sky: Sky): Texture {
        val size = 32; val levels = 6
        val tex = Texture.Builder().width(size).height(size).levels(levels).sampler(Texture.Sampler.SAMPLER_CUBEMAP)
            .format(Texture.InternalFormat.RGBA16F)
            .usage(Texture.Usage.DEFAULT or Texture.Usage.GEN_MIPMAPPABLE or Texture.Usage.COLOR_ATTACHMENT or Texture.Usage.BLIT_SRC or Texture.Usage.BLIT_DST).build(engine)
        val c = FloatArray(3); val sum = FloatArray(3)
        // Only the sharpest level is filled here; the GPU blurs the rest from it.
        for (level in 0 until 1) {
            val s = size shr level
            // Rougher steel sees a blurrier world: each level averages the light over a wider cone.
            val spread = if (level == 0) 0f else 0.18f * (1 shl (level - 1))
            for (f in 0 until 6) {
            val buf = ByteBuffer.allocateDirect(s * s * 4 * 4).order(ByteOrder.nativeOrder())
            for (py in 0 until s) for (px in 0 until s) {
                val u = 2f * (px + 0.5f) / s - 1f; val v = 2f * (py + 0.5f) / s - 1f
                val (x, y, z) = when (f) { 0 -> Triple(1f, -v, -u); 1 -> Triple(-1f, -v, u); 2 -> Triple(u, 1f, v); 3 -> Triple(u, -1f, -v); 4 -> Triple(u, -v, 1f); else -> Triple(-u, -v, -1f) }
                sum.fill(0f)
                val taps = if (spread == 0f) 1 else 9
                for (t in 0 until taps) {
                    val ox = if (taps == 1) 0f else ((t % 3) - 1) * spread; val oy = if (taps == 1) 0f else ((t / 3) - 1) * spread
                    val dx = x + ox; val dy = y + oy; val dz = z + (ox - oy) * 0.5f
                    val n = sqrt(dx * dx + dy * dy + dz * dz)
                    environment(sky, dx / n, dy / n, dz / n, c)
                    for (ch in 0..2) sum[ch] += c[ch] / taps
                }
                for (ch in 0..2) buf.putFloat(sum[ch])
                buf.putFloat(1f)
            }
            buf.flip()
            // One face at a time: the face is the layer of a cubemap.
            tex.setImage(engine, level, 0, 0, f, s, s, 1, Texture.PixelBufferDescriptor(buf, Texture.Format.RGBA, Texture.Type.FLOAT))
            }
        }
        tex.generateMipmaps(engine)
        return tex
    }

    /** The soft light on his surfaces from the whole sky, as the nine numbers Filament's shader expects. */
    private fun irradiance(sky: Sky): FloatArray {
        // Irradiance / π on a set of directions, then fitted to Filament's nine SH polynomials.
        val dirs = fibonacci(160); val env = fibonacci(900); val c = FloatArray(3)
        val envLight = env.map { d -> environment(sky, d[0], d[1], d[2], c); c.copyOf() }
        val rows = dirs.map { n -> basis(n) }
        val values = dirs.map { n ->
            val e = FloatArray(3)
            for ((i, d) in env.withIndex()) { val cosine = n[0] * d[0] + n[1] * d[1] + n[2] * d[2]; if (cosine > 0) for (ch in 0..2) e[ch] += envLight[i][ch] * cosine }
            FloatArray(3) { ch -> e[ch] * (4f * Math.PI.toFloat() / env.size) / Math.PI.toFloat() }
        }
        val sh = FloatArray(27)
        for (ch in 0..2) leastSquares(rows, values.map { it[ch] }).forEachIndexed { i, v -> sh[i * 3 + ch] = v }
        return sh
    }

    private fun basis(n: FloatArray): FloatArray { val (x, y, z) = Triple(n[0], n[1], n[2])
        return floatArrayOf(1f, y, z, x, y * x, y * z, 3 * z * z - 1, z * x, x * x - y * y) }

    private fun fibonacci(count: Int): List<FloatArray> = List(count) { i ->
        val y = 1f - 2f * (i + 0.5f) / count; val r = sqrt(max(0f, 1 - y * y)); val a = i * 2.39996323f
        floatArrayOf(r * cos(a), y, r * sin(a))
    }

    private fun leastSquares(rows: List<FloatArray>, values: List<Float>): FloatArray {
        val k = 9; val a = Array(k) { DoubleArray(k + 1) }
        for ((r, row) in rows.withIndex()) for (i in 0 until k) { for (j in 0 until k) a[i][j] += (row[i] * row[j]).toDouble(); a[i][k] += (row[i] * values[r]).toDouble() }
        for (i in 0 until k) { // Gauss-Jordan
            val p = (i until k).maxBy { abs(a[it][i]) }; val t = a[i]; a[i] = a[p]; a[p] = t
            val d = a[i][i].takeIf { abs(it) > 1e-12 } ?: continue
            for (j in i..k) a[i][j] /= d
            for (r in 0 until k) if (r != i) { val f = a[r][i]; for (j in i..k) a[r][j] -= f * a[i][j] }
        }
        return FloatArray(k) { a[it][k].toFloat() }
    }

    // What he wears: every outfit is in the model; all but one are hidden.

    private fun dress(outfit: PipOutfit) {
        outfitShown = outfit
        val rm = engine.renderableManager
        val shown = when (outfit) {
            PipOutfit.SANTA -> "santa"; PipOutfit.WITCH -> "witch"; PipOutfit.BEANIE -> "beanie"; PipOutfit.NIGHTCAP -> "nightcap"
            PipOutfit.BUNNY -> "bunny"; PipOutfit.FLAG -> "flag"; PipOutfit.FLOWER -> "flower"; PipOutfit.SUNGLASSES -> "sunglasses"
            PipOutfit.SCARF -> "scarf"; PipOutfit.NONE -> ""
        }
        val hat = outfit in setOf(PipOutfit.SANTA, PipOutfit.WITCH, PipOutfit.BEANIE, PipOutfit.NIGHTCAP)
        for (name in OUTFITS) nodes[name]?.let { e -> if (rm.hasComponent(e)) rm.setLayerMask(rm.getInstance(e), 0xff, if (name == shown) 0x01 else 0x00) }
        // A hat covers his antenna; a flower or a flag sits on top of it instead of the light.
        for (name in listOf("antenna", "bulb")) nodes[name]?.let { e -> if (rm.hasComponent(e)) rm.setLayerMask(rm.getInstance(e), 0xff,
            if (hat || (name == "bulb" && outfit in setOf(PipOutfit.FLOWER, PipOutfit.FLAG))) 0x00 else 0x01) }
        if (outfit == PipOutfit.FLAG) nodes["rosette"]?.let { e -> if (rm.hasComponent(e)) rm.setLayerMask(rm.getInstance(e), 0xff, 0x01) }
    }

    // The pose: the same body as the flat Pip, in three dimensions.

    private fun pose(m: PipMotion, mood: PipMood) {
        val walking = min(m.stride, 1.2f)
        val sitDrop = 30f * m.sit
        val bob = -(-abs(sin(m.gait * Math.PI).toFloat()) * 4f * walking + sitDrop + m.breath * 1.2f - m.air * 4f) * UNIT
        val turn = (m.speedX / 900f).coerceIn(-1f, 1f)
        // He leans into a waddle about his feet, turns his body a little the way he walks and his head further.
        val root = rotZ(-m.lean)
        set("pip", root)
        val pelvis = mul(translate(0f, 0.30f + bob, 0f), mul(rotY(turn * 24f), scaleAbout(2f - m.squash, m.squash, 2f - m.squash)))
        set("pelvis", pelvis)
        val headYaw = (turn * 38f + m.lookX * 16f).coerceIn(-45f, 45f)
        set("head", mul(translate(0f, 0.42f - m.headDy * UNIT, 0f), mul(rotY(headYaw - turn * 24f), mul(rotZ(-m.headTilt), rotX(m.lookY * 7f)))))
        eyes(m, mood)
        mouth(mood, m.talk)
        set("antenna", mul(translate(0f, 0.495f, 0f), rotZ(-m.antenna)))
        glow("antenna_light", 1.0f, 0.42f, 0.22f, 7f * m.glow)
        glow("heart", 1.0f, 0.28f, 0.38f, 6f * m.glow)
        glow("cheek", 1.0f, 0.45f, 0.55f, if (mood in setOf(PipMood.HAPPY, PipMood.WAVE, PipMood.EXCITED)) 1.4f else 0.5f)
        // Arms and legs hang under the root, so they are placed in its space: the pelvis is enough.
        arms(m, pelvis, walking)
        legs(m, pelvis, walking)
    }

    /** Lit eyes on his glass face: they squash to blink, and their height and place show his mood. */
    private fun eyes(m: PipMotion, mood: PipMood) {
        for ((i, side) in listOf("L", "R").withIndex()) {
            val (width, height, lift) = when (mood) {
                PipMood.HAPPY, PipMood.WAVE -> Triple(1.04f, 0.80f, 0.010f)
                PipMood.CURIOUS -> if (i == 0) Triple(0.92f, 0.88f, 0.004f) else Triple(1.12f, 1.12f, 0f)
                PipMood.EXCITED -> Triple(1.15f, 1.15f, 0.006f)
                PipMood.SLEEPY -> Triple(1.04f, 0.40f, -0.022f)
                PipMood.ASLEEP -> Triple(1.06f, 0.09f, -0.030f)
            }
            val x = m.lookX * 0.018f; val y = -m.lookY * 0.014f + lift
            set("eyeShape$side", mul(translate(x, y, 0f), scale(width, height * (1f - 0.9f * m.blink) + 0.02f, 1f)))
            // The glass lids in the model are not used: the eyes' own shape says it, without seams.
            set("lidTop$side", scale(1e-4f, 1e-4f, 1e-4f)); set("lidBot$side", scale(1e-4f, 1e-4f, 1e-4f))
        }
        glow("iris", 0.30f, 0.85f, 1.0f, if (mood == PipMood.ASLEEP) 1.6f else 3.6f)
    }

    private fun mouth(mood: PipMood, talk: Float) {
        val dim = if (mood == PipMood.ASLEEP || mood == PipMood.SLEEPY) 0.4f else 1f
        glow("mouth", 0.30f, 0.85f, 1.0f, 5f * dim)
        for (i in 0 until 7) {
            val x = (i - 3) * 0.034f
            val curve = (9 - (i - 3) * (i - 3)).toFloat()
            val (at, s) = when {
                talk > 0.05f -> floatArrayOf(x, 0.145f) to floatArrayOf(1f, 1f + 2.4f * talk * abs(sin(i * 1.7f + talk * 5f)))
                mood == PipMood.CURIOUS -> (if (i == 6) floatArrayOf(0f, 0.145f) else floatArrayOf(0.022f * cos(i * Math.PI / 3).toFloat(),
                    0.145f + 0.022f * sin(i * Math.PI / 3).toFloat())) to (if (i == 6) floatArrayOf(0f, 0f) else floatArrayOf(0.8f, 0.8f))
                mood == PipMood.EXCITED -> floatArrayOf(x * 1.08f, 0.15f - curve * 0.0024f) to floatArrayOf(1.25f, 1.25f)
                mood == PipMood.SLEEPY || mood == PipMood.ASLEEP -> floatArrayOf(x * 0.8f, 0.14f) to (if (i == 0 || i == 6) floatArrayOf(0f, 0f) else floatArrayOf(0.8f, 0.8f))
                else -> floatArrayOf(x, 0.15f - curve * 0.0017f) to floatArrayOf(1f, 1f)
            }
            set("mouth$i", mul(translate(at[0], at[1], 0.296f), scale(s[0], s[1], s[0])))
        }
    }

    private fun arms(m: PipMotion, body: FloatArray, walking: Float) {
        val swing = sin(m.gait * Math.PI).toFloat() * walking
        for ((sideIndex, side) in listOf("L", "R").withIndex()) {
            val sx = if (sideIndex == 0) -1f else 1f
            val shoulder = apply(body, sx * 0.255f, 0.27f, 0f)
            var elbow = add(shoulder, sx * 0.05f, -0.13f, -sx * swing * 0.04f)
            var hand = add(elbow, sx * 0.02f, -0.12f, 0.03f - sx * swing * 0.06f)
            val up = m.cheer
            elbow = lerp(elbow, add(shoulder, sx * 0.08f, 0.10f, 0.03f), up)
            hand = lerp(hand, add(shoulder, sx * 0.12f, 0.23f, 0.03f), up)
            if (sx > 0 && m.wave > 0f && up < 0.5f) {
                elbow = add(shoulder, 0.10f, 0.07f, 0.05f)
                hand = add(elbow, 0.02f + 0.07f * (m.wave - 0.5f), 0.13f, 0.02f)
            }
            if (m.sit > 0.01f) { val rest = add(shoulder, sx * 0.06f, -0.16f, 0.12f); hand = lerp(hand, rest, m.sit) }
            segment("upperArm$side", shoulder, elbow); segment("foreArm$side", elbow, hand)
            at("shoulder$side", shoulder); at("elbow$side", elbow); at("hand$side", hand)
            val d = norm(sub(hand, elbow))
            for (k in 0..2) { val a = (k - 1) * 0.6f
                val dir = floatArrayOf(d[0] * cos(a) - d[1] * sin(a), d[0] * sin(a) + d[1] * cos(a), d[2])
                set("finger$k$side", mul(translate(hand[0], hand[1], hand[2]), alignY(dir))) }
        }
    }

    private fun legs(m: PipMotion, body: FloatArray, walking: Float) {
        val step = sin(m.gait * Math.PI).toFloat()
        for ((sideIndex, side) in listOf("L", "R").withIndex()) {
            val sx = if (sideIndex == 0) -1f else 1f
            val hip = apply(body, sx * 0.11f, 0.02f, 0f)
            var lift = max(0f, if (sx < 0) step else -step) * 0.07f * walking + 0.09f * m.air
            if (sx > 0) lift += 0.05f * m.tap
            val stride = if (sx < 0) cos(m.gait * Math.PI).toFloat() else -cos(m.gait * Math.PI).toFloat()
            val foot = floatArrayOf(sx * (0.13f + 0.05f * m.sit - 0.02f * m.air), lift, 0.05f * walking * stride + 0.16f * m.sit)
            val ankle = add(foot, 0f, 0.05f, -0.01f)
            val knee = knee(hip, ankle, sx)
            segment("thigh$side", hip, knee); segment("shin$side", knee, ankle)
            at("hip$side", hip); at("knee$side", knee); at("ankle$side", ankle)
            // Toes down when the foot lifts; soles to the front when he sits.
            set("foot$side", mul(translate(foot[0], foot[1], foot[2]), rotX(-12f * (lift / 0.07f).coerceIn(0f, 1f) - 75f * m.sit)))
        }
    }

    /** Two bones from hip to ankle: the knee bends forward and a little outward. */
    private fun knee(hip: FloatArray, ankle: FloatArray, side: Float): FloatArray {
        val l1 = 0.145f; val l2 = 0.14f
        val v = sub(ankle, hip); val d = len(v).coerceIn(0.02f, l1 + l2 - 0.001f); val u = norm(v)
        val along = (l1 * l1 - l2 * l2 + d * d) / (2 * d)
        val h = sqrt(max(0f, l1 * l1 - along * along))
        val bend = norm(floatArrayOf(side * 0.25f, 0f, 1f).let { b -> val k = dot(b, u); floatArrayOf(b[0] - u[0] * k, b[1] - u[1] * k, b[2] - u[2] * k) })
        return floatArrayOf(hip[0] + u[0] * along + bend[0] * h, hip[1] + u[1] * along + bend[1] * h, hip[2] + u[2] * along + bend[2] * h)
    }

    // Small matrix helpers (column-major, as Filament wants).

    private fun set(name: String, m: FloatArray) {
        val e = nodes[name] ?: return
        val tm = engine.transformManager
        tm.setTransform(tm.getInstance(e), m)
    }
    private fun at(name: String, p: FloatArray) = set(name, translate(p[0], p[1], p[2]))
    private fun segment(name: String, a: FloatArray, b: FloatArray) {
        val mid = floatArrayOf((a[0] + b[0]) / 2, (a[1] + b[1]) / 2, (a[2] + b[2]) / 2)
        val d = sub(b, a)
        set(name, mul(translate(mid[0], mid[1], mid[2]), mul(alignY(norm(d)), scale(1f, len(d), 1f))))
    }
    private fun glow(material: String, r: Float, g: Float, b: Float, strength: Float) {
        val mi = materials[material] ?: return
        if (mi.material.hasParameter("emissiveStrength")) { mi.setParameter("emissiveStrength", 1f) }
        mi.setParameter("emissiveFactor", r * strength, g * strength, b * strength)
    }

    private fun identity() = FloatArray(16).also { it[0] = 1f; it[5] = 1f; it[10] = 1f; it[15] = 1f }
    private fun translate(x: Float, y: Float, z: Float) = identity().also { it[12] = x; it[13] = y; it[14] = z }
    private fun scale(x: Float, y: Float, z: Float) = identity().also { it[0] = x; it[5] = y; it[10] = z }
    private fun scaleAbout(x: Float, y: Float, z: Float) = scale(x, y, z)
    private fun rotX(deg: Float): FloatArray { val r = Math.toRadians(deg.toDouble()); val c = cos(r).toFloat(); val s = sin(r).toFloat()
        return identity().also { it[5] = c; it[6] = s; it[9] = -s; it[10] = c } }
    private fun rotY(deg: Float): FloatArray { val r = Math.toRadians(deg.toDouble()); val c = cos(r).toFloat(); val s = sin(r).toFloat()
        return identity().also { it[0] = c; it[2] = -s; it[8] = s; it[10] = c } }
    private fun rotZ(deg: Float): FloatArray { val r = Math.toRadians(deg.toDouble()); val c = cos(r).toFloat(); val s = sin(r).toFloat()
        return identity().also { it[0] = c; it[1] = s; it[4] = -s; it[5] = c } }
    private fun mul(a: FloatArray, b: FloatArray): FloatArray { val o = FloatArray(16)
        for (col in 0..3) for (row in 0..3) { var s = 0f; for (k in 0..3) s += a[k * 4 + row] * b[col * 4 + k]; o[col * 4 + row] = s }
        return o }
    private fun apply(m: FloatArray, x: Float, y: Float, z: Float) =
        floatArrayOf(m[0] * x + m[4] * y + m[8] * z + m[12], m[1] * x + m[5] * y + m[9] * z + m[13], m[2] * x + m[6] * y + m[10] * z + m[14])
    /** Turns +Y onto [d] (a unit vector) by the shortest way. */
    private fun alignY(d: FloatArray): FloatArray {
        val c = d[1].coerceIn(-1f, 1f)
        val axis = floatArrayOf(d[2], 0f, -d[0]) // (0,1,0) × d
        val s = len(axis)
        if (s < 1e-6f) return if (c > 0) identity() else rotX(180f)
        val (x, y, z) = Triple(axis[0] / s, axis[1] / s, axis[2] / s)
        val t = 1 - c; val sn = sin(acos(c))
        return floatArrayOf(t * x * x + c, t * x * y + sn * z, t * x * z - sn * y, 0f,
            t * x * y - sn * z, t * y * y + c, t * y * z + sn * x, 0f,
            t * x * z + sn * y, t * y * z - sn * x, t * z * z + c, 0f, 0f, 0f, 0f, 1f)
    }
    private fun add(p: FloatArray, x: Float, y: Float, z: Float) = floatArrayOf(p[0] + x, p[1] + y, p[2] + z)
    private fun sub(a: FloatArray, b: FloatArray) = floatArrayOf(a[0] - b[0], a[1] - b[1], a[2] - b[2])
    private fun dot(a: FloatArray, b: FloatArray) = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
    private fun len(a: FloatArray) = sqrt(dot(a, a))
    private fun norm(a: FloatArray): FloatArray { val l = len(a).coerceAtLeast(1e-6f); return floatArrayOf(a[0] / l, a[1] / l, a[2] / l) }
    private fun lerp(a: FloatArray, b: FloatArray, t: Float) = floatArrayOf(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t)
}

private val OUTFITS = listOf("santa", "witch", "beanie", "nightcap", "bunny", "flag", "flower", "sunglasses", "scarf", "rosette")
private val NODE_NAMES = listOf("pip", "pelvis", "head", "antenna", "bulb") + OUTFITS +
    listOf("L", "R").flatMap { s -> listOf("eyeShape$s", "lidTop$s", "lidBot$s", "upperArm$s", "foreArm$s", "thigh$s", "shin$s",
        "shoulder$s", "elbow$s", "hand$s", "hip$s", "knee$s", "ankle$s", "foot$s", "finger0$s", "finger1$s", "finger2$s") } +
    (0 until 7).map { "mouth$it" }
