package com.example.btlandr;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

    private List<Event> eventList;
    private OnEventActionListener listener;

    // Interface callback
    public interface OnEventActionListener {
        void onDelete(String eventId);
        void onDetail(Event event);
    }

    public EventAdapter(List<Event> eventList, OnEventActionListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    // 🧩 1. Tạo ViewHolder (ánh xạ layout)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new ViewHolder(view);
    }

    // 🧩 2. Gán dữ liệu cho từng item
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = eventList.get(position);

        holder.txtTitle.setText(event.getTitle());
        holder.txtNote.setText(event.getNote());
        holder.txtCategory.setText(event.getCategory());

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(event.getId());
        });

        holder.btnDetail.setOnClickListener(v -> {
            if (listener != null) listener.onDetail(event);
        });
    }

    // 🧩 3. Trả về số lượng phần tử
    @Override
    public int getItemCount() {
        return eventList != null ? eventList.size() : 0;
    }

    // 🧩 4. ViewHolder ánh xạ với item_event.xml
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtNote, txtCategory;
        Button btnDelete, btnDetail;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtNote = itemView.findViewById(R.id.txtNote);
            txtCategory = itemView.findViewById(R.id.txtCategory);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnDetail = itemView.findViewById(R.id.btnDetail);
        }
    }
}
