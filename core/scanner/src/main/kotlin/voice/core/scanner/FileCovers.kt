package voice.core.scanner

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import voice.core.data.Chapter
import voice.core.data.toUri
import java.io.File
import java.util.UUID

private val localSchemes = setOf("file", "content")

@SingleIn(AppScope::class)
@Inject
public class FileCovers
internal constructor(
  private val context: Context,
  private val coverExtractor: CoverExtractor,
) {

  private val mutex = Mutex()
  private val withoutCover = mutableSetOf<File>()

  public suspend fun cover(chapter: Chapter): File? = mutex.withLock {
    val uri = chapter.id.toUri()
    if (uri.scheme !in localSchemes) return null

    val name = UUID.nameUUIDFromBytes("${chapter.id.value}|${chapter.fileLastModified}".toByteArray())
    val cover = File(context.cacheDir, "fileCovers/$name")
    if (cover in withoutCover) return null

    withContext(Dispatchers.IO) {
      if (cover.exists()) return@withContext cover
      cover.parentFile?.mkdirs()
      val extracted = File(cover.path + ".part")
      try {
        if (coverExtractor.extractCover(uri, extracted) && extracted.length() > 0 && extracted.renameTo(cover)) {
          cover
        } else {
          withoutCover += cover
          null
        }
      } finally {
        extracted.delete()
      }
    }
  }
}
