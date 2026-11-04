package app.stova.ha

/** Some receivers keep the previous track's metadata while playing the TV input. */
internal val HaEntity.isTvAudio: Boolean
    get() = attributes.text("source")?.trim().equals("TV", ignoreCase = true) ||
        attributes.text("media_content_id")?.startsWith("x-sonos-htastream:", ignoreCase = true) == true
