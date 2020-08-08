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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coVerify
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

@RunWith(BlockJUnit4ClassRunner::class)
class CardinalViewModelTest {

    lateinit var vm: CardinalViewModel<State, Action, Effects>

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        vm = spyk(CardinalViewModel(State()))
    }

    @Test
    fun `check initial state`() {
        assertEquals(State(), vm.stateLd.value)
    }

    @Test
    fun `dispatched action exists in channel`() = runBlocking {
        val actionFlow = vm.actions()
        vm.dispatch(Action.Test1Action)
        assertEquals(Action.Test1Action, actionFlow.flow.first())
    }

    @Test
    fun `test reduce action with state notifying`() = runBlocking {
        val observer = spyk<Observer<State>>()
        val reducer = spyk(object : Reducer<State, Action> {
            override fun invoke(p1: State, p2: Action): State {
                return State("test")
            }
        })
        vm.stateLd.observeForever(observer)

        val actionFlow = vm.actions()
        vm.dispatch(Action.Test1Action)
        vm.run {
            actionFlow.any().single().reduceAction( reducer).flow.launchIn(this@runBlocking)
                .join()
        }
        vm.dispose()
        verify { reducer.invoke(any(), Action.Test1Action) }
        verify { observer.onChanged(State(test = "test")) }

    }


    @Test
    fun `test trigger action`() = runBlocking {
        val testFlow1 = vm.actions()
        val testFlow2 = vm.actions().ofType<Action.Test2Action>().unlimited()

        vm.dispatch(Action.Test1Action)
        vm.run {
            testFlow1.ofType<Action.Test1Action>().single()
                .triggerAction { Action.Test2Action }.launchIn(this@runBlocking).join()
        }

        assertEquals(Action.Test2Action, testFlow2.flow.first())
    }

    @Test
    fun `test side effect`() = runBlocking {
        val effectSpy = spyk<suspend (Effects) -> Unit>()
        vm.subscribeOnSideEffects(this, effectSpy)
        val testFlow = vm.actions()
        vm.dispatch(Action.Test1Action)

        vm.run {
            testFlow.any().single().sideEffect { Effects.TestEffect }.launchIn(this@runBlocking)
                .join()
        }
        vm.dispose()
        coVerify { vm.applyEffect(Effects.TestEffect) }
        coVerify {
            effectSpy::class.java.getMethod("invoke", Any::class.java, Any::class.java)
                .invoke(effectSpy, Effects.TestEffect, any())
        }

    }

}

data class State(val test: String = "")

sealed class Action {
    object Test1Action : Action()
    object Test2Action : Action()
}

sealed class Effects {
    object TestEffect : Effects()
}