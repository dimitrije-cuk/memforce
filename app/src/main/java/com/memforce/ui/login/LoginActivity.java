package com.memforce.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.memforce.R;
import com.memforce.data.UserDao;
import com.memforce.databinding.ActivityLoginBinding;
import com.memforce.model.User;
import com.memforce.session.Session;
import com.memforce.ui.MainActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private Session session;
    private UserDao userDao;
    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new Session(this);
        if (session.isSignedIn()) {
            openMainScreen();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        userDao = new UserDao(this);
        executor = Executors.newSingleThreadExecutor();
        binding.signInButton.setOnClickListener(v -> signIn());
    }

    private void signIn() {
        String name = text(binding.userNameInput.getText());
        String password = text(binding.passwordInput.getText());
        binding.userNameLayout.setError(TextUtils.isEmpty(name) ? getString(R.string.error_required) : null);
        binding.passwordLayout.setError(TextUtils.isEmpty(password) ? getString(R.string.error_required) : null);
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(password)) {
            return;
        }

        binding.signInButton.setEnabled(false);
        // Password hashing is deliberately slow, so it must not run on the UI thread.
        executor.execute(() -> {
            User user = userDao.authenticateOrRegister(name, password);
            runOnUiThread(() -> onSignInResult(user));
        });
    }

    private void onSignInResult(@Nullable User user) {
        binding.signInButton.setEnabled(true);
        if (user == null) {
            binding.passwordLayout.setError(getString(R.string.login_wrong_password));
            return;
        }
        session.signIn(user);
        openMainScreen();
    }

    private void openMainScreen() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private static String text(@Nullable CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    @Override
    protected void onDestroy() {
        if (executor != null) {
            executor.shutdown();
        }
        super.onDestroy();
    }
}
