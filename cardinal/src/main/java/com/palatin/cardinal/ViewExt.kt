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

import android.view.View
import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Subscribes given flows that produces [Action] to [CardinalViewModel]
 */
suspend fun <Action : Any> CardinalViewModel<out Any, Action, out Any>.bindActions(
    lifecycleOwner: LifecycleOwner,
    vararg flows: Flow<Action>
) {
    flows.forEach {
        it.onEach {
            this.dispatch(it)
        }.launchIn(lifecycleOwner.lifecycleScope)
    }
}



suspend inline fun <Action : Any> View.clickAction(crossinline actionBuilder: () -> Action): Flow<Action> =
    callbackFlow {
        setOnClickListener {
            offer(actionBuilder())
        }
        awaitClose { setOnClickListener(null) }
    }

suspend inline fun <Action : Any> EditText.textChangesAction(
    crossinline actionBuilder: (
        text: CharSequence?,
        start: Int,
        before: Int,
        count: Int
    ) -> Action
): Flow<Action> = callbackFlow {
    val tw = doOnTextChanged { text, start, before, count ->
        offer(actionBuilder(text, start, before, count))
    }

    awaitClose { removeTextChangedListener(tw) }
}
