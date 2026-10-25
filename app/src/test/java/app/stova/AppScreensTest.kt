package app.stova

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

import coil3.request.transformations
import android.content.ComponentName
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import app.stova.home.*
import app.stova.ha.HaEntity
import app.stova.ha.HaStatus
import app.stova.ha.ui.SmartHomeContent
import app.stova.ha.cards.CardConfig
import app.stova.ha.cards.CardType
import app.stova.ha.ui.ChipContent
import app.stova.ha.ui.HomeStatusChips
import app.stova.ha.ui.chipText
import app.stova.media.*
import app.stova.media.model.*
import app.stova.background.BackdropLayer
import app.stova.core.theme.StovaTheme
import app.stova.navigation.TopBar
import app.stova.profiles.*
import app.stova.apps.InstalledApp
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.BitmapFactory
import androidx.core.graphics.ColorUtils
import org.junit.Assert.assertTrue
import java.io.File
import java.util.Locale
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.performClick
import org.robolectric.Shadows.shadowOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.stova.core.theme.AvatarColor
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The real app, rendered for review. Run with -Pstova.screens=true. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], qualifiers = TV_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppScreensTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val container get() = (rule.activity.application as StovaApplication).container

    @Before fun enabled() = assumeScreensEnabled()

    @Test fun firstRun() {
        rule.saveScreen("02-first-run")
    }

    @Test fun picker() {
        runBlocking {
            val a = container.profiles.create("Demo", AvatarColor.LIME, false)
            container.profiles.create("Liten", AvatarColor.ROSE, true)
            container.profiles.create("Vaksen", AvatarColor.OCEAN, false)
            container.profiles.setPin(a.id, "1357")
            container.profiles.setStartInLastProfile(false)
            container.session.leave()
        }
        rule.saveScreen("03-picker")
    }

    @Test fun shell() {
        runBlocking { container.session.enter(container.profiles.create("Demo", AvatarColor.LIME, false).id) }
        rule.saveScreen("04-shell")
    }

    @Test fun settings() {
        runBlocking {
            val a = container.profiles.create("Demo", AvatarColor.LIME, false)
            container.profiles.create("Liten", AvatarColor.ROSE, true)
            container.profiles.setPin(a.id, "1357")
            container.session.enter(a.id)
        }
        rule.waitForIdle()
        rule.onAllNodesWithText("Innstillingar")[0].performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Liten").fetchSemanticsNodes().isNotEmpty() }
        rule.saveScreen("07-settings")
        rule.onNodeWithText("Home-knappen").performClick()
        rule.waitForIdle()
        rule.saveScreen("08-home-button")
    }

    private fun installFakeApps(): List<String> {
        val pm = shadowOf(rule.activity.packageManager)
        return listOf("Spole", "Jellyfin", "Tunet", "Kodi", "VLC", "Innstillingar").map { label ->
            val pkg = "demo.${label.lowercase()}"
            val component = ComponentName(pkg, "$pkg.Main")
            pm.addActivityIfNotPresent(component).nonLocalizedLabel = label
            pm.addIntentFilterForActivity(component, IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER) })
            "$pkg/$pkg.Main"
        }
    }

    @Test fun homeWithFavorites() {
        val keys = installFakeApps()
        runBlocking {
            val id = container.profiles.create("Demo", AvatarColor.LIME, false).id
            keys.take(4).forEach { container.apps.toggleFavorite(id, it) }
            container.session.enter(id)
        }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Favorittappar").fetchSemanticsNodes().isNotEmpty() }
        rule.saveScreen("05-home")
    }

    @Test fun appsPage() {
        val keys = installFakeApps()
        runBlocking {
            val id = container.profiles.create("Demo", AvatarColor.LIME, false).id
            keys.take(2).forEach { container.apps.toggleFavorite(id, it) }
            container.session.enter(id)
        }
        rule.waitForIdle()
        rule.onNodeWithText("Appar").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Alle appar").fetchSemanticsNodes().isNotEmpty() }
        rule.saveScreen("06-apps")
    }

    /** Local geometric art, painted deterministically; these screens never contact a server. */
    private fun demoArt(name: String, tint: Int, brightTop: Boolean = false): String {
        val bitmap = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(if (brightTop) android.graphics.Color.rgb(235, 229, 211) else tint)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = if (brightTop) android.graphics.Color.rgb(255, 252, 236) else android.graphics.Color.rgb(175, 180, 155)
        if (brightTop) canvas.drawCircle(1000f, 55f, 230f, paint)
        else canvas.drawCircle(900f, 150f, 85f, paint)
        paint.color = android.graphics.Color.rgb(46, 67, 62)
        canvas.drawPath(android.graphics.Path().apply {
            moveTo(0f, 500f); lineTo(350f, 160f); lineTo(700f, 510f)
            lineTo(1050f, 290f); lineTo(1280f, 490f); lineTo(1280f, 720f); lineTo(0f, 720f); close()
        }, paint)
        paint.color = android.graphics.Color.rgb(27, 44, 43)
        canvas.drawRect(0f, 570f, 1280f, 720f, paint)
        val file = File(rule.activity.cacheDir, "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file.absolutePath
    }

    private fun renderMediaHome(poster: Boolean = false, fontScale: Float = 1f, chips: List<ChipContent> = emptyList(), brightTop: Boolean = false, appearance: AppearancePrefs = AppearancePrefs(), showApps: Boolean = false, seasonalShowcase: Boolean = false, packArt: app.stova.apps.AppArtCache? = null, packApps: List<InstalledApp>? = null, homePrefs: HomePrefs = HomePrefs(), previewSettings: Boolean = false, animateFocus: Boolean = false, waitForBackdrop: Boolean = !seasonalShowcase) {
        val backdrop = demoArt("demo-landscape", android.graphics.Color.rgb(79, 95, 88), brightTop)
        val logoFile = File(rule.activity.cacheDir, "demo-logo.png")
        val logo = Bitmap.createBitmap(900, 300, Bitmap.Config.ARGB_8888)
        Canvas(logo).drawText("FJORDLYS", 60f, 210f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(233, 233, 226); textSize = 150f; typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        logoFile.outputStream().use { logo.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val second = demoArt("demo-evening", android.graphics.Color.rgb(68, 77, 95))
        val account = MediaAccount("demo-account", "demo-profile", ServerKind.JELLYFIN, "demo-server", "Demoarkiv", "https://demo.invalid", "demo-user", "Demo")
        val item = MediaItem("demo-film", account.id, ItemType.MOVIE, "Fjordlys", year = 2024, runtimeMinutes = 112,
            genres = listOf("Drama", "Eventyr"), overview = "Ei ferd gjennom stille fjordar og opne landskap. Vegen vidare byr på nye møte og ukjende stader.",
            ratings = Ratings(7.84, 91, "12"), progress = 0.405f, art = ItemArt(backdrop = backdrop, logo = logoFile.absolutePath, poster = backdrop, wide = backdrop))
        val items = listOf(item, item.copy(id = "demo-film-2", title = "Vegen vidare", art = ItemArt(backdrop = second, poster = second, wide = second)), item.copy(id = "demo-film-3", title = "Nordavind"))
        val kind = if (poster) MediaRowKind.LATEST_MOVIES else MediaRowKind.CONTINUE
        val key = if (poster) HomeRowKey.latestMovies(account.id) else HomeRowKey.continueWatching(account.id)
        val date = java.time.LocalDate.of(2026,10,25)
        val activeSeason = appearance.activeSeason(date)
        val seasonalRow = if (seasonalShowcase && activeSeason != Season.NONE) listOf(HomeRowUi.Media(
            HomeRowKey.seasonal(account.id,activeSeason),account,
            if(activeSeason==Season.HALLOWEEN) MediaRowKind.HALLOWEEN else MediaRowKind.CHRISTMAS,items,false)) else emptyList()
        val apps = if (showApps) packApps ?: appearanceApps() else emptyList()
        val rows = seasonalRow + (if (showApps) listOf(HomeRowUi.Apps(HomeRowKey.APPS, apps)) else emptyList()) + listOf(HomeRowUi.Media(key, account, kind, items, false),
            HomeRowUi.Media(HomeRowKey.latestSeries(account.id), account, MediaRowKind.LATEST_SERIES,
                listOf(item.copy(id = "demo-episode", type = ItemType.EPISODE, seriesName = "Fjordlys", season = 1, episode = 2)), false))
        runBlocking {
            val loader = coil3.SingletonImageLoader.get(rule.activity)
            loader.execute(coil3.request.ImageRequest.Builder(rule.activity).data(logoFile.absolutePath).size(800,240).transformations(app.stova.media.ui.TrimTransparent()).build())
        }
        val settingsProfile = if (previewSettings) runBlocking {
            // A controlled profile avoids the first-run gate starting Home before its snapshot exists.
            val saved = Profile("appearance-preview","Demo",AvatarColor.LIME,appearance=appearance,home=homePrefs)
            val mediaAccount = account.copy(profileId=saved.id)
            app.stova.core.storage.JsonFileStore(File(rule.activity.filesDir,"media-accounts.json"),
                MediaAccountsFile.serializer(),MediaAccountsFile()).update { MediaAccountsFile(listOf(mediaAccount)) }
            app.stova.core.storage.JsonFileStore(File(rule.activity.filesDir,"snapshots/${saved.id}.json"),
                MediaSnapshot.serializer(),MediaSnapshot()).update {
                MediaSnapshot(spotlight=listOf(SnapshotSpotlight(mediaAccount.id,item.copy(accountId=mediaAccount.id),true)))
            }
            saved
        } else null
        rule.runOnUiThread {
            rule.activity.setContent {
                StovaTheme(reduceMotion = !seasonalShowcase && !animateFocus, appearance = appearance, date = date) {
                    val inputMode = androidx.compose.ui.platform.LocalInputModeManager.current
                    SideEffect { inputMode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
                    val density = LocalDensity.current
                    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                        Box(Modifier.fillMaxSize()) {
                            if (previewSettings) Box(Modifier.matchParentSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background))
                            else BackdropLayer(app.stova.background.backgroundUrl(appearance,backdrop,second))
                            if (previewSettings) app.stova.settings.SettingsScreen(container, settingsProfile!!,
                                remember { HomeViewModel(container.session,emptyList()) }, {}) else
                            HomeContent(rows, if (showApps) FocusedTarget.App(apps.first().label) else FocusedTarget.Media(item, account), if (showApps) HomeRowKey.APPS else seasonalRow.firstOrNull()?.key ?: key, packArt ?: container.appArt, container.appCatalog,
                                {}, {}, {}, {}, topBar = {
                                    TopBar(Profile("demo-profile", "Demo", AvatarColor.LIME), {}, {}, {},
                                        onSmartHome = if (chips.isNotEmpty()) ({}) else null,
                                        leading = { HomeStatusChips(chips) })
                                }, spotlight = app.stova.media.SpotlightEntry(item, account, newSeason = true),homePrefs=homePrefs,canEditRows=true)
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        if(seasonalShowcase) { rule.mainClock.advanceTimeBy(1_200); rule.waitForIdle() }
        if (appearance.heroSize != HeroSize.OFF && !previewSettings) {
            rule.onNodeWithText("★ 7,8").assertIsDisplayed()
            rule.onNodeWithText(app.stova.media.ui.criticText(91, java.util.Locale.forLanguageTag("nn"))).assertExists()
        }
        // Decode the same sized requests into Coil's memory cache before taking the window image.
        runBlocking {
            val loader = coil3.SingletonImageLoader.get(rule.activity)
            for (url in listOf(backdrop, second)) for ((w, h) in listOf(1920 to 1080, 496 to 279, 264 to 396)) {
                loader.execute(coil3.request.ImageRequest.Builder(rule.activity).data(url).size(w, h).build())
            }
        }
        rule.waitForIdle()
        if (previewSettings) {
            rule.onNodeWithText("Utsjånad").performClick()
            rule.onNode(hasText("Tema") and hasClickAction()).performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("preview-backdrop",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty() }
            rule.onNode(hasText("Tema") and hasClickAction()).performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            rule.mainClock.advanceTimeBy(250)
            return
        }
        if (waitForBackdrop) app.stova.background.backgroundUrl(appearance,backdrop,second)?.let { expected ->
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("backdrop:$expected").fetchSemanticsNodes().isNotEmpty() }
        }
        val label = if(showApps) apps.first().label else "Fjordlys"
        rule.onAllNodes(hasContentDescription(label) and hasClickAction())[0].performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        rule.onAllNodes(hasContentDescription(label) and hasClickAction())[0].assertIsFocused()
        rule.mainClock.advanceTimeBy(if(seasonalShowcase) 1_200 else 250)
        rule.waitForIdle()
    }

    @Test fun customizationHeroOff() {
        renderMediaHome(waitForBackdrop=true,appearance=AppearancePrefs(heroSize=HeroSize.OFF,seasonChoice=SeasonChoice.NONE))
        rule.saveScreen("tilpassing-hero-av")
    }
    @Test fun customizationLargePosterRow() {
        renderMediaHome(waitForBackdrop=true,poster=true,appearance=AppearancePrefs(seasonChoice=SeasonChoice.NONE),
            homePrefs=HomePrefs(rowFormats=mapOf(HomeRowKey.latestMovies("demo-account") to RowStyle(RowFormat.POSTER,ArtworkSize.LARGE))))
        rule.saveScreen("tilpassing-plakatrad-stor")
    }
    @Test fun customizationRoundLightUp() {
        renderMediaHome(waitForBackdrop=true,appearance=AppearancePrefs(cornerRadius=CornerRadius.EXTRA_ROUND,focusEffect=FocusEffect.LIGHT_UP,seasonChoice=SeasonChoice.NONE))
        rule.saveScreen("tilpassing-ekstra-rund-lys-opp")
    }
    @Test fun customizationTopDate() {
        renderMediaHome(waitForBackdrop=true,appearance=AppearancePrefs(showDate=true,clockSize=ClockSize.LARGE,seasonChoice=SeasonChoice.NONE))
        rule.saveScreen("tilpassing-toppfelt-dato")
    }
    @Test fun customizationMoveRow() {
        renderMediaHome(waitForBackdrop=true,appearance=AppearancePrefs(seasonChoice=SeasonChoice.NONE))
        rule.onAllNodes(hasContentDescription("Fjordlys") and hasClickAction())[0].performKeyInput { pressKey(androidx.compose.ui.input.key.Key.Menu) }
        rule.onNodeWithText("Flytt rada").performClick()
        rule.saveScreen("tilpassing-flyttemodus")
    }
    @Test fun customizationSettings() {
        renderMediaHome(waitForBackdrop=true,appearance=AppearancePrefs(seasonChoice=SeasonChoice.NONE),previewSettings=true)
        rule.saveScreen("tilpassing-innstillingar")
    }
    @Test fun customizationFixedBackground() {
        renderMediaHome(waitForBackdrop=true,appearance=AppearancePrefs(backgroundSource=BackgroundSource.FIXED,heroTitle=HeroTitle.TITLE,seasonChoice=SeasonChoice.NONE))
        rule.saveScreen("tilpassing-fast-bakgrunn")
    }
    @Test fun customizationMinimalTheme() {
        renderMediaHome(waitForBackdrop=true,appearance=AppearancePrefs(heroSize=HeroSize.OFF,backgroundSource=BackgroundSource.THEME,
            showAppsButton=false,showSmartHomeButton=false,showProfile=false,clock=ClockMode.HIDDEN,cornerRadius=CornerRadius.SHARP,seasonChoice=SeasonChoice.NONE))
        rule.saveScreen("tilpassing-einsfarga-minimal")
    }
    @Test fun customizationRingZoomSmallWide() {
        renderMediaHome(waitForBackdrop=true,appearance=AppearancePrefs(focusEffect=FocusEffect.RING_ZOOM,ringWidth=RingWidth.THICK,cardSpacing=CardSpacing.AIRY,
            seasonChoice=SeasonChoice.NONE),homePrefs=HomePrefs(rowFormats=mapOf(HomeRowKey.continueWatching("demo-account") to RowStyle(RowFormat.SMALL_WIDE,ArtworkSize.SMALL,false))),animateFocus=true)
        rule.saveScreen("tilpassing-ring-zoom-liten-brei")
    }

    private fun appearanceApps(): List<InstalledApp> {
        val pm = shadowOf(rule.activity.packageManager)
        return listOf("Stova" to R.drawable.tv_banner, "Jellyfin" to R.drawable.ic_jellyfin, "Emby" to R.drawable.ic_emby).map { (label, image) ->
            val pkg = rule.activity.packageName
            val activity = "demo.$label.Main"
            val info = pm.addActivityIfNotPresent(ComponentName(pkg, activity))
            info.applicationInfo = rule.activity.applicationInfo
            info.banner = image
            info.icon = image
            InstalledApp("$pkg/$activity", pkg, activity, label, true).also { app ->
                runBlocking {
                    org.junit.Assert.assertNotNull(container.appArt.banner(app))
                    org.junit.Assert.assertNotNull(container.appArt.icon(app))
                }
            }
        }
    }

    private fun iconPackHome(prefs: AppearancePrefs, name: String): Bitmap {
        val source = DemoLineIconPack()
        val art = app.stova.apps.AppArtCache(rule.activity, iconPacks = app.stova.apps.IconPackRepository(source, File(rule.activity.cacheDir, "demo-pack")))
        val apps = source.apps
        runBlocking {
            art.iconPacks.refresh()
            apps.forEach { org.junit.Assert.assertNotNull(art.artwork(it, prefs)) }
        }
        renderMediaHome(appearance = prefs, showApps = true, packArt = art, packApps = apps)
        val file = rule.saveScreen(name)
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        if (prefs.iconPack != null && prefs.iconColor == IconColor.ACCENT) {
            val theme = prefs.seasonal(java.time.LocalDate.of(2026,10,25))
            val accent = app.stova.core.theme.stovaColorScheme(theme.visualTheme, theme.accent).primary
            val expected = accent.toArgb()
            val bounds = rule.onNode(hasContentDescription("Spole") and hasClickAction()).fetchSemanticsNode().boundsInRoot
            var tinted = 0
            // Ignore the focus ring: count accent pixels only within the artwork interior.
            for (y in bounds.top.toInt()+16 until bounds.bottom.toInt()-16) for (x in bounds.left.toInt()+16 until bounds.right.toInt()-16) {
                if (bitmap.getPixel(x,y) == expected) tinted++
            }
            assertTrue("The Spole supplement must actually be decoded and tinted ($tinted pixels)", tinted > 120)
        }
        return bitmap
    }

    @Test fun iconPackAccentForest() {
        iconPackHome(AppearancePrefs(iconPack = DemoLineIconPack.PACK, iconColor = IconColor.ACCENT, seasonChoice = SeasonChoice.NONE), "ikonpakke-vanleg")
    }
    @Test fun iconPackAccentHalloween() {
        iconPackHome(SeasonChoice.HALLOWEEN.applyTo(AppearancePrefs(iconPack = DemoLineIconPack.PACK, iconColor = IconColor.ACCENT, seasonalDecor = SeasonalDecor.CALM)), "ikonpakke-halloween")
    }
    @Test fun iconPackSquareTiles() {
        iconPackHome(AppearancePrefs(iconPack = DemoLineIconPack.PACK, iconColor = IconColor.ACCENT, appStyle = AppStyle.ICON, seasonChoice = SeasonChoice.NONE), "ikonpakke-ikonstil")
    }
    @Test fun iconPackCropToFill() {
        iconPackHome(AppearancePrefs(iconPack = DemoLineIconPack.PACK, iconFill = IconFill.CROP, seasonChoice = SeasonChoice.NONE), "ikonpakke-fyll")
    }
    @Test fun spoleWithoutIconPack() {
        val source = DemoLineIconPack()
        val art = app.stova.apps.AppArtCache(rule.activity)
        runBlocking { org.junit.Assert.assertNotNull(art.artwork(source.apps.first(), AppearancePrefs())) }
        renderMediaHome(appearance = AppearancePrefs(seasonChoice = SeasonChoice.NONE), showApps = true, packArt = art, packApps = listOf(source.apps.first()))
        rule.saveScreen("ikonpakke-spole-banner")
    }
    @Test fun iconPackPickerAndSettings() {
        val source = DemoLineIconPack()
        val prefs = AppearancePrefs(iconPack = DemoLineIconPack.PACK, iconColor = IconColor.ACCENT, seasonChoice = SeasonChoice.NONE)
        val art = app.stova.apps.AppArtCache(rule.activity, iconPacks = app.stova.apps.IconPackRepository(source, File(rule.activity.cacheDir, "demo-pack")))
        runBlocking { art.iconPacks.refresh(); art.iconPacks.index(source.installed().single()).drawables.forEach { art.packIcon(source.installed().single(),it) } }
        rule.runOnUiThread { rule.activity.setContent { StovaTheme(appearance = prefs) {
            androidx.compose.foundation.layout.Column(Modifier.fillMaxSize().padding(48.dp)) {
                app.stova.settings.IconPackSettingsContent(prefs, source.installed()) {}
            }
        } } }
        rule.saveScreen("ikonpakke-innstillingar")
        rule.runOnUiThread { rule.activity.setContent { StovaTheme(appearance = prefs) { app.stova.apps.IconPicker(art, DemoLineIconPack.PACK, {}, {}) } } }
        rule.onNodeWithText("Søk etter ikon").assertExists()
        val windows = android.view.inspector.WindowInspector.getGlobalWindowViews()
        assertTrue("The picker must have its own dialog window", windows.size > 1)
        rule.saveScreen("ikonpakke-veljar", windows.last())
    }

    @Test fun appearanceDefault() {
        renderMediaHome(showApps = true)
        rule.saveScreen("appearance-default")
    }

    @Test fun appearanceLargeWithTitles() {
        renderMediaHome(appearance = AppearancePrefs(cardSize = ArtworkSize.LARGE, showTitles = true, heroSize = HeroSize.LARGE))
        rule.saveScreen("appearance-large-titles")
    }

    @Test fun appearanceSmallIcons() {
        renderMediaHome(appearance = AppearancePrefs(cardSize = ArtworkSize.SMALL, appStyle = AppStyle.ICON), showApps = true)
        rule.saveScreen("appearance-small-icons")
    }

    @Test fun appearanceLightDimming() {
        renderMediaHome(appearance = AppearancePrefs(backdropDimming = BackdropDimming.LIGHT), brightTop = true)
        rule.saveScreen("appearance-light")
    }

    @Test fun appearanceOverviewHasTwoReadableLines() {
        renderMediaHome(appearance = AppearancePrefs(showOverview = true, cardSize = ArtworkSize.LARGE, showTitles = true))
        rule.onNodeWithText("Ei ferd gjennom stille fjordar og opne landskap. Vegen vidare byr på nye møte og ukjende stader.").assertIsDisplayed()
        rule.saveScreen("appearance-overview")
    }

    @Test fun appearanceOceanOutline() {
        renderMediaHome(appearance = AppearancePrefs(accent = Accent.OCEAN, seasonChoice = SeasonChoice.NONE, focusEffect = FocusEffect.OUTLINE))
        val bitmap = BitmapFactory.decodeFile(rule.saveScreen("appearance-ocean-outline").absolutePath)
        val card = rule.onAllNodes(hasContentDescription("Fjordlys") and hasClickAction())[0].fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertEquals("Ocean focus ring follows the artwork", AvatarColor.OCEAN.argb.toInt(), bitmap.getPixel(card.left.toInt()+2, card.top.toInt()+40))
    }

    @Test fun dimmingChoicesChangeThePictureAndKeepTextReadable() {
        val lightness = mutableListOf<Double>()
        for (level in BackdropDimming.entries) {
            renderMediaHome(appearance = AppearancePrefs(backdropDimming = level), brightTop = true)
            val bitmap = BitmapFactory.decodeFile(rule.saveScreen("appearance-contrast-${level.name.lowercase()}").absolutePath)
            lightness += ColorUtils.calculateLuminance(bitmap.getPixel(1320, 380))
            val foreground = android.graphics.Color.rgb(233, 233, 226)
            val muted = android.graphics.Color.rgb(154, 162, 153)
            assertTrue("$level: hero text", ColorUtils.calculateContrast(foreground, bitmap.getPixel(880, 410)) >= 4.5)
            assertTrue("$level: genre line", ColorUtils.calculateContrast(muted, bitmap.getPixel(880, 410)) >= 4.5)
            assertTrue("$level: top bar", ColorUtils.calculateContrast(foreground, bitmap.getPixel(1900, 96)) >= 4.5)
        }
        assertTrue("Light must reveal more art than medium: $lightness", lightness[0] > lightness[1])
        assertTrue("Medium must reveal more art than dark: $lightness", lightness[1] > lightness[2])
    }

    @Test fun homeWithHeroScoreAndMediaRows() {
        renderMediaHome()
        rule.saveScreen("09-home-media-hero")
    }

    @Test fun homeWithHeroMediaRowsAndHaStatus() {
        val weather = HaEntity("weather.demo", "cloudy", buildJsonObject { put("temperature", 4) }, 0, 0)
        val price = HaEntity("sensor.demo_price", "135", buildJsonObject { put("unit_of_measurement", "øre") }, 0, 0)
        val locale = Locale.forLanguageTag("nn")
        renderMediaHome(chips = listOf(
            chipText(CardConfig("weather", CardType.WEATHER, weather.id, onHome = true), weather, locale),
            chipText(CardConfig("price", CardType.ENERGY_PRICE, price.id, onHome = true), price, locale),
        ))
        rule.onNodeWithText("4°").assertIsDisplayed()
        rule.onNodeWithText("135 øre").assertIsDisplayed()
        rule.onNodeWithText("Smarthus").assertIsDisplayed()
        rule.onNodeWithText("Hald fram").assertIsDisplayed()
        rule.onNodeWithText("Nyleg lagt til · Seriar").assertIsDisplayed()
        rule.onNodeWithText("2024").assertIsDisplayed()
        rule.onNodeWithText("12 år").assertIsDisplayed()
        rule.saveScreen("12-home-media-ha")
    }

    @Test fun homeWithMoviePosters() {
        renderMediaHome(poster = true)
        rule.saveScreen("10-home-media-posters")
    }

    @Test fun topBarIsReadableOverBrightSun() {
        renderMediaHome(brightTop = true)
        val file = rule.saveScreen("13-home-bright-backdrop")
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        // At clock/icon height, beyond the safe edge so no glyph contaminates the sample.
        val skyBehindTopBar = bitmap.getPixel(1900, 96)
        val foreground = android.graphics.Color.rgb(233, 233, 226)
        val contrast = ColorUtils.calculateContrast(foreground, skyBehindTopBar)
        assertTrue("Top bar contrast over bright art: $contrast (expected >= 6)", contrast >= 6.0)
        // Bright sky at 35% width, below the top scrim and clear of text/mountains.
        val skyBehindHero = bitmap.getPixel(880, 410)
        val muted = android.graphics.Color.rgb(154, 162, 153)
        assertTrue("Hero text contrast over bright art", ColorUtils.calculateContrast(foreground, skyBehindHero) >= 6.0)
        assertTrue("Hero metadata/score contrast over bright art", ColorUtils.calculateContrast(muted, skyBehindHero) >= 4.5)
        // The reduced scrim must leave the artwork clear across the right half.
        assertTrue("Middle-right art stays visible", ColorUtils.calculateLuminance(bitmap.getPixel(1320, 380)) > 0.3)
        // The additional scrim ends at 30%; the right-side sky below it stays bright.
        assertTrue("Bright fixture must actually be loaded", ColorUtils.calculateLuminance(bitmap.getPixel(1900, 380)) > 0.6)
    }

    @Test fun smartHomeHeadingAndStatus() {
        val weather = HaEntity("weather.demo", "sunny", buildJsonObject {
            put("friendly_name", "Vêret ute"); put("temperature", 4); put("temperature_unit", "°C")
        }, 0, 0)
        val price = HaEntity("sensor.demo_price", "135", buildJsonObject {
            put("friendly_name", "Straumpris"); put("unit_of_measurement", "øre")
        }, 0, 0)
        val light = HaEntity("light.demo", "off", buildJsonObject { put("friendly_name", "Stovelys") }, 0, 0)
        val entities = listOf(weather, price, light).associateBy { it.id }
        val cards = listOf(CardConfig("w", CardType.WEATHER, weather.id),
            CardConfig("p", CardType.SENSOR, price.id), CardConfig("l", CardType.LIGHT, light.id))
        rule.runOnUiThread { rule.activity.setContent { StovaTheme {
            SmartHomeContent(Profile("demo", "Demo", AvatarColor.LIME), cards, entities, HaStatus.CONNECTED)
        } } }
        rule.onNodeWithText("Smarthus").assertIsDisplayed()
        rule.onNodeWithText("Tilkopla").assertIsDisplayed()
        rule.saveScreen("14-smarthome-heading-aligned")
    }

    @Test fun mediaHeroAtDoubleFontSize() {
        renderMediaHome(fontScale = 2f)
        rule.onAllNodesWithText("Demoarkiv")[0].assertIsDisplayed()
        rule.saveScreen("11-home-media-font-2")
    }

    @Test fun temaHalloweenFullFollowsOctoberCalendar() {
        rule.mainClock.autoAdvance=false
        renderMediaHome(appearance=AppearancePrefs(),seasonalShowcase=true)
        rule.onNodeWithText("Halloween").assertIsDisplayed()
        rule.saveScreen("tema-halloween")
    }
    @Test fun temaChristmasFull() {
        rule.mainClock.autoAdvance=false
        renderMediaHome(appearance=SeasonChoice.CHRISTMAS.applyTo(AppearancePrefs()),seasonalShowcase=true)
        rule.onNodeWithText("Jul").assertIsDisplayed()
        rule.saveScreen("tema-jul")
    }
    @Test fun temaMidnightOcean() {
        renderMediaHome(appearance=AppearancePrefs(visualTheme=app.stova.core.theme.VisualTheme.MIDNIGHT,accent=Accent.OCEAN,seasonChoice=SeasonChoice.NONE))
        rule.saveScreen("tema-midnatt-hav")
    }
    @Test fun temaCinemaPearl() {
        renderMediaHome(appearance=AppearancePrefs(visualTheme=app.stova.core.theme.VisualTheme.CINEMA,accent=Accent.PEARL,seasonChoice=SeasonChoice.NONE))
        rule.saveScreen("tema-kino-perle")
    }
    @Test fun temaForestWithoutSeason() {
        renderMediaHome(appearance=AppearancePrefs(seasonChoice=SeasonChoice.NONE))
        rule.onNodeWithText("Halloween").assertDoesNotExist()
        rule.saveScreen("tema-skog")
    }

    @Test fun temaSettingsPreview() {
        runBlocking {
            val p = container.profiles.create("Demo", AvatarColor.LIME, false)
            container.profiles.update(p.id) { it.copy(appearance=SeasonChoice.HALLOWEEN.applyTo(it.appearance)) }
            container.session.enter(p.id)
        }
        rule.waitForIdle()
        rule.onAllNodesWithText("Innstillingar")[0].performClick()
        rule.onNodeWithText("Utsjånad").performClick()
        rule.onNode(hasText("Tema") and hasClickAction()).performClick()
        rule.onNodeWithText("Sesongpynt").performScrollTo().assertIsDisplayed()
        rule.saveScreen("tema-innstillingar")
    }
}
