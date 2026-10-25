package app.stova.apps

import android.content.ComponentName
import android.util.Xml
import org.xmlpull.v1.XmlPullParser

/** Read each item independently: one broken third-party XML tag must not discard later icons. */
object AppFilter {
    private val comments = Regex("<!--.*?(?:-->|$)", RegexOption.DOT_MATCHES_ALL)
    private val items = Regex("<item\\b[^<>]*>", RegexOption.DOT_MATCHES_ALL)
    private val resourceName = Regex("[a-zA-Z0-9_]+")

    fun parse(xml: String): Map<String, String> = buildMap {
        attributes(xml).forEach { attrs ->
            val component = attrs["component"] ?: return@forEach
            val name = attrs["drawable"] ?: return@forEach
            if (!component.startsWith("ComponentInfo{") || !component.endsWith("}")) return@forEach
            val key = normalize(component.removePrefix("ComponentInfo{").removeSuffix("}")) ?: return@forEach
            if (validDrawable(name)) putIfAbsent(key, name)
        }
    }

    fun drawables(xml: String): List<String> = attributes(xml).mapNotNull { it["drawable"] }
        .filter(::validDrawable).distinct().toList()

    fun normalize(key: String): String? {
        if (key.count { it == '/' } != 1 || key.any { it.isWhitespace() || it == '{' || it == '}' }) return null
        val component = ComponentName.unflattenFromString(key) ?: return null
        if (component.packageName.isBlank() || component.className.isBlank()) return null
        return component.flattenToString()
    }

    fun validDrawable(name: String): Boolean = resourceName.matches(name)

    private fun attributes(xml: String): Sequence<Map<String, String>> =
        items.findAll(comments.replace(xml, "")).mapNotNull { match ->
            try {
                val parser = Xml.newPullParser()
                val item = match.value.removeSuffix(">").removeSuffix("/") + "/>"
                parser.setInput(item.reader())
                if (parser.nextTag() != XmlPullParser.START_TAG) null
                else (0 until parser.attributeCount).associate { parser.getAttributeName(it) to parser.getAttributeValue(it) }
            } catch (_: org.xmlpull.v1.XmlPullParserException) { null }
            catch (_: java.io.IOException) { null }
        }
}
