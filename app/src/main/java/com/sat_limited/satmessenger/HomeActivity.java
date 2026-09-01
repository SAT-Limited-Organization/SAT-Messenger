package com.sat_limited.satmessenger;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private TextView welcomeText;
    private Button logoutButton;
    private Button findPeopleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        welcomeText = findViewById(R.id.welcomeText);
        logoutButton = findViewById(R.id.logoutButton);
        findPeopleButton = findViewById(R.id.findPeopleButton);

        if (firebaseAuth.getCurrentUser() == null) {
            openLogin();
            return;
        }

        String uid = firebaseAuth.getCurrentUser().getUid();

        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("displayName");

                        if (name != null && !name.isEmpty()) {
                            welcomeText.setText("Welcome, " + name + "!");
                        } else {
                            welcomeText.setText("Welcome to SAT Messenger!");
                        }
                    } else {
                        welcomeText.setText("Welcome to SAT Messenger!");
                    }
                })
                .addOnFailureListener(e ->
                        welcomeText.setText("Welcome to SAT Messenger!")
                );

        // Open Find People
        findPeopleButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, UsersActivity.class);
            startActivity(intent);
        });

        // Logout
        logoutButton.setOnClickListener(v -> logout());
    }

    private void logout() {
        firebaseAuth.signOut();

        Toast.makeText(
                HomeActivity.this,
                "You have been logged out.",
                Toast.LENGTH_SHORT
        ).show();

        openLogin();
    }

    private void openLogin() {
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}