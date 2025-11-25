package com.example.btlandr;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class GroupPagerAdapter extends FragmentStateAdapter {

    public GroupPagerAdapter(@NonNull FragmentActivity fa) {
        super(fa);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0)
            return new AllGroupEventsFragment();
        else if (position == 1) {
            return new MyManagedGroupsFragment();
        } else
            return new InvitedGroupsFragment();
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
