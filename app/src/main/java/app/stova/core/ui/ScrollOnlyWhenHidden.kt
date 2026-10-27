package app.stova.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec

/**
 * Scroll just enough to show a focused child, and not at all when it already shows. A TV's own
 * rule instead pulls every focused child a third of the way down, which slides a page whose
 * content is in view.
 */
@OptIn(ExperimentalFoundationApi::class)
internal val ScrollOnlyWhenHidden: BringIntoViewSpec = object : BringIntoViewSpec {}
