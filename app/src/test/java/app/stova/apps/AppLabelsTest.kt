package app.stova.apps

import org.junit.Assert.*
import org.junit.Test

class AppLabelsTest {
    private val labels=AppVariantLabels("Utvikling","TV","Mobil","Android")
    private fun named(apps: List<InstalledApp>)=disambiguateAppLabels(apps,labels) {name,variant -> "$name · $variant"}
    private fun app(pkg:String,tv:Boolean=true,development:Boolean=false)=InstalledApp("$pkg/.Main",pkg,".Main","Spole",tv,development=development)
    @Test fun developmentBuildIsDistinctWhileTheOrdinaryNameAndLaunchKeysStayStable() {
        val release=app("app.spole"); val debug=app("app.spole.debug",development=true)
        val result=named(listOf(release,debug))
        assertEquals(listOf("Spole","Spole · Utvikling"),result.map {it.label})
        assertEquals(listOf(release.key,debug.key),result.map {it.key})
    }
    @Test fun tvAndMobileVariantsAreUnderstandableAndEqualVariantsStillDiffer() {
        assertEquals(listOf("Spole · TV","Spole · Mobil"),named(listOf(app("tv.spole"),app("mobile.spole",tv=false))).map {it.label})
        val ambiguous=named(listOf(app("org.alpha.player"),app("org.beta.player")))
        assertEquals(2,ambiguous.map {it.label}.distinct().size)
        assertEquals("Spole",named(listOf(app("app.spole"))).single().label)
        assertEquals(listOf("Spole · TV","Spole · Android"),
            named(listOf(app("tv.emby.embyatv"),app("com.mb.android"))).map {it.label})
    }
}
