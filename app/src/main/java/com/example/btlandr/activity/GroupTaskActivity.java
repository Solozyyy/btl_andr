package com.example.btlandr.activity;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.btlandr.R;
import com.example.btlandr.adapter.GroupPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class GroupTaskActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_task);

        TabLayout tabLayout = findViewById(R.id.tabLayoutGroup);
        ViewPager2 viewPager = findViewById(R.id.viewPagerGroup);

        GroupPagerAdapter pagerAdapter = new GroupPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Sự kiện");
                            break;
                        case 1:
                            tab.setText("Nhóm tôi quản lý");
                            break;
                        case 2:
                            tab.setText("Nhóm được mời");
                            break;
                    }
                }).attach();
    }
}
