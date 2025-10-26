package io.github.alexmaryin.followmymus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecureTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.alexmaryin.followmymus.core.forError
import io.github.alexmaryin.followmymus.core.forSuccess
import io.github.alexmaryin.followmymus.sessionManager.domain.SessionManager
import io.github.alexmaryin.followmymus.sessionManager.domain.model.Credentials
import io.github.alexmaryin.followmymus.sessionManager.domain.model.getNickname
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.auth.event.AuthEvent
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.broadcastFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(SupabaseExperimental::class, ExperimentalUuidApi::class)
@Composable
fun App(sessionManager: SessionManager = koinInject()) {

    MaterialTheme {
        val nicknameField = rememberTextFieldState()
        val passwordField = rememberTextFieldState()
        val scope = rememberCoroutineScope()
        val snackBarState = SnackbarHostState()

        Scaffold(
            snackbarHost = { SnackbarHost(snackBarState) },
            contentWindowInsets = WindowInsets.safeContent
        ) { innerPadding ->

            val sessionStatus = sessionManager.sessionStatus()
                .collectAsStateWithLifecycle(SessionStatus.Initializing)


            LaunchedEffect(Unit) {

                sessionManager.sessionStatus().collect {
                    println("SESSION STATUS CHANGED:\n")
                    println(when (it) {
                        is SessionStatus.Authenticated -> "Session: ${it.session}\nSource: ${it.source}"
                        SessionStatus.Initializing -> "Initializing"
                        is SessionStatus.NotAuthenticated -> "Not authenticated: user logoff - ${it.isSignOut}"
                        is SessionStatus.RefreshFailure -> "Refresh failure"
                    })
                }

                sessionManager.sessionEvents().collect {
                    println("SESSION EVENT:\n")
                    println(when(it) {
                        is AuthEvent.OtpError -> "One time pass error - ${it.errorCode}: ${it.errorDescription} .errorDescription}"
                        is AuthEvent.RefreshFailure -> "Refresh failure - ${it.cause}"
                    })
                }
            }

            var userInfo by remember { mutableStateOf("") }

            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OutlinedTextField(
                    state = nicknameField,
                    modifier = Modifier.padding(12.dp),
                    label = { Text("nickname") },
                    supportingText = { Text("no need to use real name") },
                    lineLimits = TextFieldLineLimits.SingleLine
                )
                SecureTextField(
                    state = passwordField,
                    modifier = Modifier.padding(12.dp),
                    label = { Text("password") },
                    supportingText = { Text("At least 6 lowercase, uppercase letters and digits") }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (sessionStatus != SessionStatus.Initializing) {
                        Button(
                            onClick = {
                                scope.launch {
                                    userInfo = ""
                                    val result = sessionManager.signUp(
                                        Credentials(
                                            nickname = nicknameField.text.toString(),
                                            password = passwordField.text.toString()
                                        )
                                    )
                                    result.forSuccess { userInfo = it.toString() }
                                    result.forError { _, message ->
                                        userInfo = message ?: "Unknown error"
                                    }
                                }
                            },
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text("Sign up")
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    userInfo = ""
                                    val result = sessionManager.signIn(
                                        Credentials(
                                            nickname = nicknameField.text.toString(),
                                            password = passwordField.text.toString()
                                        )
                                    )
                                    result.forError { _, message ->
                                        userInfo = message ?: "Unknown error"
                                    }
                                }
                            },
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text("Log in")
                        }
                        Button(
                            onClick = {
                                userInfo = ""
                                scope.launch { sessionManager.signOut() }
                            },
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text("Log out")
                        }
                    } else {
                        CircularProgressIndicator(Modifier.padding(16.dp))
                    }
                }

                HorizontalDivider(Modifier.padding(6.dp))

                val msg = when (val status = sessionStatus.value) {
                    is SessionStatus.Authenticated -> "Authenticated as ${status.session.getNickname()}."
                    is SessionStatus.NotAuthenticated if status.isSignOut -> "Signed out!"
                    is SessionStatus.NotAuthenticated if !status.isSignOut -> "Not authenticated!"
                    is SessionStatus.RefreshFailure -> "Session expired and could not be refreshed!"
                    else -> null
                }
                Text(
                    "Session status: $msg",
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Text(
                    "User info: $userInfo",
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                QRCodeBlock()
            }

//            val transferId = Uuid.random().toString()
//            val channel = koinInject<RealtimeChannel> { parametersOf(transferId) }
//
//            LaunchedEffect(Unit) {
//                channel.subscribe(true)
//                val broadcast = channel.broadcastFlow<String>(event = "transfer_request")
//                broadcast.collect {
//                    val session = sessionManager.currentSession()
//                    session.forSuccess {
//                        channel.broadcast("session_payload", buildJsonObject {
//                            put("accessToken", it.accessToken)
//                            put("refreshToken", it.refreshToken)
//                            put("expiresIn", it.expiresIn)
//                            put("tokenType", it.tokenType)
//                        })
//                        channel.unsubscribe()
//                    }
//                }
//            }
        }
    }
}

@Composable expect fun QRCodeBlock(modifier: Modifier = Modifier)