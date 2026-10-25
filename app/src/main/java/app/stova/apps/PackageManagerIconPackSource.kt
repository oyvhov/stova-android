package app.stova.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringWriter

class PackageManagerIconPackSource(context: Context) : IconPackSource {
    private val pm = context.packageManager

    override fun installed(): List<IconPack> = ACTIONS.flatMap { action ->
        pm.queryIntentActivities(Intent(action), 0).mapNotNull { info ->
            try {
                val pkg = info.activityInfo.packageName
                val version = pm.getPackageInfo(pkg, 0)
                IconPack(pkg, pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(), version.longVersionCode, version.lastUpdateTime)
            } catch (_: PackageManager.NameNotFoundException) { null }
        }
    }.distinctBy { it.packageName }.sortedWith(compareBy<IconPack> { !it.monochrome }.thenBy { it.label.lowercase() })

    override fun appFilter(pack: IconPack): String? = xml(pack, "appfilter")
    override fun drawableCatalog(pack: IconPack): String? = xml(pack, "drawable")

    // Binary res/xml files must go through Resources.getXml, never a raw input stream.
    @Suppress("DiscouragedApi")
    private fun xml(pack: IconPack, name: String): String? = try {
        val res = pm.getResourcesForApplication(pack.packageName)
        val id = res.getIdentifier(name, "xml", pack.packageName)
        val compiled = if (id == 0) null else try {
            res.getXml(id).use { parser ->
                val output = StringWriter()
                val writer = Xml.newSerializer().apply { setOutput(output); startTag(null, "resources") }
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType == XmlPullParser.START_TAG && parser.name == "item") {
                        writer.startTag(null, "item")
                        for (i in 0 until parser.attributeCount) writer.attribute(null, parser.getAttributeName(i), parser.getAttributeValue(i))
                        writer.endTag(null, "item")
                    }
                }
                writer.endTag(null, "resources"); writer.flush()
                output.toString()
            }
        } catch (_: Resources.NotFoundException) { null }
        catch (_: org.xmlpull.v1.XmlPullParserException) { null }
        compiled ?: try { res.assets.open("$name.xml").bufferedReader().use { it.readText() } } catch (_: java.io.IOException) { null }
    } catch (_: PackageManager.NameNotFoundException) { null }

    @Suppress("DiscouragedApi")
    override fun drawable(pack: IconPack, name: String, density: Int): Drawable? = try {
        val res = pm.getResourcesForApplication(pack.packageName)
        val id = res.getIdentifier(name, "drawable", pack.packageName)
        if (id == 0) null else res.getDrawableForDensity(id, density, null)
    } catch (_: PackageManager.NameNotFoundException) { null }
    catch (_: Resources.NotFoundException) { null }

    companion object {
        val ACTIONS = listOf("org.adw.launcher.THEMES", "com.novalauncher.THEME", "org.adw.ActivityStarter.THEMES",
            "com.gau.go.launcherex.theme", "org.adw.launcher.icons.ACTION_PICK_ICON")
    }
}
