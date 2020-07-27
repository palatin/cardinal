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

import android.app.Activity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.palatin.cardinal.*

import com.palatin.cardinalsample.R
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private val vm: LoginViewModel by viewModels { LoginViewModel.LoginViewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        vm.subscribeOnState(this, Observer {
            btn_login.isEnabled = it.isFormCorrect
            if(et_username.text.toString() != it.email)
                et_username.setText(it.email)
            if(et_password.text.toString() != it.password)
                et_password.setText(it.password)
            loading.visibility = if(it.isLoading) View.VISIBLE else View.GONE
        })

        lifecycleScope.launch {
            vm.subscribeOnSideEffects(this) {
                when(it) {
                    is LoginViewModel.SideEffect.ShowSnack -> Snackbar.make(container, it.message, Snackbar.LENGTH_SHORT).show()
                }
            }
            vm.bindActions(this@LoginActivity,
                et_username.textChangesAction { text, start, before, count -> LoginViewModel.Action.AccountFormChanged.EmailChanged(text?.toString() ?: "" ) },
                et_password.textChangesAction { text, start, before, count -> LoginViewModel.Action.AccountFormChanged.PasswordChanged(text?.toString() ?: "" ) },
                btn_login.clickAction { LoginViewModel.Action.OnLoginClicked }
            )
        }


    }

}

