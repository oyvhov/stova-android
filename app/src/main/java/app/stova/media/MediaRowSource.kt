package app.stova.media
import app.stova.home.HomeRowSource
import app.stova.profiles.Profile
import app.stova.profiles.season
class MediaRowSource(private val repo:MediaRepository):HomeRowSource {
 override val followsSeason = true
 override fun rows(profile:Profile)=repo.rows(profile.id,profile.appearance.season())
}
