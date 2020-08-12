# Cardinal
Cardinal is an MVI library for Android (Kotlin). It based on Kotlin's asynchronous flow and hightly tied to Android lifecycle. 
Cardinal provides a minimum functionality to implement MVI pattern and doesn't force to use some strategy. 
It drews inspiration from [Orbit MVI](https://github.com/babylonhealth/orbit-mvi).
# Getting started
`implementation 'com.palatin:cardinal:0.1.0'`
## Define the data to communicate between UI and ViewModel
State represent the data which displayed on the screen. The good approach is to create it as data class with immutable fields for thread-safety.
```kotlin 
data class State(val count: Int = 0)
```
Action represents actions that influence on current state, the way view communicates with ViewModel, usually sealed class.
```kotlin
sealed class Action {
    data class AddAction(val number: Int) : Action()
    object ResetAction : Action()
}
```
SideEffect represents one time events that view should handle, e.g. navigating, showing message etc.
```kotlin
sealed class SideEffect {
    data class Toast(val message: String) : SideEffect()
}
```
Tying everything together:
```kotlin
class CounterVM : CardinalViewModel<CounterVM.State, CounterVM.Action, CounterVM.SideEffect>(State()) {

    init {
        actions()
            .ofType<Action.AddAction>()
            .unlimited()
            .reduceAction { state, addAction -> state.copy(state.count + addAction.number) }
            .sideEffect { action -> SideEffect.Toast(action.toString()) }
            .launchIn(viewModelScope)

        actions()
                .ofType<Action.ResetAction>()
                .unlimited()
                .reduceAction { state, resetAction -> state.copy(0) }
                .sideEffect { action -> SideEffect.Toast(action.toString()) }
                .launchIn(viewModelScope)

    }

    data class State(val count: Int = 0)

    sealed class Action {
        data class AddAction(val number: Int) : Action()
        object ResetAction : Action()
    }

    sealed class SideEffect {
        data class Toast(val message: String) : SideEffect()
    }
}
```
## Connect activity/fragment to viewmodel
```kotlin
class MainActivity : AppCompatActivity() {

    private val vm: CounterVM by viewModels()

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launchWhenCreated {

            vm.bindActions(this@MainActivity,
                binding.btnAdd.clickAction { CounterVM.Action.AddAction(1) },
                binding.btnReset.clickAction { CounterVM.Action.ResetAction }
            )

            vm.subscribeOnSideEffects(this) {
                when(it) {
                    is CounterVM.SideEffect.Toast -> 
                        Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        vm.subscribeOnState(this, Observer {
            binding.tvCount.text = "Count: ${it.count}"
        })


    }
}
```
For more complicated example see [sample](sample).
# License
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

```
Copyright 2020 Ihor Shamin

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
