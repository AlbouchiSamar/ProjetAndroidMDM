package com.hmdm.launcher.ui.Admin.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hmdm.launcher.R;
import com.hmdm.launcher.ui.Admin.SendMessageFragment;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MessageListAdapter extends RecyclerView.Adapter<MessageListAdapter.MessageViewHolder> {
    private List<SendMessageFragment.Message> messages;

    public MessageListAdapter(List<SendMessageFragment.Message> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        SendMessageFragment.Message message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void updateMessages(List<SendMessageFragment.Message> newMessages) {
        this.messages.clear();
        this.messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private TextView textDeviceNumber;
        private TextView textTimestamp;
        private TextView textMessage;
        private TextView textStatus;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textDeviceNumber = itemView.findViewById(R.id.text_device_number);
            textTimestamp = itemView.findViewById(R.id.text_timestamp);
            textMessage = itemView.findViewById(R.id.text_message);
            textStatus = itemView.findViewById(R.id.text_status);
        }

        public void bind(SendMessageFragment.Message message) {
            textDeviceNumber.setText(message.getDeviceNumber());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            textTimestamp.setText(sdf.format(message.getTimestamp()));
            textMessage.setText(message.getMessageText());
            textStatus.setText(message.getStatus() == 0 ? "Envoyé" : "Lu");
        }
    }
}