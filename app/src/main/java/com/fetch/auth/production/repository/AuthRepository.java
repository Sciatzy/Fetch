package com.fetch.auth.production.repository;

import android.content.Context;

import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

import java.util.List;

public class AuthRepository {

    private static final String PROVIDER_GOOGLE = "google.com";
    private static final String PROVIDER_FACEBOOK = "facebook.com";

    private final FirebaseAuth auth;

    public AuthRepository() {
        this.auth = FirebaseAuth.getInstance();
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public void createUserWithEmail(String email, String password, AuthResultCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> callback.onSuccess(auth.getCurrentUser()))
                .addOnFailureListener(callback::onError);
    }

    public void signInWithEmail(String email, String password, AuthResultCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> callback.onSuccess(auth.getCurrentUser()))
                .addOnFailureListener(callback::onError);
    }

    public void signInWithCredential(AuthCredential credential, AuthResultCallback callback) {
        auth.signInWithCredential(credential)
                .addOnSuccessListener(result -> callback.onSuccess(auth.getCurrentUser()))
                .addOnFailureListener(callback::onError);
    }

    public void fetchSignInMethodsForEmail(String email, SignInMethodsCallback callback) {
        auth.fetchSignInMethodsForEmail(email)
                .addOnSuccessListener(result -> callback.onSuccess(result.getSignInMethods()))
                .addOnFailureListener(callback::onError);
    }

    public void sendPasswordResetEmail(String email, OperationCallback callback) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void signOut() {
        auth.signOut();
    }

    public void signOutAllProviders(Context context, OperationCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        boolean hadGoogleSession = hasProvider(currentUser, PROVIDER_GOOGLE);
        boolean hadFacebookSession = hasProvider(currentUser, PROVIDER_FACEBOOK);

        auth.signOut();

        if (hadFacebookSession) {
            LoginManager.getInstance().logOut();
        }

        if (!hadGoogleSession) {
            callback.onSuccess();
            return;
        }

        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(context.getApplicationContext(), signInOptions);

        googleSignInClient.signOut()
                .addOnSuccessListener(unused -> googleSignInClient.revokeAccess()
                        .addOnSuccessListener(unusedRevoke -> callback.onSuccess())
                        .addOnFailureListener(callback::onError))
                .addOnFailureListener(callback::onError);
    }

    private boolean hasProvider(FirebaseUser user, String providerId) {
        if (user == null) {
            return false;
        }
        for (UserInfo info : user.getProviderData()) {
            if (providerId.equals(info.getProviderId())) {
                return true;
            }
        }
        return false;
    }

    public interface AuthResultCallback {
        void onSuccess(FirebaseUser user);
        void onError(Exception error);
    }

    public interface SignInMethodsCallback {
        void onSuccess(List<String> methods);
        void onError(Exception error);
    }

    public interface OperationCallback {
        void onSuccess();
        void onError(Exception error);
    }
}
