package com.kidsplay.app.ui.parent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.kidsplay.app.R;
import com.kidsplay.app.data.local.entities.KidProfileEntity;
import com.kidsplay.app.databinding.FragmentKidDetailBinding;
import com.kidsplay.app.utils.AvatarUtils;

import java.util.Locale;

/** Màn "Thông tin hồ sơ trẻ". Bấm vào một dòng thông tin để chỉnh sửa hồ sơ. */
public class KidDetailFragment extends BaseParentFragment {

    private static final String ARG_KID_ID = "kid_id";

    private FragmentKidDetailBinding binding;
    private ParentViewModel viewModel;
    private KidProfileEntity kid;

    public static KidDetailFragment newInstance(long kidId) {
        KidDetailFragment f = new KidDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_KID_ID, kidId);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentKidDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ParentViewModel.class);

        binding.btnBack.setOnClickListener(v -> goBack());

        View.OnClickListener edit = v -> {
            if (kid != null) host().navigateTo(AddKidFragment.newInstance(kid.id));
        };
        binding.rowGender.setOnClickListener(edit);
        binding.rowAge.setOnClickListener(edit);
        binding.rowInterests.setOnClickListener(edit);
        binding.rowLimit.setOnClickListener(edit);

        binding.rowTimeSettings.setOnClickListener(v -> {
            if (kid != null) ParentDialogs.showTimeLimit(requireContext(), kid, viewModel);
        });
        binding.rowContentSettings.setOnClickListener(v -> host().navigateTo(new ContentManageFragment()));
        binding.btnDelete.setOnClickListener(v -> confirmDelete());

        viewModel.getKid(requireArguments().getLong(ARG_KID_ID, -1)).observe(getViewLifecycleOwner(), k -> {
            if (k == null) {
                if (kid != null) goBack(); // hồ sơ vừa bị xóa
                return;
            }
            kid = k;
            bind(k);
        });
    }

    private void bind(KidProfileEntity k) {
        AvatarUtils.bind(binding.ivAvatar, k.avatarPath, k.id);
        binding.tvName.setText(k.name);
        boolean hasBirth = k.birthDate != null && !k.birthDate.isEmpty();
        binding.tvSubtitle.setText(hasBirth
                ? String.format(Locale.getDefault(), "%d tuổi (%s)", k.currentAge(), k.birthDate)
                : String.format(Locale.getDefault(), "%d tuổi", k.currentAge()));
        binding.tvGender.setText(k.gender == null || k.gender.isEmpty() ? "Chưa chọn" : k.gender);
        binding.tvAge.setText(String.format(Locale.getDefault(), "%d tuổi", k.currentAge()));
        binding.tvInterests.setText(k.interests == null || k.interests.isEmpty() ? "Chưa chọn" : k.interests);
        binding.tvLimit.setText(String.format(Locale.getDefault(), "%d phút", k.dailyLimitMinutes));
    }

    private void confirmDelete() {
        if (kid == null) return;
        KidProfileEntity target = kid;
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa hồ sơ \"" + target.name + "\"?")
                .setMessage("Hồ sơ và nhật ký sử dụng của bé sẽ bị xóa khỏi máy.")
                .setPositiveButton("Xóa", (d, w) -> viewModel.deleteKid(target, null))
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        host().highlightTab(R.id.nav_kids);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
