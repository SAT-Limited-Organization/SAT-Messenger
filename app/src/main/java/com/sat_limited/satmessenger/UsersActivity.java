package com.sat_limited.satmessenger;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UsersActivity extends AppCompatActivity {

    private EditText searchInput;
    private RecyclerView usersRecyclerView;

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;

    private final List<User> allUsers = new ArrayList<>();
    private final List<User> visibleUsers = new ArrayList<>();

    private UserAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        searchInput = findViewById(R.id.searchInput);
        usersRecyclerView = findViewById(R.id.usersRecyclerView);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        adapter = new UserAdapter(
                visibleUsers,
                this::openChat
        );

        usersRecyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        usersRecyclerView.setAdapter(adapter);

        loadUsers();

        searchInput.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadUsers() {

        if (firebaseAuth.getCurrentUser() == null) {
            finish();
            return;
        }

        String currentUid =
                firebaseAuth.getCurrentUser().getUid();

        firestore.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    allUsers.clear();

                    for (var document : querySnapshot.getDocuments()) {

                        User user = document.toObject(User.class);

                        if (user == null) {
                            continue;
                        }

                        user.setUid(document.getId());

                        if (!currentUid.equals(user.getUid())) {
                            allUsers.add(user);
                        }
                    }

                    filterUsers(searchInput.getText().toString());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Unable to load users: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void filterUsers(String search) {

        String query = search
                .trim()
                .toLowerCase(Locale.ROOT);

        visibleUsers.clear();

        for (User user : allUsers) {

            String name = user.getDisplayName() == null
                    ? ""
                    : user.getDisplayName().toLowerCase(Locale.ROOT);

            String email = user.getEmail() == null
                    ? ""
                    : user.getEmail().toLowerCase(Locale.ROOT);

            if (name.contains(query) || email.contains(query)) {
                visibleUsers.add(user);
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void openChat(User user) {

        Intent intent = new Intent(
                UsersActivity.this,
                ChatActivity.class
        );

        intent.putExtra("userUid", user.getUid());
        intent.putExtra("userName", user.getDisplayName());

        startActivity(intent);
    }
}