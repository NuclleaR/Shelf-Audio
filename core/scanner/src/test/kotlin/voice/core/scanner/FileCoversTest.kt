package voice.core.scanner

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import voice.core.data.Chapter
import voice.core.data.ChapterId
import java.io.File
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class FileCoversTest {

  @get:Rule
  val testFolder = TemporaryFolder()

  private val coverExtractor = mockk<CoverExtractor>()
  private val fileCovers = FileCovers(ApplicationProvider.getApplicationContext(), coverExtractor)

  @Test
  fun `the cover embedded in a file is read once`() = runTest {
    coEvery { coverExtractor.extractCover(any(), any()) } answers {
      secondArg<File>().writeText("cover")
      true
    }
    val chapter = chapter(File(testFolder.root, "1.mp3").toURI().toString())

    val cover = fileCovers.cover(chapter)

    assertEquals(expected = "cover", actual = cover!!.readText())
    assertEquals(expected = cover, actual = fileCovers.cover(chapter))
    coVerify(exactly = 1) { val _ = coverExtractor.extractCover(any(), any()) }
  }

  @Test
  fun `a file without an embedded cover is read once`() = runTest {
    coEvery { coverExtractor.extractCover(any(), any()) } returns false
    val chapter = chapter(File(testFolder.root, "1.mp3").toURI().toString())

    assertNull(fileCovers.cover(chapter))
    assertNull(fileCovers.cover(chapter))
    coVerify(exactly = 1) { val _ = coverExtractor.extractCover(any(), any()) }
  }

  @Test
  fun `a file on a server is not read`() = runTest {
    assertNull(fileCovers.cover(chapter("https://example.com/1.mp3")))
    coVerify(exactly = 0) { val _ = coverExtractor.extractCover(any(), any()) }
  }

  private fun chapter(id: String) = Chapter(
    id = ChapterId(id),
    name = null,
    duration = 1000,
    fileLastModified = Instant.ofEpochMilli(testFolder.root.lastModified()),
    fileSize = 0,
    markData = emptyList(),
  )
}
