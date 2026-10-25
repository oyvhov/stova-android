package app.stova.ha.cards

import androidx.compose.runtime.*
import app.stova.ha.HaEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Debounce only unsent input. A newer key must never cancel an in-flight HA request. */
@Stable
class PanelState(private val scope:CoroutineScope,initial:HaEntity?,private val send:suspend (ServiceCall)->Boolean) {
    var live by mutableStateOf(initial)
    private var draft by mutableStateOf<HaEntity?>(null)
    val shown:HaEntity? get()=draft ?: live
    var pending by mutableStateOf(false);private set
    var failed by mutableStateOf(false);private set
    private var version=0
    private var debounce:Job?=null
    private var scheduled:Pair<ServiceCall,Int>?=null
    private var queued=0
    private val mutex=Mutex()

    fun submit(call:ServiceCall,delayed:Boolean=false) {
        failed=false
        // Preserve an earlier slider edit when a button is pressed before its debounce expires.
        if(!delayed) flush()
        debounce?.cancel()
        val revision=++version
        shown?.let {draft=optimisticEntity(it,call)}
        scheduled=call to revision
        pending=true
        if(delayed) debounce=scope.launch {delay(300);flush()} else flush()
    }
    private fun flush() {
        val (call,revision)=scheduled ?: return
        scheduled=null
        queued++
        scope.launch {
            try {
                mutex.withLock {
                    val success=try {send(call)} catch(e:CancellationException) {throw e} catch(_:Exception) {false}
                    if(revision==version) {draft=null;failed=!success}
                }
            } finally {queued--;pending=queued>0 || scheduled!=null}
        }
    }
}

internal fun percentageStep(repeat:Int)=when {repeat>=14->10;repeat>=5->5;else->1}
