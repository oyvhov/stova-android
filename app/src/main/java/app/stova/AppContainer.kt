package app.stova

import android.content.Context

/** Hand-wired dependencies for the whole app. Later tasks add their repositories here. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
}
