import jp.co.panpanini.ProtokConverterFactory
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET

class ProtokConverterFactoryTest{
    interface Service {
        @GET("/")
        fun getLikedItems(): Call<HasLikedItemsResponse>
    }

    val server = MockWebServer()

    val service = Retrofit.Builder()
            .addConverterFactory(ProtokConverterFactory.create())
            .baseUrl(server.url("/"))
            .build()
            .create(Service::class.java)




    @Test
    fun `deserialize`() {
        val bytes = HasLikedItemsResponse(true).protoMarshal()

        server.enqueue(MockResponse().setBody(Buffer().apply { write(bytes) }))

        val call = service.getLikedItems()

        val response = call.execute()

        assertThat(response.body()).isEqualTo(HasLikedItemsResponse(true))
    }

}
