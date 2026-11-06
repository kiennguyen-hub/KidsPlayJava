package com.kidsplay.app.ui.auth;

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

import com.kidsplay.app.databinding.FragmentRegisterBinding;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AuthViewModel viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        binding.btnBack.setOnClickListener(v -> ((AuthActivity) requireActivity()).goBack());
        binding.tvGoLogin.setOnClickListener(v -> ((AuthActivity) requireActivity()).goBack());

        binding.btnRegister.setOnClickListener(v -> {
            String name = binding.etName.getText().toString().trim();
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString();
            String confirm = binding.etConfirm.getText().toString();

            if (name.isEmpty()) {
                binding.etName.setError("Vui lòng nhập họ tên");
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.setError("Hiện chỉ hỗ trợ đăng ký bằng email hợp lệ");
            } else if (password.length() < 6) {
                binding.etPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            } else if (!password.equals(confirm)) {
                binding.etConfirm.setError("Mật khẩu nhập lại không khớp");
            } else if (!binding.cbTerms.isChecked()) {
                Toast.makeText(requireContext(), "Bạn cần đồng ý Điều khoản và Chính sách bảo mật",
                        Toast.LENGTH_SHORT).show();
            } else {
                viewModel.register(name, email, password);
            }
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), busy -> {
            boolean isBusy = Boolean.TRUE.equals(busy);
            binding.progress.setVisibility(isBusy ? View.VISIBLE : View.GONE);
            binding.btnRegister.setEnabled(!isBusy);
            binding.btnRegister.setAlpha(isBusy ? 0.6f : 1f);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) {
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
                viewModel.consumeError();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
