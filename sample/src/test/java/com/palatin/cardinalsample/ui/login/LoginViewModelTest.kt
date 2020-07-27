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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import com.palatin.cardinalsample.data.AccountRepository
import com.palatin.cardinalsample.data.Result
import com.palatin.cardinalsample.data.model.Account
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType


class LoginViewModelTest {

    lateinit var vm: LoginViewModel

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val reducer = spyk(loginReducer)
    private val observer = spyk<Observer<LoginViewModel.State>>()
    private val accountValidator = mockk<AccountValidator>(relaxed = true)
    private val accountRepository = mockk<AccountRepository>(relaxed = true)

    private val email: String = "test@mail.com"
    private val password: String = "test"


    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        every { accountRepository.getAccount() } returns null
        vm = LoginViewModel(accountRepository, accountValidator, reducer, Dispatchers.Unconfined)
        vm.stateLd.observeForever(observer)
    }

    @After
    fun tearDown() {
        vm.stateLd.removeObserver(observer)
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `initial state when repository doesn't provide account`() = runBlocking {
        assertTrue(vm.state == LoginViewModel.State())
    }

    @Test
    fun `initial state when repository provides account`() = runBlocking {
        every { accountRepository.getAccount() } returns Account(email, password)
        vm = LoginViewModel(accountRepository, accountValidator, reducer, Dispatchers.Unconfined)
        assertTrue(vm.state == LoginViewModel.State(email = email, password = password, isFormCorrect = true))
    }

    @Test
    fun `state records login fields on account change action`() = runBlocking {
        vm.dispatch(LoginViewModel.Action.AccountFormChanged.EmailChanged(email))
        vm.dispatch(LoginViewModel.Action.AccountFormChanged.PasswordChanged(password))
        assert(vm.state.email == email)
        assert(vm.state.password == password)
    }

    @Test
    fun `flow produce account AccountFormVerified`() = runBlocking {
        every { accountValidator.validateAccount(any(), any()) } returns false
        every { accountValidator.validateAccount(email, password) } returns true
        vm.dispatch(LoginViewModel.Action.AccountFormChanged.EmailChanged(email))
        vm.dispatch(LoginViewModel.Action.AccountFormChanged.PasswordChanged(password))
        verify(exactly = 1) { reducer(any(), LoginViewModel.Action.AccountFormState.AccountFormVerified) }
        assertTrue(vm.state == LoginViewModel.State(email = email, password = password, isLoading = false, isFormCorrect = true))
    }

    @Test
    fun `flow produce AccountFormIncorrect`() = runBlocking {
        every { accountValidator.validateAccount(any(), any()) } returns false
        vm.dispatch(LoginViewModel.Action.AccountFormChanged.EmailChanged(email))
        vm.dispatch(LoginViewModel.Action.AccountFormChanged.PasswordChanged(password))
        verify(exactly = 2) { reducer(any(), LoginViewModel.Action.AccountFormState.AccountFormIncorrect) }
        assertTrue(vm.state == LoginViewModel.State(email = email, password = password, isLoading = false, isFormCorrect = false))
    }

    @Test
    fun `flow changes state to loading while auth`() = runBlocking {
        coEvery { accountRepository.login(any(), any()) } returns Result.Error(Exception("error"))
        vm.dispatch(LoginViewModel.Action.OnLoginClicked)
        verifyOrder {
            observer.onChanged(LoginViewModel.State(isLoading = true))
            observer.onChanged(LoginViewModel.State(isLoading = false))
        }
    }

    @Test
    fun `flow produce SuccessLoginAction and Navigate when account repository returns account`() = runBlocking {
        val sideEffect = spyk<suspend ((LoginViewModel.SideEffect) -> Unit)>({
        })

        coEvery { accountRepository.login(any(), any()) } returns Result.Success(Account(email, password))
        vm.subscribeOnSideEffects(TestCoroutineScope(), sideEffect)
        vm.dispatch(LoginViewModel.Action.OnLoginClicked)
        verify(exactly = 1) { reducer(any(), LoginViewModel.Action.OnLoginClicked) }
        coVerify(exactly = 1) { accountRepository.login(any(), any()) }
        verify(exactly = 1) { reducer(any(), LoginViewModel.Action.LoginAction.SuccessLogin(Account(email, password))) }
        //using java reflect because of not supporting mockk suspend high order function https://github.com/mockk/mockk/issues/339
        coVerify(exactly = 1) { sideEffect::class.java.getMethod("invoke", Any::class.java, Any::class.java)
            .invoke(sideEffect, LoginViewModel.SideEffect.Navigate(Account(email, password)), any())}
    }

    @Test
    fun `flow produce FailureLogin and ShowSnack when account repository returns error`() = runBlocking {
        val sideEffect = spyk<suspend ((LoginViewModel.SideEffect) -> Unit)>({
        })
        coEvery { accountRepository.login(any(), any()) } returns Result.Error(Exception("test"))
        vm.subscribeOnSideEffects(TestCoroutineScope(), sideEffect)
        vm.dispatch(LoginViewModel.Action.OnLoginClicked)
        verify(exactly = 1) { reducer(any(), LoginViewModel.Action.OnLoginClicked) }
        coVerify(exactly = 1) { accountRepository.login(any(), any()) }
        verify(exactly = 1) { reducer(any(), LoginViewModel.Action.LoginAction.FailureLogin("test")) }
        //using java reflect because of not supporting mockk suspend high order function https://github.com/mockk/mockk/issues/339
        coVerify(exactly = 1) { sideEffect::class.java.getMethod("invoke", Any::class.java, Any::class.java)
            .invoke(sideEffect, LoginViewModel.SideEffect.ShowSnack("test"), any())}
    }







}

