import jp.co.panpanini.CodeGenerator
import org.assertj.core.api.Assertions.*
import org.junit.Test
import pbandk.gen.pb.CodeGeneratorRequest
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class MessageGeneratorTest {

    @Test
    fun `mappy input produces the two expected classes`() {
        val request = createRequest("mappy.input")
        val mappy = getResourceString("Mappy.kt", Source)
        val thing = getResourceString("Thing.kt", Source)

        val files = CodeGenerator(request).generate()

        assertThat(files.size).isEqualTo(2)

        assertThat(files[0].name).isEqualTo("api/Mappy.kt")
        assertThat(files[1].name).isEqualTo("api/Thing.kt")

        assertThat(files[0].content).isEqualTo(mappy)
        assertThat(files[1].content).isEqualTo(thing)
    }

    @Test
    fun `language input produces the expected enum class`() {
        val request = createRequest("language.input")
        val language = getResourceString("Language.kt", Source)

        val files = CodeGenerator(request).generate()

        assertThat(files.size).isEqualTo(1)

        assertThat(files[0].name).isEqualTo("api/Language.kt")

        assertThat(files[0].content).isEqualTo(language)
    }

    private fun createRequest(resourceName: String) =
            CodeGeneratorRequest.protoUnmarshal(getResourceBytes(resourceName, Input))

    private fun getResourceBytes(resourceName: String, type: FileType) =
            FileInputStream(File("src/test/resources/${type.location}/$resourceName")).use(InputStream::readBytes)

    private fun getResourceString(resourceName: String, type: FileType) =
            FileInputStream(File("src/test/resources/${type.location}/$resourceName"))
                    .use { it.reader().readText() }
}

sealed class FileType(val location: String)
object Source : FileType("kotlin")
object Input : FileType("input")