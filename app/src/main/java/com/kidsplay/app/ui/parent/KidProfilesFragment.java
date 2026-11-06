package com.kidsplay.app.ui.parent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.kidsplay.app.R;
import com.kidsplay.app.data.local.entities.KidProfileEntity;
import com.kidsplay.app.databinding.FragmentKidProfilesBinding;
import com.kidsplay.app.utils.PinDialogs;
import com.kidsplay.app.utils.SessionManager;

import java.util.Locale;

/** Tab "Hồ sơ trẻ": danh sách các bé, nút thêm hồ sơ và đặt/đổi mã PIN. */
public class KidProfilesFragment extends BaseParentFragment {

    private FragmentKidProfilesBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentKidProfilesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ParentViewModel viewModel = new ViewModelProvider(this).get(ParentViewModel.class);

        KidProfileAdapter adapter = new KidProfileAdapter(new KidProfileAdapter.Listener() {
            @Override
            public void onOpen(KidProfileEntity kid) {
                host().navigateTo(KidDetailFragment.newInstance(kid.id));
            }

            @Override
            public void onEnter(KidProfileEntity kid) {
                KidModeLauncher.enter(KidProfilesFragment.this, kid);
            }

            @Override
            public void onEdit(KidProfileEntity kid) {
                host().navigateTo(KidDetailFragment.newInstance(kid.id));
            }
        });
        binding.rvKids.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvKids.setAdapter(adapter);

        viewModel.getAllKids().observe(getViewLifecycleOwner(), kids -> {
            adapter.submit(kids);
            int n = kids == null ? 0 : kids.size();
            binding.tvCount.setText(String.format(Locale.getDefault(), "%d hồ sơ", n));
            binding.tvEmpty.setVisibility(n == 0 ? View.VISIBLE : View.GONE);
        });

        // Hồ sơ bé lưu trong máy (chỉ nội dung truyện/video được đồng bộ cloud)
        binding.tvSync.setBackgroundResource(R.drawable.bg_badge_grey);
        binding.tvSync.setTextColor(requireContext().getColor(R.color.kp_grey));
        binding.tvSync.setText("Lưu trong máy");

        binding.btnBack.setOnClickListener(v -> host().selectTab(R.id.nav_home));
        binding.btnAddKid.setOnClickListener(v -> host().navigateTo(AddKidFragment.newInstance(-1)));
        binding.tvPin.setOnClickListener(v ->
                PinDialogs.showSetPin(requireActivity(), new SessionManager(requireContext()).getParentId(), null));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
