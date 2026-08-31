package com.sat_limited.satmessenger;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;

    private Button registerButton;
    private Button loginButton;
    private ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);

        registerButton = findViewById(R.id.registerButton);
        loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.progressBar);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        registerButton.setOnClickListener(v -> registerUser());

        loginButton.setOnClickListener(v -> {
            finish();
        });
    }

    private void registerUser() {

        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        if (TextUtils.isEmpty(name)) {
            nameInput.setError("Enter your name");
            nameInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email address");
            emailInput.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            passwordInput.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match");
            confirmPasswordInput.requestFocus();
            return;
        }

        setLoading(true);

        firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {
                        setLoading(false);

                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registration failed";

                        Toast.makeText(
                                RegisterActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    if (firebaseAuth.getCurrentUser() == null) {
                        setLoading(false);
                        Toast.makeText(
                                RegisterActivity.this,
                                "Account created, but user data was unavailable.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    String uid = firebaseAuth.getCurrentUser().getUid();

                    Map<String, Object> user = new HashMap<>();
                    user.put("displayName", name);
                    user.put("email", email);
                    user.put("photoUrl", "");
                    user.put("about", "Hey! I'm using SAT Messenger.");
                    user.put("online", true);
                    user.put("lastSeen", System.currentTimeMillis());

                    firestore
                            .collection("users")
                            .document(uid)
                            .set(user)
                            .addOnSuccessListener(unused -> {

                                setLoading(false);

                                Toast.makeText(
                                        RegisterActivity.this,
                                        "Account created successfully!",
                                        Toast.LENGTH_SHORT
                                ).show();

                                // Temporary destination.
                                // We'll replace this with the real Home screen.
                                Intent intent = new Intent(
                                        RegisterActivity.this,
                                        MainActivity.class
                                );

                                intent.addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK |
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                                );

                                startActivity(intent);
                            })
                            .addOnFailureListener(e -> {

                                setLoading(false);

                                Toast.makeText(
                                        RegisterActivity.this,
                                        "Account created, but profile setup failed: "
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                });
    }

    private void setLoading(boolean loading) {

        if (loading) {
            progressBar.setVisibility(View.VISIBLE);
            registerButton.setEnabled(false);
            loginButton.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            registerButton.setEnabled(true);
            loginButton.setEnabled(true);
        }
    }
}