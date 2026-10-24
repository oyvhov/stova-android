package app.stova.media
import app.stova.home.HomeRowSource
import app.stova.profiles.Profile
class MediaRowSource(private val repo:MediaRepository):HomeRowSource {
 override fun rows(profile:Profile)=repo.rows(profile.id)
}
