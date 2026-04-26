package com.fetch.auth.production;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fetch.auth.production.repository.AuthRepository;
import com.fetch.auth.production.repository.UserProfileRepository;
import com.google.firebase.auth.FirebaseUser;

public class CustomerCreateTaskActivity extends AppCompatActivity {

    private EditText etTaskTitle;
    private EditText etTaskDescription;
    private Button btnSubmitTask;

    private AuthRepository authRepository;
    private UserProfileRepository userProfileRepository;
    private boolean isCustomerAuthorized;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_create_task);

        authRepository = new AuthRepository();
        userProfileRepository = new UserProfileRepository();

        etTaskTitle = findViewById(R.id.etTaskTitle);
        etTaskDescription = findViewById(R.id.etTaskDescription);
        btnSubmitTask = findViewById(R.id.btnSubmitTask);
        btnSubmitTask.setEnabled(false);

        btnSubmitTask.setOnClickListener(v -> submitTask());

        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, R.string.error_user_not_authenticated, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        verifyCustomerRole(currentUser.getUid());
    }

    private void submitTask() {
        if (!isCustomerAuthorized) {
            Toast.makeText(this, R.string.error_customer_access_required, Toast.LENGTH_SHORT).show();
            return;
        }

        String title = etTaskTitle.getText().toString().trim();
        String description = etTaskDescription.getText().toString().trim();

        Intent intent = new Intent(this, MapTaskComposerActivity.class);
        intent.putExtra(MapTaskComposerActivity.EXTRA_PREFILL_TITLE, title);
        intent.putExtra(MapTaskComposerActivity.EXTRA_PREFILL_DESCRIPTION, description);
        startActivity(intent);
        Toast.makeText(this, R.string.task_auto_pricing_enabled, Toast.LENGTH_SHORT).show();
    }

    private void verifyCustomerRole(String uid) {
        userProfileRepository.getUserRole(uid, new UserProfileRepository.UserRoleCallback() {
            @Override
            public void onSuccess(String role) {
                isCustomerAuthorized = "customer".equalsIgnoreCase(role);
                if (isCustomerAuthorized) {
                    btnSubmitTask.setEnabled(true);
                    return;
                }

                Toast.makeText(CustomerCreateTaskActivity.this, R.string.error_customer_access_required, Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(CustomerCreateTaskActivity.this, R.string.error_role_check_failed, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }
}


