package com.kidsplay.app.ui.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.kidsplay.app.databinding.FragmentLoginBinding;

public class LoginFragment extends Fragment {

    private static final String PREFS = "kp_login";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_REMEMBER = "remember";

    private FragmentLoginBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AuthViewModel viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        // "Ghi nhớ": chỉ nhớ email cho lần sau (không lưu mật khẩu vì lý do an toàn)
        if (prefs.getBoolean(KEY_REMEMBER, false)) {
            binding.cbRemember.setChecked(true);
            binding.etEmail.setText(prefs.getString(KEY_EMAIL, ""));
        }

        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString();

            if (email.isEmpty()) {
                binding.etEmail.setError("Vui lòng nhập email");
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.setError("Hãy nhập địa chỉ email hợp lệ");
                return;
            }
            if (password.isEmpty()) {
                binding.etPassword.setError("Vui lòng nhập mật khẩu");
                return;
            }

            SharedPreferences.Editor editor = prefs.edit();
            if (binding.cbRemember.isChecked()) {
                editor.putBoolean(KEY_REMEMBER, true).putString(KEY_EMAIL, email);
            } else {
                editor.remove(KEY_REMEMBER).remove(KEY_EMAIL);
            }
            editor.apply();

            viewModel.login(email, password);
        });

        binding.tvForgot.setOnClickListener(v -> sendResetEmail());

        binding.btnGoogle.setOnClickListener(v -> Toast.makeText(requireContext(),
                "Đăng nhập bằng Google chưa được hỗ trợ trong bản này", Toast.LENGTH_SHORT).show());

        binding.tvGoRegister.setOnClickListener(v -> ((AuthActivity) requireActivity()).showRegister());

        viewModel.getLoading().observe(getViewLifecycleOwner(), busy -> {
            boolean isBusy = Boolean.TRUE.equals(busy);
            binding.progress.setVisibility(isBusy ? View.VISIBLE : View.GONE);
            binding.btnLogin.setEnabled(!isBusy);
            binding.btnLogin.setAlpha(isBusy ? 0.6f : 1f);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) {
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
                viewModel.consumeError();
            }
        });
    }

    /** Quên mật khẩu: Firebase gửi email đặt lại mật khẩu tới địa chỉ đã nhập. */
    private void sendResetEmail() {
        String email = binding.etEmail.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Nhập email của bạn rồi bấm lại \"Quên mật khẩu?\"");
            return;
        }
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                                "Nếu email tồn tại, hướng dẫn đặt lại mật khẩu đã được gửi tới " + email,
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Không gửi được email: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
