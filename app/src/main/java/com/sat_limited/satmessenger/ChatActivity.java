package com.sat_limited.satmessenger;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private TextView chatUserName;
    private EditText messageInput;
    private Button sendButton;
    private RecyclerView messagesRecyclerView;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private final List<Message> messages = new ArrayList<>();

    private MessageAdapter adapter;

    private String currentUserId;
    private String otherUserId;
    private String otherUserName;
    private String chatId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatUserName = findViewById(R.id.chatUserName);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        messagesRecyclerView =
                findViewById(R.id.messagesRecyclerView);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        if (firebaseAuth.getCurrentUser() == null) {
            finish();
            return;
        }

        currentUserId =
                firebaseAuth.getCurrentUser().getUid();

        otherUserId =
                getIntent().getStringExtra("userUid");

        otherUserName =
                getIntent().getStringExtra("userName");

        if (otherUserId == null || otherUserId.isEmpty()) {
            Toast.makeText(
                    this,
                    "User unavailable.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        chatUserName.setText(
                otherUserName != null
                        ? otherUserName
                        : "Chat"
        );

        chatId = createChatId(
                currentUserId,
                otherUserId
        );

        adapter = new MessageAdapter(
                messages,
                currentUserId
        );

        messagesRecyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        messagesRecyclerView.setAdapter(adapter);

        createChatIfNeeded();
        listenForMessages();

        sendButton.setOnClickListener(v ->
                sendMessage()
        );
    }

    private String createChatId(
            String firstUser,
            String secondUser
    ) {
        if (firstUser.compareTo(secondUser) < 0) {
            return firstUser + "_" + secondUser;
        }

        return secondUser + "_" + firstUser;
    }

    private void createChatIfNeeded() {

        firestore.collection("chats")
                .document(chatId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {

                        java.util.Map<String, Object> chat =
                                new java.util.HashMap<>();

                        chat.put(
                                "participants",
                                Arrays.asList(
                                        currentUserId,
                                        otherUserId
                                )
                        );

                        chat.put("lastMessage", "");
                        chat.put(
                                "updatedAt",
                                System.currentTimeMillis()
                        );

                        firestore.collection("chats")
                                .document(chatId)
                                .set(chat);
                    }
                });
    }

    private void listenForMessages() {

        firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy(
                        "timestamp",
                        Query.Direction.ASCENDING
                )
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {
                        Toast.makeText(
                                this,
                                "Unable to load messages: "
                                        + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    if (snapshot == null) {
                        return;
                    }

                    messages.clear();

                    for (DocumentSnapshot document :
                            snapshot.getDocuments()) {

                        Message message =
                                document.toObject(Message.class);

                        if (message != null) {

                            if (message.getMessageId() == null) {
                                message.setMessageId(
                                        document.getId()
                                );
                            }

                            messages.add(message);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (!messages.isEmpty()) {
                        messagesRecyclerView.scrollToPosition(
                                messages.size() - 1
                        );
                    }
                });
    }

    private void sendMessage() {

        String text =
                messageInput.getText()
                        .toString()
                        .trim();

        if (TextUtils.isEmpty(text)) {
            return;
        }

        String messageId =
                firestore.collection("chats")
                        .document(chatId)
                        .collection("messages")
                        .document()
                        .getId();

        long timestamp =
                System.currentTimeMillis();

        Message message = new Message(
                messageId,
                currentUserId,
                text,
                timestamp
        );

        firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .set(message)
                .addOnSuccessListener(unused -> {

                    messageInput.setText("");

                    firestore.collection("chats")
                            .document(chatId)
                            .update(
                                    "lastMessage",
                                    text,
                                    "updatedAt",
                                    timestamp
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Message failed: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }
}