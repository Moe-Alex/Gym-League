package com.gymleague.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.gymleague.app.models.CreateUserRequest
import com.gymleague.app.models.User
import com.gymleague.app.models.UserSettings
import com.gymleague.app.repository.SettingsRepository
import com.gymleague.app.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "GymLeagueAuth"
    }

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    private lateinit var userRepository: UserRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var sessionManager: SessionManager

    private lateinit var btnGoogleSignIn: Button
    private lateinit var progressBar: ProgressBar

    private val activityScope =
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.Main
        )

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_login
        )

        firebaseAuth =
            FirebaseAuth.getInstance()

        credentialManager =
            CredentialManager.create(this)

        userRepository =
            UserRepository()

        settingsRepository =
            SettingsRepository()

        sessionManager =
            SessionManager(this)

        btnGoogleSignIn =
            findViewById(
                R.id.btnGoogleSignIn
            )

        progressBar =
            findViewById(
                R.id.progressBar
            )

        btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        Log.d(
            TAG,
            "LoginActivity created"
        )
    }

    override fun onStart() {
        super.onStart()

        val currentUser =
            firebaseAuth.currentUser

        if (currentUser != null) {

            Log.d(
                TAG,
                "Existing Firebase user found: ${currentUser.uid}"
            )

            showLoading(true)

            syncFirebaseUserWithGymLeague(
                currentUser
            )
        }
    }

    private fun signInWithGoogle() {

        Log.d(
            TAG,
            "Google sign-in started"
        )

        showLoading(true)

        val googleIdOption =
            GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(
                    getString(
                        R.string.default_web_client_id
                    )
                )
                .setAutoSelectEnabled(false)
                .build()

        val request =
            GetCredentialRequest.Builder()
                .addCredentialOption(
                    googleIdOption
                )
                .build()

        activityScope.launch {

            try {

                val result =
                    credentialManager.getCredential(
                        context = this@LoginActivity,
                        request = request
                    )

                handleCredential(
                    result.credential
                )

            } catch (
                e: GetCredentialException
            ) {

                showLoading(false)

                Log.e(
                    TAG,
                    "Google sign-in request failed",
                    e
                )

                Toast.makeText(
                    this@LoginActivity,
                    getString(R.string.login_google_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun handleCredential(
        credential: Credential
    ) {

        if (
            credential is CustomCredential &&
            credential.type ==
            GoogleIdTokenCredential
                .TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {

            try {

                val googleCredential =
                    GoogleIdTokenCredential
                        .createFrom(
                            credential.data
                        )

                Log.d(
                    TAG,
                    "Google ID token received"
                )

                authenticateWithFirebase(
                    googleCredential.idToken
                )

            } catch (
                e: GoogleIdTokenParsingException
            ) {

                showLoading(false)

                Log.e(
                    TAG,
                    "Google ID token could not be parsed",
                    e
                )

                Toast.makeText(
                    this,
                    getString(R.string.login_google_process),
                    Toast.LENGTH_LONG
                ).show()
            }

        } else {

            showLoading(false)

            Log.e(
                TAG,
                "Returned credential was not a Google ID token"
            )

            Toast.makeText(
                this,
                getString(R.string.login_google_invalid),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun authenticateWithFirebase(
        googleIdToken: String
    ) {

        val firebaseCredential =
            GoogleAuthProvider.getCredential(
                googleIdToken,
                null
            )

        firebaseAuth
            .signInWithCredential(
                firebaseCredential
            )
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {

                    val firebaseUser =
                        firebaseAuth.currentUser

                    Log.d(
                        TAG,
                        "Firebase authentication successful"
                    )

                    Log.d(
                        TAG,
                        "Firebase UID: ${firebaseUser?.uid}"
                    )

                    Log.d(
                        TAG,
                        "Email: ${firebaseUser?.email}"
                    )

                    Log.d(
                        TAG,
                        "Display name: ${firebaseUser?.displayName}"
                    )

                    if (firebaseUser != null) {

                        syncFirebaseUserWithGymLeague(
                            firebaseUser
                        )

                    } else {

                        showLoading(false)

                        Toast.makeText(
                            this,
                            getString(R.string.login_firebase_user),
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } else {

                    showLoading(false)

                    Log.e(
                        TAG,
                        "Firebase authentication failed",
                        task.exception
                    )

                    Toast.makeText(
                        this,
                        getString(R.string.login_auth_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun syncFirebaseUserWithGymLeague(
        firebaseUser: FirebaseUser
    ) {

        activityScope.launch {

            Log.d(
                TAG,
                "Checking GymLeague API for Firebase UID"
            )

            val lookupResult =
                userRepository
                    .getUserByFirebaseUid(
                        firebaseUser.uid
                    )

            lookupResult
                .onSuccess { existingUser ->

                    if (existingUser != null) {

                        Log.d(
                            TAG,
                            "Existing GymLeague user found. UserId: ${existingUser.userId}"
                        )

                        saveUserAndContinue(
                            existingUser
                        )

                    } else {

                        Log.d(
                            TAG,
                            "No GymLeague user found. Creating new user."
                        )

                        createGymLeagueUser(
                            firebaseUser
                        )
                    }
                }
                .onFailure { error ->

                    showLoading(false)

                    Log.e(
                        TAG,
                        "Failed to check GymLeague user",
                        error
                    )

                    Toast.makeText(
                        this@LoginActivity,
                        getString(R.string.login_connect_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    private suspend fun createGymLeagueUser(
        firebaseUser: FirebaseUser
    ) {

        val email =
            firebaseUser.email ?: ""

        val username =
            firebaseUser.displayName
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: email
                    .substringBefore("@")
                    .ifBlank {
                        getString(R.string.generic_user)
                    }

        val request =
            CreateUserRequest(
                firebaseUid =
                    firebaseUser.uid,

                username =
                    username,

                email =
                    email,

                profileImageUrl =
                    firebaseUser.photoUrl
                        ?.toString(),

                xp = 0,

                level = 1
            )

        val createResult =
            userRepository.createUser(
                request
            )

        createResult
            .onSuccess { newUser ->

                Log.d(
                    TAG,
                    "New GymLeague user created. UserId: ${newUser.userId}"
                )

                createDefaultSettingsForNewUser(
                    newUser
                )
            }
            .onFailure { error ->

                showLoading(false)

                Log.e(
                    TAG,
                    "Failed to create GymLeague user",
                    error
                )

                Toast.makeText(
                    this,
                    getString(R.string.login_create_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private suspend fun createDefaultSettingsForNewUser(
        newUser: User
    ) {

        Log.d(
            TAG,
            "Creating default settings for UserId: ${newUser.userId}"
        )

        val defaultSettings =
            UserSettings(
                userSettingsId = 0,
                userId = newUser.userId,
                notificationsEnabled = true,
                appearanceMode = "system",
                language = "English"
            )

        val settingsResult =
            settingsRepository.createSettings(
                defaultSettings
            )

        settingsResult
            .onSuccess {

                Log.d(
                    TAG,
                    "Default settings created successfully"
                )

                saveUserAndContinue(
                    newUser
                )
            }
            .onFailure { error ->

                Log.e(
                    TAG,
                    "Default settings could not be created",
                    error
                )

                /*
                 * We still allow the user into the app.
                 * SettingsFragment can create missing
                 * settings later if necessary.
                 */
                saveUserAndContinue(
                    newUser
                )
            }
    }

    private fun saveUserAndContinue(
        user: User
    ) {

        sessionManager.saveUserSession(
            userId =
                user.userId,

            firebaseUid =
                user.firebaseUid,

            username =
                user.username,

            email =
                user.email
        )

        Log.d(
            TAG,
            "GymLeague session saved. UserId: ${user.userId}"
        )

        showLoading(false)

        Toast.makeText(
            this,
            getString(R.string.login_welcome, user.username),
            Toast.LENGTH_SHORT
        ).show()

        openMainActivity()
    }

    private fun showLoading(
        loading: Boolean
    ) {

        progressBar.visibility =
            if (loading) {
                View.VISIBLE
            } else {
                View.GONE
            }

        btnGoogleSignIn.isEnabled =
            !loading
    }

    private fun openMainActivity() {

        val intent =
            Intent(
                this,
                MainActivity::class.java
            )

        startActivity(intent)

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()

        activityScope.cancel()
    }
}