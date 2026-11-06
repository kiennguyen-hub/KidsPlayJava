package com.kidsplay.app.ui.parent;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.kidsplay.app.R;
import com.kidsplay.app.data.local.AppDatabase;
import com.kidsplay.app.databinding.FragmentSettingsBinding;
import com.kidsplay.app.ui.auth.AuthActivity;
import com.kidsplay.app.utils.SessionManager;

import java.util.concurrent.Executors;

/** Tab "Cài đặt": thông tin tài khoản, đổi mật khẩu, đăng xuất. */
public class SettingsFragment extends BaseParentFragment {

    private FragmentSettingsBinding binding;
    private SessionManager session;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        session = new SessionManager(requireContext());

        refreshHeader();

        binding.btnBack.setOnClickListener(v -> host().selectTab(R.id.nav_home));
        binding.rowPassword.setOnClickListener(v -> showChangePassword());
        binding.rowProfile.setOnClickListener(v -> showEditProfile());
        binding.rowDevices.setOnClickListener(v -> comingSoon("Thiết bị liên kết"));
        binding.rowNotify.setOnClickListener(v -> comingSoon("Tùy chọn thông báo"));
        binding.rowHelp.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle("Trợ giúp & hỗ trợ")
                .setMessage("KidsPlay giúp phụ huynh quản lý nội dung, thời gian sử dụng và giữ bé trong "
                        + "môi trường an toàn.\n\nMã PIN phụ huynh dùng để thoát chế độ trẻ em; đặt hoặc đổi "
                        + "PIN ở tab Hồ sơ trẻ (nút PIN).")
                .setPositiveButton("Đóng", null)
                .show());
        binding.btnLogout.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất?")
                .setMessage("Bạn sẽ cần đăng nhập lại để quản lý hồ sơ của bé.")
                .setPositiveButton("Đăng xuất", (d, w) -> {
                    session.logout();
                    Intent intent = new Intent(requireContext(), AuthActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Hủy", null)
                .show());
    }

    private void refreshHeader() {
        binding.tvName.setText(session.getParentName());
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        binding.tvEmail.setText(user != null && user.getEmail() != null ? user.getEmail() : "");
    }

    private void comingSoon(String feature) {
        Toast.makeText(requireContext(), feature + ": tính năng đang phát triển", Toast.LENGTH_SHORT).show();
    }

    private LinearLayout boxFor(EditText input) {
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        LinearLayout box = new LinearLayout(requireContext());
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return box;
    }

    private void showChangePassword() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        EditText input = new EditText(requireContext());
        input.setHint("Mật khẩu mới (ít nhất 6 ký tự)");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Đổi mật khẩu")
                .setView(boxFor(input))
                .setPositiveButton("Đổi", null)
                .setNegativeButton("Hủy", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String pw = input.getText().toString();
            if (pw.length() < 6) {
                input.setError("Mật khẩu phải có ít nhất 6 ký tự");
                return;
            }
            user.updatePassword(pw)
                    .addOnSuccessListener(unused -> {
                        dialog.dismiss();
                        if (isAdded()) Toast.makeText(requireContext(), "Đã đổi mật khẩu", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        String msg = e instanceof FirebaseAuthRecentLoginRequiredException
                                ? "Vì lý do bảo mật, hãy đăng xuất, đăng nhập lại rồi đổi mật khẩu"
                                : "Không đổi được mật khẩu: " + e.getMessage();
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                    });
        }));
        dialog.show();
    }

    private void showEditProfile() {
        EditText input = new EditText(requireContext());
        input.setHint("Họ và tên");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setText(session.getParentName());

        new AlertDialog.Builder(requireContext())
                .setTitle("Thông tin cá nhân")
                .setView(boxFor(input))
                .setPositiveButton("Lưu", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) return;
                    long parentId = session.getParentId();
                    android.content.Context app = requireContext().getApplicationContext();
                    Executors.newSingleThreadExecutor().execute(() ->
                            AppDatabase.getDatabase(app).parentDao().updateName(parentId, name));
                    session.login(parentId, name, session.getFirebaseUid());
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        user.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(name).build());
                    }
                    refreshHeader();
                    Toast.makeText(requireContext(), "Đã cập nhật thông tin", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
