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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.take
import kotlin.coroutines.CoroutineContext


/**
 * Wraps flow with [Action] type and provides operators around MVI flow
 */
class ActionFlow<Action>(@PublishedApi internal val flow: Flow<Action>) {

    /**
     * Allows applying transformation directly to [Flow]
     */
    inline fun transform(transformer: Flow<Action>.() -> Flow<Action>) =
        ActionFlow(transformer(flow))

    /**
     * Terminal operator that calls [Flow.launchIn] method and return it's [Job]
     */
    fun launchIn(coroutineScope: CoroutineScope) = flow.launchIn(coroutineScope)

    /**
     * Applies [Flow.flowOn]
     */
    fun flowOn(coroutineContext: CoroutineContext) =
        ActionFlow(flow.flowOn(coroutineContext))

    /**
     * Drops actions until flow is ready to process
     */
    fun dropWhileBusy() = ActionFlow(flow.dropWhileBusy())

}


/**
 * Class that contains operators for instantiating [ActionFlow]
 */
class ActionFlowBuilder<Action : Any>(@PublishedApi internal val flow: Flow<Action>) {

    /**
     * Filters flow by [T] subtype of [Action]
     */
    inline fun <reified T : Action> ofType() = ActionCountBuilder(flow.ofType<T>())

    /**
     * Consumes any actions
     */
    fun any() = ActionCountBuilder(flow)


    inner class ActionCountBuilder<F : Action>(@PublishedApi internal val flow: Flow<F>) {

        /**
         * Unsubscribe after first consumed action
         */
        fun single(): ActionFlow<F> = count(1)

        /**
         * Takes first [number] actions and unsubscribe than
         */
        fun count(number: Int) = ActionFlow(flow.take(number))

        /**
         * Consumes all actions
         */
        fun unlimited(): ActionFlow<F> = ActionFlow(flow)
    }
}

