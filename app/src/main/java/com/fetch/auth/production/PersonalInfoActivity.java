package com.fetch.auth.production;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fetch.auth.production.repository.AuthRepository;
import com.fetch.auth.production.repository.UserProfileRepository;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Locale;

public class PersonalInfoActivity extends AppCompatActivity {

    private AuthRepository authRepository;
    private UserProfileRepository userProfileRepository;

    private TextView tvInfoName, tvInfoEmail, tvInfoPhone, tvInfoRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);

        authRepository = new AuthRepository();
        userProfileRepository = new UserProfileRepository();

        tvInfoName = findViewById(R.id.tvInfoName);
        tvInfoEmail = findViewById(R.id.tvInfoEmail);
        tvInfoPhone = findViewById(R.id.tvInfoPhone);
        tvInfoRole = findViewById(R.id.tvInfoRole);

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.error_not_authenticated, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvInfoEmail.setText(user.getEmail() != null ? user.getEmail() : getString(R.string.not_available));
        
        userProfileRepository.getUserProfile(user.getUid(), new UserProfileRepository.UserProfileDataCallback() {
            @Override
            public void onSuccess(DocumentSnapshot document) {
                String name = document.getString("name");
                String phone = document.getString("phone");
                String role = document.getString("role");

                tvInfoName.setText(name != null ? name : getString(R.string.not_available));
                tvInfoPhone.setText(phone != null ? phone : getString(R.string.not_available));
                
                if (role != null && !role.isEmpty()) {
                    String formattedRole = role.substring(0, 1).toUpperCase(Locale.US) + role.substring(1);
                    tvInfoRole.setText(formattedRole);
                } else {
                    tvInfoRole.setText(getString(R.string.not_available));
                }
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(PersonalInfoActivity.this, R.string.error_load_info, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

