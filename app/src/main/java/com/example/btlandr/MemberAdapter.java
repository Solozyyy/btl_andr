package com.example.btlandr;

import android.content.Context;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MemberAdapter extends ArrayAdapter<String> {

    public interface OnMemberActionListener {
        void onRemoveMember(String uid, String info);
    }

    private final List<String> uids;
    private final List<String> infos;
    private final boolean isAdmin;
    private final OnMemberActionListener listener;

    public MemberAdapter(Context context, List<String> uids, List<String> infos, boolean isAdmin, OnMemberActionListener listener) {
        super(context, 0, infos);
        this.uids = uids;
        this.infos = infos;
        this.isAdmin = isAdmin;
        this.listener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.member_item, parent, false);

        TextView memberInfo = convertView.findViewById(R.id.memberInfo);
        ImageButton btnRemove = convertView.findViewById(R.id.btnRemove);

        memberInfo.setText(infos.get(position));

        if (isAdmin) {
            btnRemove.setVisibility(View.VISIBLE);
            btnRemove.setOnClickListener(v -> {
                String uid = uids.get(position);
                String info = infos.get(position);
                listener.onRemoveMember(uid, info);
            });
        } else {
            btnRemove.setVisibility(View.GONE);
        }

        return convertView;
    }
}
