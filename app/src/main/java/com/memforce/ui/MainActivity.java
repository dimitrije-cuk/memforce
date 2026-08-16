package com.memforce.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.memforce.R;
import com.memforce.databinding.ActivityMainBinding;
import com.memforce.session.Session;
import com.memforce.ui.login.LoginActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Session session = new Session(this);
        if (!session.isSignedIn()) {
            openLogin();
            return;
        }

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.greeting.setText(getString(R.string.menu_greeting, session.getUserName()));
        binding.signOutButton.setOnClickListener(v -> {
            session.signOut();
            openLogin();
        });
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
