package com.kidsplay.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.kidsplay.app.R;
import com.kidsplay.app.databinding.ActivityAuthBinding;
import com.kidsplay.app.ui.parent.ParentDashboardActivity;
import com.kidsplay.app.utils.SessionManager;

/** Khung chứa các màn hình Đăng nhập, Đăng ký, Xác thực OTP (dạng Fragment). */
public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager session = new SessionManager(this);
        if (session.isLoggedIn() && FirebaseAuth.getInstance().getCurrentUser() != null) {
            openDashboard();
            return;
        }
        if (session.isLoggedIn()) {
            session.logout(); // phiên cũ chưa gắn với Firebase -> bắt đăng nhập lại
        }

        ActivityAuthBinding binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AuthViewModel viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        viewModel.getSuccess().observe(this, ok -> {
            if (Boolean.TRUE.equals(ok)) openDashboard();
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.authContainer, new LoginFragment())
                    .commit();
        }
    }

    public void showRegister() {
        push(new RegisterFragment());
    }

    public void showOtp() {
        push(new OtpFragment());
    }

    public void goBack() {
        getOnBackPressedDispatcher().onBackPressed();
    }

    private void push(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.authContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void openDashboard() {
        Intent intent = new Intent(this, ParentDashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
