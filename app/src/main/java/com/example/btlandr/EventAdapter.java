package com.example.btlandr;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btlandr.Event;
import com.example.btlandr.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

    private List<Event> eventList;
    private final SimpleDateFormat fmt = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

    public EventAdapter(List<Event> list) {
        this.eventList = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event e = eventList.get(position);
        holder.title.setText(e.getTitle());
        holder.time.setText(fmt.format(new Date(e.getStartTime())) + " - " + fmt.format(new Date(e.getEndTime())));
        holder.category.setText(e.getCategory());
    }

    @Override
    public int getItemCount() { return eventList.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, time, category;
        ViewHolder(View v) {
            super(v);
            title = v.findViewById(R.id.txtTitle);
            time = v.findViewById(R.id.txtTime);
            category = v.findViewById(R.id.txtCategory);
        }
    }
}
