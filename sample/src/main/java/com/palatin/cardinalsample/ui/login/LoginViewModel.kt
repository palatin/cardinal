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

package com.palatin.cardinalsample.ui.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.palatin.cardinal.*
import com.palatin.cardinalsample.data.AccountRepository
import com.palatin.cardinalsample.data.Result
import com.palatin.cardinalsample.data.model.Account
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

class LoginViewModel(private val accountRepository: AccountRepository,
                     private val accountValidator: AccountValidator,
                     private val reducer: Reducer<State, Action>,
                     private val ioDispatcher: CoroutineDispatcher
)
    : CardinalViewModel<LoginViewModel.State, LoginViewModel.Action, LoginViewModel.SideEffect>(

    with(accountRepository.getAccount()) {
        if (this != null)
            State.fromAccount(this)
        else
            State()
    }
) {
    init {
        bindActions()
    }

    private fun bindActions() {

        actions()
            .ofType<Action.AccountFormChanged>()
            .unlimited()
            .reduceAction(reducer)
            .transformAction {
                if(accountValidator.validateAccount(state.email, state.password)) Action.AccountFormState.AccountFormVerified else Action.AccountFormState.AccountFormIncorrect
            }
            .reduceAction( reducer)
            .launchIn(viewModelScope)

        actions()
            .ofType<Action.OnLoginClicked>()
            .unlimited()
            .dropWhileBusy()
            .transform { dropWhileBusy() }
            .reduceAction( reducer)
            .transformAction {
                when(val result = accountRepository.login(state.email, state.password)) {
                    is Result.Success -> Action.LoginAction.SuccessLogin(result.data)
                    is Result.Error -> Action.LoginAction.FailureLogin(result.exception.message ?: "error")
                }
            }
            .flowOn(ioDispatcher)
            .reduceAction( reducer)
            .sideEffect {
                when(it) {
                    is Action.LoginAction.SuccessLogin -> SideEffect.Navigate(it.account)
                    is Action.LoginAction.FailureLogin -> SideEffect.ShowSnack(it.error)
                }
            }.launchIn(viewModelScope)
    }




    sealed class Action {

        sealed class AccountFormChanged : Action() {
            data class EmailChanged(val email: String) : AccountFormChanged()
            data class PasswordChanged(val password: String) : AccountFormChanged()
        }

        sealed class AccountFormState : Action() {
            object AccountFormVerified : AccountFormState()
            object AccountFormIncorrect : AccountFormState()
        }

        sealed class LoginAction : Action() {
            data class SuccessLogin(val account: Account) : LoginAction()
            data class FailureLogin(val error: String) : LoginAction()
        }

        object OnLoginClicked : Action()
    }

    sealed class SideEffect {
        data class ShowSnack(val message: String) : SideEffect()
        data class Navigate(val account: Account) : SideEffect()
    }

    data class State(val email: String = "", val password: String = "", val isLoading: Boolean = false, val isFormCorrect: Boolean = false) {

        companion object {
            fun fromAccount(account: Account) = State(email = account.email, password = account.password, isFormCorrect = true)
        }
    }

    object LoginViewModelFactory : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return LoginViewModel(AccountRepository(), object : AccountValidator {}, loginReducer, Dispatchers.IO) as T
        }
    }
}

interface AccountValidator {

    fun validateAccount(email: String, password: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches() && password.length >= 6
    }
}

val loginReducer: Reducer<LoginViewModel.State, LoginViewModel.Action> =  { state, action ->

    when(action) {
        is LoginViewModel.Action.AccountFormChanged.EmailChanged -> state.copy(email = action.email)
        is LoginViewModel.Action.AccountFormChanged.PasswordChanged -> state.copy(password = action.password)
        is LoginViewModel.Action.LoginAction -> state.copy(isLoading = false)
        LoginViewModel.Action.AccountFormState.AccountFormVerified -> state.copy(isFormCorrect = true)
        LoginViewModel.Action.AccountFormState.AccountFormIncorrect -> state.copy(isFormCorrect = false)
        LoginViewModel.Action.OnLoginClicked -> state.copy(isLoading = true)
    }
}

