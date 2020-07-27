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

import kotlinx.coroutines.flow.*

/**
 * Consume this [Flow] using a channelFlow with no buffer. Elements emitted from [this] flow
 * are offered to the underlying [channelFlow]. If the consumer is not currently suspended and
 * waiting for the next element, the element is dropped.
 *
 * @return a flow that only emits elements when the downstream [Flow.collect] is waiting for the next element
 */
fun <T> Flow<T>.dropWhileBusy(): Flow<T> = channelFlow {
    collect { offer(it) }
}.buffer(capacity = 0)


/**
 * Consumes only values by given [T] type
 */
inline fun <reified T> Flow<*>.ofType(): Flow<T> = flow<T> {
    collect {
        if (it is T)
            emit(it)
    }
}