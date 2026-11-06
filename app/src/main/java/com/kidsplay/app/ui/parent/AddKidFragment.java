package com.kidsplay.app.ui.parent;

import android.app.DatePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.kidsplay.app.data.local.entities.KidProfileEntity;
import com.kidsplay.app.databinding.FragmentAddKidBinding;
import com.kidsplay.app.utils.AgeUtils;
import com.kidsplay.app.utils.AvatarUtils;
import com.kidsplay.app.utils.SessionManager;
import com.kidsplay.app.utils.UsageTracker;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;

/** Form "Thêm hồ sơ trẻ" (cũng dùng để sửa hồ sơ đã có khi truyền kidId). */
public class AddKidFragment extends BaseParentFragment {

    private static final String ARG_KID_ID = "kid_id";

    private FragmentAddKidBinding binding;
    private ParentViewModel viewModel;
    private KidProfileEntity editing;
    private boolean prefilled = false;
    private Uri pickedAvatar;
    private String gender = "Nam";
    private final Calendar birth = Calendar.getInstance();

    private final ActivityResultLauncher<String> avatarLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null || binding == null) return;
                pickedAvatar = uri;
                showAvatarPreview(uri);
            });

    public static AddKidFragment newInstance(long kidId) {
        AddKidFragment f = new AddKidFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_KID_ID, kidId);
        f.setArguments(args);
        return f;
    }

    @Override
    protected boolean showsBottomNav() {
        return false;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddKidBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ParentViewModel.class);

        birth.set(2020, Calendar.JANUARY, 1);

        binding.btnBack.setOnClickListener(v -> goBack());
        binding.ivAvatar.setOnClickListener(v -> avatarLauncher.launch("image/*"));
        binding.etBirth.setOnClickListener(v -> pickBirthDate());

        // Giới tính: chọn một
        View[] genderChips = {binding.chipMale, binding.chipFemale, binding.chipOther};
        String[] genders = {"Nam", "Nữ", "Khác"};
        for (int i = 0; i < genderChips.length; i++) {
            final int index = i;
            genderChips[i].setOnClickListener(v -> selectGender(genders[index]));
        }
        selectGender("Nam");

        // Sở thích: chọn nhiều
        for (View chip : interestChips()) {
            chip.setOnClickListener(v -> v.setSelected(!v.isSelected()));
        }
        binding.chipInt1.setSelected(true); // mặc định như thiết kế: Truyện tranh

        binding.btnSave.setOnClickListener(v -> save());

        long kidId = requireArguments().getLong(ARG_KID_ID, -1);
        if (kidId > 0) {
            binding.tvTitle.setText("Sửa hồ sơ trẻ");
            viewModel.getKid(kidId).observe(getViewLifecycleOwner(), kid -> {
                if (kid != null && !prefilled) {
                    prefilled = true;
                    editing = kid;
                    prefill(kid);
                }
            });
        }
    }

    private TextView[] interestChips() {
        return new TextView[]{binding.chipInt1, binding.chipInt2, binding.chipInt3,
                binding.chipInt4, binding.chipInt5, binding.chipInt6};
    }

    private void selectGender(String value) {
        gender = value;
        binding.chipMale.setSelected("Nam".equals(value));
        binding.chipFemale.setSelected("Nữ".equals(value));
        binding.chipOther.setSelected("Khác".equals(value));
    }

    private void pickBirthDate() {
        DatePickerDialog dialog = new DatePickerDialog(requireContext(), (picker, y, m, d) -> {
            birth.set(y, m, d);
            binding.etBirth.setText(AgeUtils.format(birth));
            binding.etBirth.setError(null);
        }, birth.get(Calendar.YEAR), birth.get(Calendar.MONTH), birth.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private void showAvatarPreview(Object source) {
        binding.ivAvatar.setImageTintList(null);
        binding.ivAvatar.setPadding(0, 0, 0, 0);
        Glide.with(binding.ivAvatar).load(source).circleCrop().into(binding.ivAvatar);
    }

    private void prefill(KidProfileEntity kid) {
        binding.etName.setText(kid.name);
        if (kid.birthDate != null && !kid.birthDate.isEmpty()) {
            binding.etBirth.setText(kid.birthDate);
            if (AgeUtils.parse(kid.birthDate) != null) birth.setTime(AgeUtils.parse(kid.birthDate));
        }
        if (kid.gender != null) selectGender(kid.gender);
        binding.etLimit.setText(String.valueOf(kid.dailyLimitMinutes));

        String interests = kid.interests == null ? "" : kid.interests;
        for (TextView chip : interestChips()) {
            chip.setSelected(interests.contains(chip.getText().toString()));
        }
        if (kid.avatarPath != null && new File(kid.avatarPath).exists()) {
            showAvatarPreview(new File(kid.avatarPath));
        }
    }

    private void save() {
        String name = binding.etName.getText().toString().trim();
        String birthText = binding.etBirth.getText().toString().trim();
        String limitText = binding.etLimit.getText().toString().trim();

        if (name.isEmpty()) {
            binding.etName.setError("Vui lòng nhập tên bé");
            return;
        }
        int age = AgeUtils.ageOf(birthText);
        if (age < 0) {
            binding.etBirth.setError("Hãy chọn ngày sinh");
            Toast.makeText(requireContext(), "Hãy chọn ngày sinh của bé", Toast.LENGTH_SHORT).show();
            return;
        }
        if (age > 17) {
            binding.etBirth.setError("Hồ sơ dành cho trẻ dưới 18 tuổi");
            return;
        }
        int limit = 40;
        if (!limitText.isEmpty()) {
            try {
                limit = Integer.parseInt(limitText);
            } catch (NumberFormatException ignored) {
                limit = 0;
            }
        }
        if (limit < 1 || limit > 600) {
            binding.etLimit.setError("Nhập từ 1 đến 600 phút");
            return;
        }

        List<String> chosen = new ArrayList<>();
        for (TextView chip : interestChips()) {
            if (chip.isSelected()) chosen.add(chip.getText().toString());
        }
        String interests = String.join(", ", chosen);
        final int finalLimit = limit;

        binding.btnSave.setEnabled(false);
        binding.btnSave.setAlpha(0.6f);
        long parentId = new SessionManager(requireContext()).getParentId();
        android.app.Activity activity = requireActivity();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String avatarPath = editing != null ? editing.avatarPath : null;
                if (pickedAvatar != null) {
                    String old = avatarPath;
                    avatarPath = AvatarUtils.copyAvatar(activity.getApplicationContext(), pickedAvatar);
                    if (old != null) {
                        File f = new File(old);
                        if (f.exists() && !f.delete()) f.deleteOnExit();
                    }
                }

                KidProfileEntity kid = editing != null ? editing
                        : new KidProfileEntity(name, age, interests, finalLimit, 0, "", UsageTracker.today());
                kid.name = name;
                kid.age = age;
                kid.birthDate = birthText;
                kid.gender = gender;
                kid.avatarPath = avatarPath;
                kid.interests = interests;
                kid.allowedCategories = interests;
                kid.dailyLimitMinutes = finalLimit;
                if (editing == null) kid.parentId = parentId;

                Runnable done = () -> activity.runOnUiThread(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(activity, "Đã lưu hồ sơ của " + name, Toast.LENGTH_SHORT).show();
                    goBack();
                });
                if (editing != null) {
                    viewModel.updateKid(kid);
                    done.run();
                } else {
                    viewModel.createKidProfile(kid, id -> done.run());
                }
            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    if (binding != null) {
                        binding.btnSave.setEnabled(true);
                        binding.btnSave.setAlpha(1f);
                    }
                    Toast.makeText(activity, "Lưu thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
