@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package app.stova.media
import app.stova.core.storage.*
import app.stova.core.security.*
import app.stova.media.model.*
import app.stova.media.net.*
import app.stova.home.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder

class MergeContinueTest {
 private fun ep(id:String,series:String)=MediaItem(id,"a",ItemType.EPISODE,id,seriesId=series)
 @Test fun resumeFirstAndNoSeriesTwice()=assertEquals(listOf("r1","n2"),mergeContinue(listOf(ep("r1","s1")),listOf(ep("n1","s1"),ep("n2","s2"),ep("r1","s1"))).map { it.id })
}
