package com.kidsplay.app.ui.parent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.kidsplay.app.R;
import com.kidsplay.app.data.local.entities.ContentEntity;
import com.kidsplay.app.databinding.FragmentContentManageBinding;
import com.kidsplay.app.ui.common.ContentAdapter;

import java.util.Locale;

/** "Quản lý nội dung an toàn": danh sách nội dung của phụ huynh, xuất bản/ẩn/xóa, đồng bộ cloud. */
public class ContentManageFragment extends BaseParentFragment {

    private FragmentContentManageBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentContentManageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ContentViewModel viewModel = new ViewModelProvider(requireActivity()).get(ContentViewModel.class);

        ContentAdapter adapter = new ContentAdapter(
                item -> showActions(viewModel, item),
                item -> showActions(viewModel, item));
        binding.rvContents.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvContents.setAdapter(adapter);

        viewModel.getAllContents().observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
            int n = list == null ? 0 : list.size();
            binding.tvCount.setText(String.format(Locale.getDefault(), "%d nội dung", n));
            binding.tvEmpty.setVisibility(n == 0 ? View.VISIBLE : View.GONE);
        });

        binding.btnBack.setOnClickListener(v -> goBack());
        binding.btnAdd.setOnClickListener(v -> host().navigateTo(new AddContentFragment()));
        binding.btnSync.setOnClickListener(v -> viewModel.syncNow(false));
        viewModel.getSyncing().observe(getViewLifecycleOwner(), syncing -> {
            boolean busy = Boolean.TRUE.equals(syncing);
            binding.btnSync.setEnabled(!busy);
            binding.btnSync.setText(busy ? "Đang đồng bộ..." : "☁ Đồng bộ");
        });
    }

    private void showActions(ContentViewModel viewModel, ContentEntity item) {
        String[] options = {item.published ? "Ẩn khỏi bé" : "Xuất bản cho bé", "Xóa nội dung"};
        new AlertDialog.Builder(requireContext())
                .setTitle(item.title)
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        viewModel.setPublished(item, !item.published);
                    } else {
                        confirmDelete(viewModel, item);
                    }
                })
                .show();
    }

    private void confirmDelete(ContentViewModel viewModel, ContentEntity item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa nội dung?")
                .setMessage("Xóa \"" + item.title + "\" khỏi thư viện (và khỏi cloud nếu đã đồng bộ).")
                .setPositiveButton("Xóa", (d, w) -> viewModel.delete(item))
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        host().highlightTab(R.id.nav_home);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
