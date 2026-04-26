package com.fetch.auth.production;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.fetch.auth.production.repository.AuthRepository;
import com.fetch.auth.production.repository.UserProfileRepository;
import com.fetch.auth.production.model.UserProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CompleteProfileActivity extends AppCompatActivity {

    public static final String EXTRA_NAME = "extra_name";
    public static final String EXTRA_EMAIL = "extra_email";

    private EditText etFullName;
    private EditText etEmail;
    private EditText etPhone;
    private RadioGroup rgRole;
    private LinearLayout layoutRiderFields;
    private Spinner spinnerVehicleType;
    private EditText etLicenseNumber;
    private LinearLayout layoutCustomerFields;
    private EditText etDefaultAddress;
    private Button btnSaveProfile;

    private AuthRepository authRepository;
    private UserProfileRepository userProfileRepository;
    private String prefilledName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_profile);

        authRepository = new AuthRepository();
        userProfileRepository = new UserProfileRepository();

        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        rgRole = findViewById(R.id.rgRole);
        layoutRiderFields = findViewById(R.id.layoutRiderFields);
        spinnerVehicleType = findViewById(R.id.spinnerVehicleType);
        etLicenseNumber = findViewById(R.id.etLicenseNumber);
        layoutCustomerFields = findViewById(R.id.layoutCustomerFields);
        etDefaultAddress = findViewById(R.id.etDefaultAddress);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        setupVehicleSpinner();
        bindPrefilledValues(currentUser);
        setupRoleToggle();

        btnSaveProfile.setOnClickListener(v -> saveProfile(currentUser));

        findViewById(R.id.btnGovernmentId).setOnClickListener(v ->
                Toast.makeText(this, R.string.upload_placeholder_message, Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnSelfieVerification).setOnClickListener(v ->
                Toast.makeText(this, R.string.upload_placeholder_message, Toast.LENGTH_SHORT).show());
    }

    private void setupVehicleSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.vehicle_types,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVehicleType.setAdapter(adapter);
    }

    private void bindPrefilledValues(FirebaseUser user) {
        String passedName = getIntent().getStringExtra(EXTRA_NAME);
        String passedEmail = getIntent().getStringExtra(EXTRA_EMAIL);

        prefilledName = !TextUtils.isEmpty(passedName) ? passedName : user.getDisplayName();
        String email = !TextUtils.isEmpty(passedEmail) ? passedEmail : user.getEmail();

        if (!TextUtils.isEmpty(prefilledName)) {
            // Name already captured during sign-up/provider auth; don't ask again.
            etFullName.setText(prefilledName);
            etFullName.setEnabled(false);
            etFullName.setFocusable(false);
            etFullName.setClickable(false);
            etFullName.setVisibility(View.GONE);
        } else {
            etFullName.setEnabled(true);
            etFullName.setFocusableInTouchMode(true);
            etFullName.setVisibility(View.VISIBLE);
        }

        if (!TextUtils.isEmpty(email)) {
            etEmail.setText(email);
        }
    }

    private void setupRoleToggle() {
        rgRole.clearCheck();
        layoutRiderFields.setVisibility(View.GONE);
        layoutCustomerFields.setVisibility(View.GONE);

        rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbRider) {
                layoutRiderFields.setVisibility(View.VISIBLE);
                layoutCustomerFields.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbCustomer) {
                layoutRiderFields.setVisibility(View.GONE);
                layoutCustomerFields.setVisibility(View.VISIBLE);
            } else {
                layoutRiderFields.setVisibility(View.GONE);
                layoutCustomerFields.setVisibility(View.GONE);
            }
        });
    }

    private void saveProfile(FirebaseUser user) {
        String inputName = etFullName.getText().toString().trim();
        String name = !TextUtils.isEmpty(prefilledName) ? prefilledName : inputName;
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etFullName.setVisibility(View.VISIBLE);
            etFullName.setEnabled(true);
            etFullName.setFocusableInTouchMode(true);
            etFullName.setError(getString(R.string.error_name_required));
            etFullName.requestFocus();
            return;
        }

        if (!TextUtils.isEmpty(email) && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.error_valid_email));
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            etPhone.setError(getString(R.string.error_phone_required));
            etPhone.requestFocus();
            return;
        }

        int checkedRole = rgRole.getCheckedRadioButtonId();
        if (checkedRole == View.NO_ID) {
            Toast.makeText(this, R.string.error_role_required, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isRider = checkedRole == R.id.rbRider;
        String role = isRider ? UserProfile.ROLE_RIDER : UserProfile.ROLE_CUSTOMER;

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("name", name);
        userData.put("email", email);
        userData.put("phone", phone);
        userData.put("role", role);
        userData.put("profileImage", null);
        userData.put("createdAt", FieldValue.serverTimestamp());

        if (isRider) {
            String vehicleType = spinnerVehicleType.getSelectedItem() != null
                    ? spinnerVehicleType.getSelectedItem().toString()
                    : "";
            String licenseNumber = etLicenseNumber.getText().toString().trim();

            boolean bypassRiderRequirements = BuildConfig.ALLOW_RIDER_TEST_BYPASS;
            boolean didAutoFillForTest = false;
            if (bypassRiderRequirements) {
                if (TextUtils.isEmpty(vehicleType) || vehicleType.equals(getString(R.string.vehicle_type_prompt))) {
                    vehicleType = "Motorcycle";
                    didAutoFillForTest = true;
                }
                if (TextUtils.isEmpty(licenseNumber)) {
                    licenseNumber = "TEST-LICENSE-0001";
                    didAutoFillForTest = true;
                }
            }

            if (didAutoFillForTest) {
                Toast.makeText(this, R.string.rider_test_bypass_notice, Toast.LENGTH_SHORT).show();
            }

            if (TextUtils.isEmpty(vehicleType) || vehicleType.equals(getString(R.string.vehicle_type_prompt))) {
                Toast.makeText(this, R.string.error_vehicle_required, Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(licenseNumber)) {
                etLicenseNumber.setError(getString(R.string.error_license_required));
                etLicenseNumber.requestFocus();
                return;
            }

            Map<String, Object> riderDetails = new HashMap<>();
            riderDetails.put("vehicleType", vehicleType);
            riderDetails.put("licenseNumber", licenseNumber);
            riderDetails.put("verified", false);
            userData.put("riderDetails", riderDetails);

            Map<String, Object> requirements = new HashMap<>();
            requirements.put("governmentIdUrl", "");
            requirements.put("selfieUrl", "");

            Map<String, Object> application = new HashMap<>();
            application.put("status", "pending");
            application.put("requirements", requirements);
            application.put("submittedAt", FieldValue.serverTimestamp());
            userData.put("application", application);

            Map<String, Object> customerDetails = new HashMap<>();
            customerDetails.put("savedLocations", new ArrayList<>());
            userData.put("customerDetails", customerDetails);
        } else {
            String address = etDefaultAddress.getText().toString().trim();
            ArrayList<String> savedLocations = new ArrayList<>();
            if (!TextUtils.isEmpty(address)) {
                savedLocations.add(address);
            }

            Map<String, Object> customerDetails = new HashMap<>();
            customerDetails.put("savedLocations", savedLocations);
            userData.put("customerDetails", customerDetails);

            Map<String, Object> riderDetails = new HashMap<>();
            riderDetails.put("vehicleType", "");
            riderDetails.put("licenseNumber", "");
            riderDetails.put("verified", false);
            userData.put("riderDetails", riderDetails);

            Map<String, Object> application = new HashMap<>();
            application.put("status", "not_applicable");
            userData.put("application", application);
        }

        btnSaveProfile.setEnabled(false);

        userProfileRepository.saveUserProfile(user.getUid(), userData, new UserProfileRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(CompleteProfileActivity.this, R.string.profile_saved_success, Toast.LENGTH_SHORT).show();
                Intent nextIntent;
                if (UserProfile.ROLE_RIDER.equalsIgnoreCase(role)) {
                    nextIntent = new Intent(CompleteProfileActivity.this, RiderMainActivity.class);
                } else {
                    nextIntent = new Intent(CompleteProfileActivity.this, CustomerMainActivity.class);
                }
                startActivity(nextIntent);
                finish();
            }

            @Override
            public void onError(Exception error) {
                btnSaveProfile.setEnabled(true);
                String message = error != null ? error.getMessage() : "Unknown error";
                if (message.toUpperCase().contains("PERMISSION_DENIED")) {
                    Toast.makeText(CompleteProfileActivity.this, R.string.profile_saved_permission_denied, Toast.LENGTH_LONG).show();
                    return;
                }
                Toast.makeText(CompleteProfileActivity.this, getString(R.string.profile_saved_error, message), Toast.LENGTH_LONG).show();
            }
        });
    }
}
