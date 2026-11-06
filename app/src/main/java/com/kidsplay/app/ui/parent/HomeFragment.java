package com.kidsplay.app.ui.parent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.kidsplay.app.R;
import com.kidsplay.app.data.local.entities.KidProfileEntity;
import com.kidsplay.app.databinding.FragmentHomeBinding;
import com.kidsplay.app.utils.AvatarUtils;
import com.kidsplay.app.utils.SessionManager;

import java.util.List;
import java.util.Locale;

/** Trang chủ: thẻ "Tài khoản của bé" và 4 lối tắt (nội dung, thời gian, khóa app, báo cáo). */
public class HomeFragment extends BaseParentFragment {

    private FragmentHomeBinding binding;
    private ParentViewModel viewModel;
    private KidProfileEntity currentKid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ParentViewModel.class);
        binding.tvParentName.setText(new SessionManager(requireContext()).getParentName());

        viewModel.getAllKids().observe(getViewLifecycleOwner(), this::bindKid);

        binding.btnEnterKid.setOnClickListener(v -> {
            if (currentKid != null) KidModeLauncher.enter(this, currentKid);
        });
        binding.btnEditKid.setOnClickListener(v -> {
            if (currentKid != null) host().navigateTo(KidDetailFragment.newInstance(currentKid.id));
        });
        binding.btnDeleteKid.setOnClickListener(v -> confirmDelete());
        binding.btnAddKidEmpty.setOnClickListener(v -> host().navigateTo(AddKidFragment.newInstance(-1)));

        binding.tileContent.setOnClickListener(v -> host().navigateTo(new ContentManageFragment()));
        binding.tileTime.setOnClickListener(v -> {
            if (currentKid == null) {
                Toast.makeText(requireContext(), "Hãy tạo hồ sơ bé trước", Toast.LENGTH_SHORT).show();
            } else {
                ParentDialogs.showTimeLimit(requireContext(), currentKid, viewModel);
            }
        });
        binding.tileLock.setOnClickListener(v -> ParentDialogs.showLockSetting(requireContext()));
        binding.tileReport.setOnClickListener(v -> host().selectTab(R.id.nav_reports));
        binding.btnBell.setOnClickListener(v -> host().selectTab(R.id.nav_reports));
    }

    private void bindKid(List<KidProfileEntity> kids) {
        currentKid = kids == null || kids.isEmpty() ? null : kids.get(0);
        boolean has = currentKid != null;
        binding.cardKid.setVisibility(has ? View.VISIBLE : View.GONE);
        binding.cardNoKid.setVisibility(has ? View.GONE : View.VISIBLE);
        if (!has) return;

        binding.tvKidName.setText(currentKid.name);
        binding.tvKidInfo.setText(String.format(Locale.getDefault(), "%d tuổi · Giới hạn %d phút / ngày",
                currentKid.currentAge(), currentKid.dailyLimitMinutes));
        AvatarUtils.bind(binding.ivKidAvatar, currentKid.avatarPath, currentKid.id);
    }

    private void confirmDelete() {
        if (currentKid == null) return;
        KidProfileEntity kid = currentKid;
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa hồ sơ \"" + kid.name + "\"?")
                .setMessage("Hồ sơ và nhật ký sử dụng của bé sẽ bị xóa khỏi máy.")
                .setPositiveButton("Xóa", (d, w) -> viewModel.deleteKid(kid, null))
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
