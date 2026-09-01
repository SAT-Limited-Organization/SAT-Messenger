package com.sat_limited.satmessenger;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    private final List<User> users;
    private final OnUserClickListener listener;

    public UserAdapter(
            List<User> users,
            OnUserClickListener listener
    ) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);

        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull UserViewHolder holder,
            int position
    ) {
        User user = users.get(position);

        holder.userName.setText(user.getDisplayName());
        holder.userEmail.setText(user.getEmail());

        String name = user.getDisplayName();

        if (name != null && !name.isEmpty()) {
            holder.userAvatar.setText(
                    name.substring(0, 1).toUpperCase()
            );
        } else {
            holder.userAvatar.setText("?");
        }

        holder.itemView.setOnClickListener(v ->
                listener.onUserClick(user)
        );
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {

        TextView userAvatar;
        TextView userName;
        TextView userEmail;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);

            userAvatar = itemView.findViewById(R.id.userAvatar);
            userName = itemView.findViewById(R.id.userName);
            userEmail = itemView.findViewById(R.id.userEmail);
        }
    }
}