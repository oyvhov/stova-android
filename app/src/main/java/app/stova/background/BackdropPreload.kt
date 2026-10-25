package app.stova.background

import app.stova.home.HomeRowUi

/** Bound startup work to the first three titles in each visible row, in display order. */
fun backdropPreloadUrls(rows: List<HomeRowUi>): List<String> = rows.asSequence()
    .filterIsInstance<HomeRowUi.Media>()
    .flatMap { it.items.take(3).asSequence() }
    .mapNotNull { it.art.backdrop?.takeIf(String::isNotBlank) }
    .distinct()
    .take(12)
    .toList()
