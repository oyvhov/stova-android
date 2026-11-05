package app.stova.apps

internal data class AppVariantLabels(val development: String, val tv: String, val mobile: String, val android: String)

internal fun disambiguateAppLabels(apps: List<InstalledApp>, labels: AppVariantLabels,
    format: (String, String) -> String): List<InstalledApp> {
    val groups=apps.groupBy {it.label.trim().lowercase(java.util.Locale.ROOT)}
    return apps.map {app ->
        val group=groups.getValue(app.label.trim().lowercase(java.util.Locale.ROOT))
        if(group.size==1) return@map app
        fun variant(item:InstalledApp):String? = when {
            item.development -> labels.development
            group.count {!it.development}==1 -> null
            item.tv -> labels.tv
            else -> labels.mobile
        }
        val tag=variant(app) ?: return@map app
        val same=group.filter {variant(it)==tag}
        val detail=if(same.size==1) tag else {
            // A package suffix is the last resort when two apps advertise identical identities.
            val suffix=app.packageName.substringAfterLast('.')
            if(app.packageName.startsWith("tv.") && same.count {it.packageName.startsWith("tv.")}==1) labels.tv
            else if(suffix=="android" && same.count {it.packageName.substringAfterLast('.')==suffix}==1) labels.android
            else if(same.count {it.packageName.substringAfterLast('.')==suffix}==1) suffix
            else app.packageName
        }
        app.copy(label=format(app.label,detail))
    }
}
