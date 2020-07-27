/*
* Copyright 2020 Ihor Shamin
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.palatin.cardinal

import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*


typealias Reducer<State, Action> = (State, Action) -> State

/**
 * Android viewmodel that represents store in MVI pattern
 * [State] - class that hold current screen state and used by views, usually data class with immutable fields because of copy operator
 * [Action] - represents actions that influence on current state, the way view communicates with ViewModel, usually sealed class
 * [SideEffect] - represents one time events that view should handle, e.g. navigating, showing message etc.
 * @param initialState - object of [State] type that will be stored and passed to [stateLd]
 */
open class CardinalViewModel<State : Any, Action : Any, SideEffect : Any>(initialState: State) :
    ViewModel() {

    var state: State = initialState
        private set


    private val _stateLd = MutableLiveData<State>(state)
    val stateLd: LiveData<State> = _stateLd
    private val effectChannel = BroadcastChannel<SideEffect>(1)
    private val actionChannel = BroadcastChannel<Action>(Channel.BUFFERED)




    /**
     * Subscribes on [actionChannel] and returns [ActionFlowBuilder]
     * **Note** returned value can be used only once for subscription
     */
    fun actions(): ActionFlowBuilder<Action> {
        return ActionFlowBuilder(actionChannel.openSubscription().consumeAsFlow())
    }


    /**
     * Sends an action to channel
     */
    suspend fun dispatch(action: Action) {
        if (!actionChannel.isClosedForSend)
            actionChannel.send(action)
    }

    /**
     * Subscribes [observer] on [stateLd] with given [lifecycleOwner]
     */
    fun subscribeOnState(lifecycleOwner: LifecycleOwner, observer: Observer<State>) {
        stateLd.observe(lifecycleOwner, observer)
    }

    /**
     * Opens subscription on [effectChannel] and consumes each [SideEffect] to [action] block
     */
    fun subscribeOnSideEffects(scope: CoroutineScope, action: suspend (SideEffect) -> Unit) {
        effectChannel.openSubscription().consumeAsFlow().onEach(action).launchIn(scope)
    }


    @PublishedApi
    @JvmSynthetic
    internal fun setState(state: State) {
        this.state = state
    }

    @PublishedApi
    @JvmSynthetic
    internal fun applyState(state: State) {
        this.state = state
        _stateLd.postValue(state)
    }


    @PublishedApi
    @JvmSynthetic
    internal suspend fun applyEffect(effect: SideEffect) {
        if (!effectChannel.isClosedForSend)
            effectChannel.send(effect)
    }


    /**
     * Closes channels
     */
    fun dispose() {
        actionChannel.close()
        effectChannel.close()
    }


    override fun onCleared() {
        super.onCleared()
        dispose()
    }

    /**
     * Invokes [dispatch] with the action from [actionProducer]
     * **Note** - that can causes an infinite loop on current [ActionFlow], especially
     * when using [ActionFlowBuilder.any] with [ActionFlowBuilder.ActionCountBuilder.unlimited]
     */
    inline fun <T : Action> ActionFlow<T>.triggerAction(crossinline actionProducer: suspend (T) -> (Action)) =
        transform {
            flow {
                collect {
                    dispatch(actionProducer(it))
                    emit(it)
                }
            }
        }

    /**
     * Changes flow on the [F] action type
     */
    inline fun <T : Action, F : Action> ActionFlow<T>.transformAction(crossinline transformer: suspend (T) -> (F)) =
        ActionFlow<F>(flow.map { transformer.invoke(it) })

    /**
     * Updates current [state] with value produced by [reducer].
     * Commonly used on [ActionFlow] instantiation to handle the first [Action] and after [triggerAction]
     * @param reducer - produces new state by current [state] and [T] flow's action
     */
    inline fun <T : Action> ActionFlow<T>.reduceAction(
        crossinline reducer: Reducer<State, T>
    ) = transform {
        flow<T> {
            collect {
                applyState(reducer(state, it))
                emit(it)
            }
        }
    }

    /**
     * Sends [SideEffect] produced by [effectProducer] to [effectChannel]
     */
    inline fun <T : Action> ActionFlow<T>.sideEffect(crossinline effectProducer: (T) -> (SideEffect)) =
        transform {
            flow {
                collect {
                    applyEffect(effectProducer(it))
                    emit(it)
                }
            }
        }

}




