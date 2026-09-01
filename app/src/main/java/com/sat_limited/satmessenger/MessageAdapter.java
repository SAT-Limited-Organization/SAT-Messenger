package com.sat_limited.satmessenger;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MessageAdapter
        extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private final List<Message> messages;
    private final String currentUserId;

    public MessageAdapter(
            List<Message> messages,
            String currentUserId
    ) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);

        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull MessageViewHolder holder,
            int position
    ) {
        Message message = messages.get(position);

        holder.messageText.setText(message.getText());

        LinearLayout.LayoutParams params =
                (LinearLayout.LayoutParams)
                        holder.messageText.getLayoutParams();

        if (currentUserId.equals(message.getSenderId())) {

            params.gravity = Gravity.END;
            holder.messageText.setGravity(Gravity.START);

        } else {

            params.gravity = Gravity.START;
            holder.messageText.setGravity(Gravity.START);
        }

        holder.messageText.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder
            extends RecyclerView.ViewHolder {

        TextView messageText;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            messageText =
                    itemView.findViewById(R.id.messageText);
        }
    }
}