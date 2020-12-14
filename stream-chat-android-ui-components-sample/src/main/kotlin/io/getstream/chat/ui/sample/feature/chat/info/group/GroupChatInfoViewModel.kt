package io.getstream.chat.ui.sample.feature.chat.info.group

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.call.await
import io.getstream.chat.android.client.channel.ChannelClient
import io.getstream.chat.android.client.models.ChannelMute
import io.getstream.chat.android.client.models.Member
import io.getstream.chat.android.client.models.User
import io.getstream.chat.android.livedata.ChatDomain
import io.getstream.chat.ui.sample.common.name
import io.getstream.chat.ui.sample.feature.chat.info.ChatMember
import kotlinx.coroutines.launch

class GroupChatInfoViewModel(
    private val cid: String,
    private val chatDomain: ChatDomain = ChatDomain.instance(),
    private val chatClient: ChatClient = ChatClient.instance()
) : ViewModel() {

    private val channelClient: ChannelClient = chatClient.channel(cid)
    private val _state = MediatorLiveData<State>()
    private val _channelLeftState = MutableLiveData(false)
    val state: LiveData<State> = _state
    val channelLeftState: LiveData<Boolean> = _channelLeftState

    init {
        _state.value = INITIAL_STATE
        chatDomain.useCases.getChannelController(cid).enqueue { result ->
            if (result.isSuccess) {
                val controller = result.data()
                // Update channel mute status
                updateChannelMuteStatus(chatDomain.currentUser.channelMutes)

                // Update members
                _state.addSource(controller.members) { members ->
                    val channelData = controller.channelData.value!!
                    updateMembers(members, channelData.createdBy)
                }

                _state.addSource(controller.channelData) { channelData ->
                    _state.value = _state.value?.copy(channelName = channelData.name)
                }
            }
        }
    }

    fun onAction(action: Action) {
        when (action) {
            is Action.NameChanged -> changeGroupName(action.name)
            Action.MembersSeparatorClicked -> _state.value = _state.value!!.copy(shouldExpandMembers = true)
            is Action.MuteChannelClicked -> switchGroupMute(action.isEnabled)
            is Action.ChannelMutesUpdated -> updateChannelMuteStatus(action.channelMutes)
            Action.LeaveChannelClicked -> leaveChannel()
        }
    }

    private fun changeGroupName(name: String) {
        viewModelScope.launch {
            val result = channelClient.update(message = null, mapOf("name" to name)).await()
            if (result.isError) {
                // TODO: Handle error
            }
        }
    }

    private fun leaveChannel() {
        viewModelScope.launch {
            val result = chatDomain.useCases.hideChannel(cid, keepHistory = true).await()
            if (result.isSuccess) {
                _channelLeftState.value = true
            } else {
                // TODO: Handle error
            }
        }
    }

    private fun updateMembers(members: List<Member>, owner: User) {
        val currentState = _state.value!!
        val membersToShow = members.map {
            ChatMember(it, isOwner = it.getUserId() == owner.id)
        }
        _state.value =
            currentState.copy(
                members = membersToShow,
                shouldExpandMembers = currentState.shouldExpandMembers ?: false || members.size <= COLLAPSED_MEMBERS_COUNT,
                membersToShowCount = members.size - COLLAPSED_MEMBERS_COUNT
            )
    }

    private fun updateChannelMuteStatus(channelMutes: List<ChannelMute>) {
        _state.value = _state.value!!.copy(channelMuted = channelMutes.any { it.channel.cid == cid })
    }

    private fun switchGroupMute(isEnabled: Boolean) {
        viewModelScope.launch {
            val result = if (isEnabled) {
                channelClient.mute().await()
            } else {
                channelClient.unmute().await()
            }
            if (result.isError) {
                // Handle error in a better way
                _state.value = _state.value!!.copy(channelMuted = !isEnabled)
            }
        }
    }

    data class State(
        val members: List<ChatMember>,
        val channelName: String,
        val channelMuted: Boolean,
        val shouldExpandMembers: Boolean?,
        val membersToShowCount: Int,
    )

    sealed class Action {
        data class NameChanged(val name: String) : Action()
        object MembersSeparatorClicked : Action()
        data class MuteChannelClicked(val isEnabled: Boolean) : Action()
        data class ChannelMutesUpdated(val channelMutes: List<ChannelMute>) : Action()
        object LeaveChannelClicked : Action()
    }

    companion object {
        const val COLLAPSED_MEMBERS_COUNT = 5

        private val INITIAL_STATE = State(
            members = emptyList(),
            channelName = "",
            channelMuted = false,
            shouldExpandMembers = null,
            membersToShowCount = 0
        )
    }
}
