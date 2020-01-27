package io.getstream.chat.android.core.poc

import io.getstream.chat.android.core.poc.library.*
import io.getstream.chat.android.core.poc.library.rest.ChannelQueryRequest
import io.getstream.chat.android.core.poc.library.socket.ChatSocket
import io.getstream.chat.android.core.poc.library.socket.ConnectionData
import io.getstream.chat.android.core.poc.utils.RetroSuccess
import io.getstream.chat.android.core.poc.utils.SuccessCall
import io.getstream.chat.android.core.poc.utils.SuccessTokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class ChannelsApiCallsTests {

    val user = User("test-id")
    val connection = ConnectionData("connection-id", user)
    val tokenProvider = SuccessTokenProvider()

    val channelType = "test-type"
    val channelId = "test-id"

    lateinit var api: ChatApi
    lateinit var socket: ChatSocket
    lateinit var client: ChatClient
    lateinit var retrofitApi: RetrofitApi

    val apiKey = "api-key"

    @Before
    fun before() {
        api = Mockito.mock(ChatApi::class.java)
        socket = Mockito.mock(ChatSocket::class.java)
        retrofitApi = Mockito.mock(RetrofitApi::class.java)
        client = ChatClientImpl(ChatApiImpl(apiKey, retrofitApi), socket)

        Mockito.`when`(socket.connect(user, tokenProvider)).thenReturn(SuccessCall(connection))

        client.setUser(user, tokenProvider) {}
    }

    @Test
    fun queryChannelSuccess() {

        val response = Channel()

        Mockito.`when`(
            retrofitApi.queryChannel(
                channelType,
                channelId,
                apiKey,
                user.id,
                connection.connectionId,
                ChannelQueryRequest()
            )
        ).thenReturn(RetroSuccess(ChannelState().apply { channel = response }))

        val result = client.queryChannel(channelType, channelId, ChannelQueryRequest()).execute()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.data()).isEqualTo(response)
    }

    @Test
    fun showChannelSuccess() {

        Mockito.`when`(
            retrofitApi.showChannel(
                channelType,
                channelId,
                apiKey,
                connection.connectionId,
                emptyMap()
            )
        ).thenReturn(RetroSuccess(CompletableResponse()))

        val result = client.showChannel(channelType, channelId).execute()

        assertThat(result.isSuccess).isTrue()
    }


}

