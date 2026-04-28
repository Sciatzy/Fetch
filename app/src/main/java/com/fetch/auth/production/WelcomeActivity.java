package com.fetch.auth.production;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;



public class WelcomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Button btnNewUser = findViewById(R.id.btnNewUser);
        Button btnLogin = findViewById(R.id.btnLogin);

        btnNewUser.setOnClickListener(v -> {
            // New user goes to onboarding sequence
            startActivity(new Intent(WelcomeActivity.this, OnboardingActivity.class));
        });

        btnLogin.setOnClickListener(v -> {
            // Existing user goes straight to Login mode in MainActivity
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            intent.putExtra("IS_LOGIN_MODE", true);
            startActivity(intent);
        });
    }
}
