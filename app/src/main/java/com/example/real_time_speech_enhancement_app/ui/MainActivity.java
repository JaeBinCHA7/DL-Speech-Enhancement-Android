package com.example.real_time_speech_enhancement_app.ui;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.real_time_speech_enhancement_app.R;
import com.example.real_time_speech_enhancement_app.ui.bottomnavi.AudioFragment;
import com.example.real_time_speech_enhancement_app.ui.bottomnavi.VideoFragment;
import com.example.real_time_speech_enhancement_app.ui.bottomnavi.SettingFragment;
import com.example.real_time_speech_enhancement_app.ui.bottomnavi.StartFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView; //바텀 네비게이션 뷰

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toobar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);//기본 제목을 없애줍니다.
//        actionBar.setDisplayHomeAsUpEnabled(true); # 뒤로 가기

        // Bottom Navigation
        bottomNavigationView = findViewById(R.id.bottomnavi); // 하단 네비게이션
        // 처음화면
        getSupportFragmentManager().beginTransaction().add(R.id.fl_main, new AudioFragment()).commit();
        // 바텀 네비게이션뷰 안의 아이템 설정
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    //item을 클릭시 id값을 가져와 FrameLayout에 fragment.xml띄우기
                    case R.id.bottom_home:
                        getSupportFragmentManager().beginTransaction().replace(R.id.fl_main, new AudioFragment()).commit();
                        break;
                    case R.id.bottom_news:
                        getSupportFragmentManager().beginTransaction().replace(R.id.fl_main, new VideoFragment()).commit();
                        break;
                    case R.id.bottom_quiz:
                        getSupportFragmentManager().beginTransaction().replace(R.id.fl_main, new StartFragment()).commit();
                        break;
                    case R.id.bottom_reward:
                        getSupportFragmentManager().beginTransaction().replace(R.id.fl_main, new SettingFragment()).commit();
                        break;
                }
                return true;
            }
        });

    }
}