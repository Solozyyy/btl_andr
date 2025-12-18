package com.example.btlandr;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import android.widget.CheckBox;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {
    // Hàm cập nhật UI khi check hoặc bỏ check
    private void updateEventDoneUI(ViewHolder holder, boolean isChecked) {
        if (isChecked) {
            holder.txtTitle.setPaintFlags(holder.txtTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            holder.txtTitle.setTextColor(Color.parseColor("#388E3C")); // Xanh lá đậm
            holder.itemView.setBackgroundColor(Color.parseColor("#E8F5E9")); // Nền xanh lá nhạt
        } else {
            holder.txtTitle.setPaintFlags(holder.txtTitle.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
            holder.txtTitle.setTextColor(Color.parseColor("#212121")); // Màu mặc định
            holder.itemView.setBackgroundColor(Color.WHITE);
        }
    }

    private List<Event> eventList;
    private OnEventActionListener listener;

    private String category = "";

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
        long now = System.currentTimeMillis();

        // Set title và category
        holder.txtTitle.setText(event.getTitle());
        if (category.isEmpty()) {
            holder.txtCategory.setText(event.getCategory());
        } else {
            holder.txtCategory.setText(category);
        }

        // ⭐ Hiển thị icon important
        if (event.isImportant()) {
            holder.iconImportant.setVisibility(View.VISIBLE);
        } else {
            holder.iconImportant.setVisibility(View.GONE);
        }

        // Format và hiển thị thời gian
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault());
        String timeFormatted = sdf.format(new Date(event.getStartTime()));
        holder.txtTime.setText(timeFormatted);

        // Xử lý note (ẩn nếu rỗng)
        if (event.getNote() == null || event.getNote().trim().isEmpty()) {
            holder.txtNote.setVisibility(View.GONE);
        } else {
            holder.txtNote.setVisibility(View.VISIBLE);
            holder.txtNote.setText(event.getNote());
        }

        // Lấy CheckBox từ holder
        if (holder.checkDone == null) {
            holder.checkDone = holder.itemView.findViewById(R.id.checkDone);
        }

        // Thiết lập trạng thái và hiệu ứng cho CheckBox
        holder.checkDone.setOnCheckedChangeListener(null);
        holder.checkDone.setChecked(false);
        updateEventDoneUI(holder, holder.checkDone.isChecked());
        holder.checkDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateEventDoneUI(holder, isChecked);
        });

        // Xác định trạng thái và đổi màu
        String status;
        int statusColor;
        int statusBgColor;
        int barColor;

        if (event.getEndTime() < now) {
            // Đã qua
            status = "Đã qua";
            statusColor = Color.parseColor("#757575");
            statusBgColor = Color.parseColor("#F5F5F5");
            barColor = Color.parseColor("#9E9E9E");
        } else if (event.getStartTime() <= now && event.getEndTime() >= now) {
            // Đang diễn ra
            status = "Đang diễn ra";
            statusColor = Color.parseColor("#FF5722");
            statusBgColor = Color.parseColor("#FFEBEE");
            barColor = Color.parseColor("#FF5722");
        } else {
            // Sắp tới
            status = "Sắp tới";
            statusColor = Color.parseColor("#4CAF50");
            statusBgColor = Color.parseColor("#E8F5E9");
            barColor = Color.parseColor("#2196F3");
        }

        holder.txtStatus.setText(status);
        holder.txtStatus.setTextColor(statusColor);
        holder.txtStatus.setBackgroundColor(statusBgColor);
        holder.colorBar.setBackgroundColor(barColor);

        // Set click listeners
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(event.getId());
        });

        holder.btnDetail.setOnClickListener(v -> {
            if (listener != null) listener.onDetail(event);
        });
    }

    public void setEventList(List<Event> newList) {
        this.eventList.clear();
        this.eventList.addAll(newList);
        notifyDataSetChanged();
    }

    // 🧩 3. Trả về số lượng phần tử
    @Override
    public int getItemCount() {
        return eventList != null ? eventList.size() : 0;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    // 🧩 4. ViewHolder ánh xạ với item_event.xml
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtNote, txtCategory, txtTime, txtStatus;
        MaterialButton btnDelete, btnDetail;
        ImageView iconImportant;
        View colorBar;

        CheckBox checkDone;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtNote = itemView.findViewById(R.id.txtNote);
            txtCategory = itemView.findViewById(R.id.txtCategory);
            txtTime = itemView.findViewById(R.id.txtTime);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnDetail = itemView.findViewById(R.id.btnDetail);
            iconImportant = itemView.findViewById(R.id.iconImportant);
            colorBar = itemView.findViewById(R.id.colorBar);
            checkDone = itemView.findViewById(R.id.checkDone);
        }
    }
}