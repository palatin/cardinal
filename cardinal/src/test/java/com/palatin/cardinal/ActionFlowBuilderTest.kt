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

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionFlowBuilderTest {

    @Test
    fun `test ofType operator`() = runBlocking {
        val flow = flow<Any> {
            emit("test")
            emit(true)
            emit(2)
        }
        val list = ActionFlowBuilder<Any>(flow).ofType<Boolean>().unlimited().flow.toList()
        assertTrue(list.size == 1 && list.first())
    }

    @Test
    fun `test single operator`() = runBlocking {
        val flow = flow<Any> {
            emit("test")
            emit(true)
            emit(2)
        }
        val list = ActionFlowBuilder<Any>(flow).any().single().flow.toList()
        assertTrue(list.size == 1 && list.first() == "test")
    }

    @Test
    fun `test count operator`() = runBlocking {
        val flow = flow<Any> {
            emit("test")
            emit(true)
            emit(2)
            repeat(100) {
                emit(it)
            }
        }
        val list = ActionFlowBuilder<Any>(flow).any().count(32).flow.toList()
        assertTrue(list.size == 32)
    }

    @Test
    fun `test transform operator`() = runBlocking {
        val flow = flow<String> {
            emit("test")
        }
        val list = ActionFlowBuilder<String>(flow).any().unlimited()
            .transform { map { it + "1" } }.flow.toList()
        assertTrue(list.first() == "test1")
    }

}