package com.fetch.auth.production;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AdminAddRiderActivity extends AppCompatActivity {

    private EditText etRiderName;
    private EditText etRiderAge;
    private EditText etRiderBirthplace;
    private EditText etRiderLicense;
    private Button btnSubmitAddRider;

    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_rider);

        firestore = FirebaseFirestore.getInstance();

        etRiderName = findViewById(R.id.etRiderName);
        etRiderAge = findViewById(R.id.etRiderAge);
        etRiderBirthplace = findViewById(R.id.etRiderBirthplace);
        etRiderLicense = findViewById(R.id.etRiderLicense);
        btnSubmitAddRider = findViewById(R.id.btnSubmitAddRider);

        btnSubmitAddRider.setOnClickListener(v -> {
            String name = etRiderName.getText().toString().trim();
            String age = etRiderAge.getText().toString().trim();
            String birthplace = etRiderBirthplace.getText().toString().trim();
            String license = etRiderLicense.getText().toString().trim();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(age) ||
                    TextUtils.isEmpty(birthplace) || TextUtils.isEmpty(license)) {
                Toast.makeText(AdminAddRiderActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            showEmailPasswordDialog(name, age, birthplace, license);
        });
    }

    private void showEmailPasswordDialog(String name, String age, String birthplace, String license) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Credentials");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_register_rider, null);
        final EditText etEmail = viewInflated.findViewById(R.id.etEmail);
        final EditText etPassword = viewInflated.findViewById(R.id.etPassword);

        // Pre-fill email with something based on name
        String suggestedEmail = name.replaceAll("\\s+", "").toLowerCase() + "@rider.com";
        etEmail.setText(suggestedEmail);

        builder.setView(viewInflated);

        builder.setPositiveButton("Register", null); // Handled manually below
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        
        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    etEmail.setError("Valid Email required");
                    return;
                }

                if (TextUtils.isEmpty(password) || password.length() < 6) {
                    etPassword.setError("Password must be at least 6 characters long");
                    Toast.makeText(AdminAddRiderActivity.this, "Password rejected. Too short (min 6 chars)", Toast.LENGTH_SHORT).show();
                    return;
                }

                dialog.dismiss();
                registerRiderWithSecondaryApp(name, age, birthplace, license, email, password);
            });
        });

        dialog.show();
    }

    private void registerRiderWithSecondaryApp(String name, String age, String birthplace, String license, String email, String password) {
        btnSubmitAddRider.setEnabled(false);
        Toast.makeText(this, "Registering rider, please wait...", Toast.LENGTH_SHORT).show();

        FirebaseApp secondaryApp = null;
        try {
            FirebaseOptions options = FirebaseApp.getInstance().getOptions();
            secondaryApp = FirebaseApp.initializeApp(getApplicationContext(), options, "SecondaryApp");
        } catch (Exception e) {
            secondaryApp = FirebaseApp.getInstance("SecondaryApp");
        }

        FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(secondaryApp);
        
        secondaryAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser newUser = authResult.getUser();
                    if (newUser != null) {
                        saveRiderToFirestore(newUser.getUid(), name, age, birthplace, license, email);
                    }
                    secondaryAuth.signOut();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdminAddRiderActivity.this, "Auth Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSubmitAddRider.setEnabled(true);
                });
    }

    private void saveRiderToFirestore(String uid, String name, String age, String birthplace, String license, String email) {
        Map<String, Object> riderDetails = new HashMap<>();
        riderDetails.put("age", age);
        riderDetails.put("birthplace", birthplace);
        riderDetails.put("licenseNumber", license);
        riderDetails.put("verified", true);

        Map<String, Object> application = new HashMap<>();
        application.put("status", "approved");

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid", uid);
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("role", "rider");
        userMap.put("riderDetails", riderDetails);
        userMap.put("application", application);
        userMap.put("verificationStatus", "verified");

        firestore.collection("users").document(uid).set(userMap)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(AdminAddRiderActivity.this, "Rider fully registered!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdminAddRiderActivity.this, "Firestore Save Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSubmitAddRider.setEnabled(true);
                });
    }
}
