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
            "Login screen created"
        )
    }

    override fun onStart() {
        super.onStart()

        val currentUser =
            firebaseAuth.currentUser

        if (currentUser != null) {

            /*
             * Do not log Firebase UID, email address,
             * display name or any other personal data.
             */
            Log.d(
                TAG,
                "Existing Firebase session detected"
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
                    "Google sign-in request failed: ${e.javaClass.simpleName}"
                )

                Toast.makeText(
                    this@LoginActivity,
                    getString(
                        R.string.login_google_failed
                    ),
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
                    "Google credential received"
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
                    "Google credential could not be processed: ${e.javaClass.simpleName}"
                )

                Toast.makeText(
                    this,
                    getString(
                        R.string.login_google_process
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }

        } else {

            showLoading(false)

            Log.e(
                TAG,
                "Unexpected Google credential type"
            )

            Toast.makeText(
                this,
                getString(
                    R.string.login_google_invalid
                ),
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

                    /*
                     * Keep authentication logging generic.
                     * Never print UID, email, display name,
                     * Google ID token or Firebase token.
                     */
                    Log.d(
                        TAG,
                        "Firebase authentication successful"
                    )

                    if (firebaseUser != null) {

                        syncFirebaseUserWithGymLeague(
                            firebaseUser
                        )

                    } else {

                        showLoading(false)

                        Log.e(
                            TAG,
                            "Firebase authentication completed without a user"
                        )

                        Toast.makeText(
                            this,
                            getString(
                                R.string.login_firebase_user
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } else {

                    showLoading(false)

                    val errorName =
                        task.exception
                            ?.javaClass
                            ?.simpleName
                            ?: "Unknown"

                    Log.e(
                        TAG,
                        "Firebase authentication failed: $errorName"
                    )

                    Toast.makeText(
                        this,
                        getString(
                            R.string.login_auth_failed
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun syncFirebaseUserWithGymLeague(
        firebaseUser: FirebaseUser
    ) {

        activityScope.launch {

            /*
             * The Firebase UID is used for the API lookup,
             * but is deliberately not written to Logcat.
             */
            Log.d(
                TAG,
                "Checking GymLeague account"
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
                            "Existing GymLeague account found"
                        )

                        saveUserAndContinue(
                            existingUser
                        )

                    } else {

                        Log.d(
                            TAG,
                            "No GymLeague account found. Creating account."
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
                        "GymLeague account lookup failed: ${error.javaClass.simpleName}"
                    )

                    Toast.makeText(
                        this@LoginActivity,
                        getString(
                            R.string.login_connect_failed
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    private suspend fun createGymLeagueUser(
        firebaseUser: FirebaseUser
    ) {

        val email =
            firebaseUser.email
                ?: ""

        val username =
            firebaseUser.displayName
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: email
                    .substringBefore("@")
                    .ifBlank {
                        getString(
                            R.string.generic_user
                        )
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

                /*
                 * Do not log the user's internal database ID.
                 */
                Log.d(
                    TAG,
                    "GymLeague account created successfully"
                )

                createDefaultSettingsForNewUser(
                    newUser
                )
            }
            .onFailure { error ->

                showLoading(false)

                Log.e(
                    TAG,
                    "GymLeague account creation failed: ${error.javaClass.simpleName}"
                )

                Toast.makeText(
                    this,
                    getString(
                        R.string.login_create_failed
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private suspend fun createDefaultSettingsForNewUser(
        newUser: User
    ) {

        Log.d(
            TAG,
            "Creating default account settings"
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
            settingsRepository
                .createSettings(
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

                /*
                 * SettingsFragment can create missing settings later,
                 * so failure here should not block login.
                 */
                Log.w(
                    TAG,
                    "Default settings creation failed: ${error.javaClass.simpleName}"
                )

                saveUserAndContinue(
                    newUser
                )
            }
    }

    private fun saveUserAndContinue(
        user: User
    ) {

        sessionManager
            .saveUserSession(
                userId =
                    user.userId,

                firebaseUid =
                    user.firebaseUid,

                username =
                    user.username,

                email =
                    user.email
            )

        /*
         * Session success is useful for debugging,
         * but the actual user ID is not logged.
         */
        Log.d(
            TAG,
            "GymLeague session saved successfully"
        )

        showLoading(false)

        Toast.makeText(
            this,
            getString(
                R.string.login_welcome,
                user.username
            ),
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