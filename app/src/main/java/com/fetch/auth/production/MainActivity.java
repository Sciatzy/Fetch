package com.fetch.auth.production;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.fetch.auth.production.model.UserProfile;
import com.fetch.auth.production.repository.AuthRepository;
import com.fetch.auth.production.repository.UserProfileRepository;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Fetch_Auth";

    private EditText etEmail, etPassword, etName;
    private TextView tvTitle, tvSubtitle, tvToggleAction, tvForgotPassword;
    private Button btnMainAction;
    private View btnGoogle, btnFacebook;
    private View nameContainer, emailContainer, passwordContainer, dividerContainer, bottomLinkContainer;

    private AuthRepository authRepository;
    private UserProfileRepository userProfileRepository;
    private GoogleSignInClient mGoogleSignInClient;
    private CallbackManager mCallbackManager;
    private boolean isLoginMode = true;
    private boolean isAuthInProgress;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "Google Sign In Result Code: " + result.getResultCode());
                Intent data = result.getData();
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    if (account != null) {
                        Log.d(TAG, "Google account retrieved: " + account.getEmail());
                        firebaseAuthWithGoogle(account);
                    } else {
                        setAuthInProgress(false);
                    }
                } catch (ApiException e) {
                    setAuthInProgress(false);
                    Log.e(TAG, "Google sign in failed. Code: " + e.getStatusCode(), e);
                    String errorMsg = buildGoogleSignInErrorMessage(e.getStatusCode());
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnMainAction = findViewById(R.id.btnLogin);
        tvToggleAction = findViewById(R.id.tvToggleAction);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnFacebook = findViewById(R.id.btnFacebook);
        nameContainer = findViewById(R.id.nameContainer);
        emailContainer = findViewById(R.id.emailContainer);
        passwordContainer = findViewById(R.id.passwordContainer);
        dividerContainer = findViewById(R.id.dividerContainer);
        bottomLinkContainer = findViewById(R.id.bottomLinkContainer);

        authRepository = new AuthRepository();
        userProfileRepository = new UserProfileRepository();

        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser != null) {
            checkUserProfileAndRoute(currentUser, null, null);
            return;
        }

        printKeyHash();
        logGoogleConfigDiagnostics();

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Initialize Google Sign-In
        String webClientId = resolveDefaultWebClientId("dummy_client_id_for_compilation");

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mCallbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                firebaseAuthWithFacebook(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                setAuthInProgress(false);
                Log.d(TAG, "Facebook login canceled");
            }

            @Override
            public void onError(FacebookException error) {
                setAuthInProgress(false);
                Log.e(TAG, "Facebook login error", error);
                String message = error != null ? error.getMessage() : getString(R.string.error_unknown);
                Toast.makeText(MainActivity.this, getString(R.string.auth_error_fb, message), Toast.LENGTH_SHORT).show();
            }
        });

        tvToggleAction.setOnClickListener(v -> toggleMode());
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        btnMainAction.setOnClickListener(v -> {
            if (isLoginMode) loginUser(); else registerUser();
        });

        btnGoogle.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                Log.d(TAG, "Google button clicked");
                signInWithGoogle();
            } else {
                Toast.makeText(this, R.string.auth_error_no_internet, Toast.LENGTH_SHORT).show();
            }
        });
        
        btnFacebook.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                signInWithFacebook();
            } else {
                Toast.makeText(this, R.string.auth_error_no_internet, Toast.LENGTH_SHORT).show();
            }
        });

        // Read intent to set default mode
        if (getIntent() != null && getIntent().hasExtra("IS_LOGIN_MODE")) {
            boolean modeShouldBeLogin = getIntent().getBooleanExtra("IS_LOGIN_MODE", true);
            if (isLoginMode != modeShouldBeLogin) {
                toggleMode();
            }
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void printKeyHash() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error printing key hash", e);
        }
    }

    private void logGoogleConfigDiagnostics() {
        String webClientId = resolveDefaultWebClientId("missing_web_client_id");
        Log.d(TAG, "Google config package=" + getPackageName());
        Log.d(TAG, "Google config default_web_client_id=" + webClientId);
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                byte[] certBytes = signature.toByteArray();
                Log.d(TAG, "Signing SHA-1=" + digestHex("SHA-1", certBytes));
                Log.d(TAG, "Signing SHA-256=" + digestHex("SHA-256", certBytes));
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to log signing fingerprints", e);
        }
    }

    private String resolveDefaultWebClientId(String fallbackValue) {
        int webClientIdRes = getResources().getIdentifier("default_web_client_id", "string", getPackageName());
        return webClientIdRes != 0 ? getString(webClientIdRes) : fallbackValue;
    }

    private String digestHex(String algorithm, byte[] bytes) throws Exception {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] digest = md.digest(bytes);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            if (i > 0) sb.append(':');
            sb.append(String.format(Locale.US, "%02X", digest[i]));
        }
        return sb.toString();
    }

    private String buildGoogleSignInErrorMessage(int statusCode) {
        if (statusCode == CommonStatusCodes.DEVELOPER_ERROR) {
            return "Google Sign-In developer error (10). Add this app SHA-1/SHA-256 to Firebase, download updated google-services.json, then rebuild.";
        }
        if (statusCode == CommonStatusCodes.CANCELED) {
            return "Google Sign-In canceled.";
        }
        if (statusCode == CommonStatusCodes.NETWORK_ERROR) {
            return "Google Sign-In failed: network error.";
        }
        if (statusCode == 12500) {
            return "Google Sign-In failed: internal error (12500).";
        }
        return "Google Sign-In failed (code: " + statusCode + ")";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mCallbackManager != null) mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void toggleMode() {
        if (isAuthInProgress) {
            return;
        }

        isLoginMode = !isLoginMode;
        tvTitle.setText(isLoginMode ? R.string.auth_login_title : R.string.auth_register_title);
        tvSubtitle.setText(isLoginMode ? R.string.auth_login_subtitle : R.string.auth_register_subtitle);
        btnMainAction.setText(isLoginMode ? R.string.auth_btn_login : R.string.auth_btn_signup);
        tvToggleAction.setText(isLoginMode ? R.string.auth_toggle_signup : R.string.auth_toggle_login);

        nameContainer.setVisibility(isLoginMode ? View.GONE : View.VISIBLE);

        emailContainer.setVisibility(View.VISIBLE);
        passwordContainer.setVisibility(View.VISIBLE);
        dividerContainer.setVisibility(View.VISIBLE);
        btnGoogle.setVisibility(View.VISIBLE);
        btnFacebook.setVisibility(View.VISIBLE);
        bottomLinkContainer.setVisibility(View.VISIBLE);
        tvForgotPassword.setVisibility(isLoginMode ? View.VISIBLE : View.GONE);
    }

    private void signInWithGoogle() {
        if (isAuthInProgress) {
            return;
        }

        setAuthInProgress(true);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void signInWithFacebook() {
        if (isAuthInProgress) {
            return;
        }

        setAuthInProgress(true);
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "email"));
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        authRepository.signInWithCredential(credential, new AuthRepository.AuthResultCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                setAuthInProgress(false);
                if (user != null) {
                    checkUserProfileAndRoute(user, account.getDisplayName(), account.getEmail());
                }
            }

            @Override
            public void onError(Exception error) {
                setAuthInProgress(false);
                Log.e(TAG, "Firebase Auth with Google failed", error);
                String message = error != null ? error.getMessage() : "Unknown error";
                Toast.makeText(MainActivity.this, getString(R.string.auth_error_google_auth, message), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void firebaseAuthWithFacebook(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        authRepository.signInWithCredential(credential, new AuthRepository.AuthResultCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                setAuthInProgress(false);
                if (user != null) {
                    checkUserProfileAndRoute(user, null, null);
                }
            }

            @Override
            public void onError(Exception error) {
                setAuthInProgress(false);
                Log.e(TAG, "Firebase Auth Failed", error);
                String message = error != null ? error.getMessage() : "Unknown error";
                Toast.makeText(MainActivity.this, "Firebase Auth Failed: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkUserProfileAndRoute(FirebaseUser user, String providedName, String providedEmail) {
        if (user == null) return;

        Log.d(TAG, "Checking Firestore for UID: " + user.getUid());

        userProfileRepository.isProfileComplete(user.getUid(), new UserProfileRepository.ProfileStatusCallback() {
            @Override
            public void onSuccess(boolean isComplete) {
                if (isComplete) {
                    onLoginSuccess(user);
                } else {
                    goToCompleteProfile(user, providedName, providedEmail);
                }
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Firestore check failed", error);
                goToCompleteProfile(user, providedName, providedEmail);
            }
        });
    }

    private void goToCompleteProfile(FirebaseUser user, String providedName, String providedEmail) {
        String fallbackName = !TextUtils.isEmpty(providedName) ? providedName : user.getDisplayName();
        String fallbackEmail = !TextUtils.isEmpty(providedEmail) ? providedEmail : user.getEmail();

        Intent intent = new Intent(this, CompleteProfileActivity.class);
        intent.putExtra(CompleteProfileActivity.EXTRA_NAME, fallbackName);
        intent.putExtra(CompleteProfileActivity.EXTRA_EMAIL, fallbackEmail);
        startActivity(intent);
        finish();
    }

    private void registerUser() {
        if (isAuthInProgress) {
            return;
        }

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError(getString(R.string.error_name_required));
            etName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.error_valid_email));
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError(getString(R.string.auth_error_password_required));
            etPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError(getString(R.string.auth_error_password_short));
            etPassword.requestFocus();
            return;
        }

        setAuthInProgress(true);

        authRepository.createUserWithEmail(email, password, new AuthRepository.AuthResultCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                setAuthInProgress(false);
                if (user != null) {
                    checkUserProfileAndRoute(user, name, email);
                }
            }

            @Override
            public void onError(Exception error) {
                setAuthInProgress(false);
                String message = error != null ? error.getMessage() : "Unknown error";
                Toast.makeText(MainActivity.this, getString(R.string.auth_error_prefix, message), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loginUser() {
        if (isAuthInProgress) {
            return;
        }

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.error_valid_email));
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError(getString(R.string.auth_error_password_required));
            etPassword.requestFocus();
            return;
        }

        setAuthInProgress(true);

        authRepository.signInWithEmail(email, password, new AuthRepository.AuthResultCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                setAuthInProgress(false);
                checkUserProfileAndRoute(user, null, email);
            }

            @Override
            public void onError(Exception error) {
                setAuthInProgress(false);
                String message = error != null ? error.getMessage() : "Unknown error";
                Toast.makeText(MainActivity.this, getString(R.string.auth_error_login_failed, message), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showForgotPasswordDialog() {
        final EditText emailInput = new EditText(this);
        emailInput.setHint(R.string.forgot_password_hint);
        emailInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setText(etEmail.getText().toString().trim());

        LinearLayout container = new LinearLayout(this);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, 0);
        container.addView(emailInput);

        new AlertDialog.Builder(this)
                .setTitle(R.string.forgot_password_title)
                .setView(container)
                .setPositiveButton(R.string.forgot_password_send, (dialog, which) -> {
                    String email = emailInput.getText().toString().trim();
                    if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(this, R.string.forgot_password_email_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    handlePasswordResetForEmail(email);
                })
                .setNegativeButton(R.string.forgot_password_cancel, null)
                .show();
    }

    private void handlePasswordResetForEmail(String email) {
        authRepository.fetchSignInMethodsForEmail(email, new AuthRepository.SignInMethodsCallback() {
            @Override
            public void onSuccess(List<String> methods) {
                if (methods != null && methods.contains(EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD)) {
                    sendPasswordResetEmail(email);
                    return;
                }

                if (methods != null && (methods.contains("google.com") || methods.contains("facebook.com"))) {
                    Toast.makeText(MainActivity.this, R.string.forgot_password_social_only, Toast.LENGTH_LONG).show();
                    return;
                }

                // Keep behavior user-friendly and avoid leaking account existence.
                sendPasswordResetEmail(email);
            }

            @Override
            public void onError(Exception error) {
                String message = error != null ? error.getMessage() : "Unknown error";
                Toast.makeText(MainActivity.this, getString(R.string.forgot_password_error, message), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void sendPasswordResetEmail(String email) {
        authRepository.sendPasswordResetEmail(email, new AuthRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, R.string.forgot_password_email_sent, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(Exception error) {
                String message = error != null ? error.getMessage() : "Unknown error";
                Toast.makeText(MainActivity.this, getString(R.string.forgot_password_error, message), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onLoginSuccess(FirebaseUser user) {
        if (user == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        userProfileRepository.getUserRole(user.getUid(), new UserProfileRepository.UserRoleCallback() {
            @Override
            public void onSuccess(String role) {
                String normalizedRole = UserProfile.normalizeRole(role);
                Intent intent;

                if (UserProfile.ROLE_ADMIN.equals(normalizedRole)) {
                    intent = new Intent(MainActivity.this, AdminDashboardActivity.class);
                } else if (UserProfile.ROLE_RIDER.equals(normalizedRole)) {
                    intent = new Intent(MainActivity.this, RiderMainActivity.class);
                } else {
                    intent = new Intent(MainActivity.this, CustomerMainActivity.class);
                }

                startActivity(intent);
                finish();
            }

            @Override
            public void onError(Exception error) {
                // Keep fallback resilient if role lookup fails.
                startActivity(new Intent(MainActivity.this, HomeActivity.class));
                finish();
            }
        });
    }

    private void setAuthInProgress(boolean inProgress) {
        isAuthInProgress = inProgress;
        btnMainAction.setEnabled(!inProgress);
        btnGoogle.setEnabled(!inProgress);
        btnFacebook.setEnabled(!inProgress);
        tvToggleAction.setEnabled(!inProgress);
        tvForgotPassword.setEnabled(!inProgress);
        etEmail.setEnabled(!inProgress);
        etPassword.setEnabled(!inProgress);
        etName.setEnabled(!inProgress && !isLoginMode);

        if (inProgress) {
            btnMainAction.setText(R.string.auth_btn_loading);
            btnMainAction.setAlpha(0.7f);
        } else {
            btnMainAction.setText(isLoginMode ? R.string.auth_btn_login : R.string.auth_btn_signup);
            btnMainAction.setAlpha(1f);
        }
    }
}
