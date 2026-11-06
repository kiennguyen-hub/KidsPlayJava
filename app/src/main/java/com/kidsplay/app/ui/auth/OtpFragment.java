package com.kidsplay.app.ui.auth;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.kidsplay.app.R;
import com.kidsplay.app.databinding.FragmentOtpBinding;

import java.util.Locale;

/**
 * Màn hình Xác thực tài khoản (nhập mã OTP 6 số, đếm ngược gửi lại mã).
 * Lưu ý: phần giao diện và thao tác nhập đã hoàn chỉnh; việc GỬI và KIỂM TRA mã cần máy chủ
 * (SMS/Email OTP) nên chưa nối vào luồng đăng ký hiện tại.
 */
public class OtpFragment extends Fragment {

    private FragmentOtpBinding binding;
    private CountDownTimer timer;
    private boolean canResend = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOtpBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBack.setOnClickListener(v -> ((AuthActivity) requireActivity()).goBack());

        EditText[] boxes = {binding.otp1, binding.otp2, binding.otp3, binding.otp4, binding.otp5, binding.otp6};
        for (int i = 0; i < boxes.length; i++) {
            final int index = i;
            boxes[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && index < boxes.length - 1) {
                        boxes[index + 1].requestFocus(); // tự nhảy sang ô kế tiếp
                    }
                }
            });
            boxes[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN
                        && boxes[index].getText().length() == 0 && index > 0) {
                    boxes[index - 1].setText("");
                    boxes[index - 1].requestFocus();
                    return true;
                }
                return false;
            });
        }
        boxes[0].requestFocus();

        binding.btnConfirm.setOnClickListener(v -> {
            StringBuilder code = new StringBuilder();
            for (EditText b : boxes) code.append(b.getText());
            if (code.length() < 6) {
                Toast.makeText(requireContext(), "Vui lòng nhập đủ 6 số", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(),
                        "Xác thực OTP cần máy chủ gửi mã, chưa được kết nối trong bản này",
                        Toast.LENGTH_LONG).show();
            }
        });

        binding.tvResend.setOnClickListener(v -> {
            if (!canResend) return;
            for (EditText b : boxes) b.setText("");
            boxes[0].requestFocus();
            startTimer();
            Toast.makeText(requireContext(), "Gửi lại mã chưa được kết nối trong bản này",
                    Toast.LENGTH_SHORT).show();
        });

        startTimer();
    }

    private void startTimer() {
        canResend = false;
        binding.tvResend.setTextColor(ContextCompat.getColor(requireContext(), R.color.kp_grey));
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(59_999, 1000) {
            @Override
            public void onTick(long ms) {
                if (binding != null) {
                    binding.tvResend.setText(String.format(Locale.US, "Gửi lại mã sau 00:%02d", ms / 1000));
                }
            }

            @Override
            public void onFinish() {
                if (binding != null) {
                    canResend = true;
                    binding.tvResend.setText("Gửi lại mã");
                    binding.tvResend.setTextColor(ContextCompat.getColor(requireContext(), R.color.kp_blue));
                }
            }
        }.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (timer != null) timer.cancel();
        binding = null;
    }
}
