package com.kidsplay.app.ui.kid;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.kidsplay.app.databinding.FragmentComicReaderBinding;
import com.kidsplay.app.utils.ComicStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Đọc truyện tranh kiểu MangaDex: mỗi ảnh là một trang, vuốt để qua trang.
 * Có thể đổi hướng đọc (trái→phải / phải→trái cho manga) và chiều vuốt (ngang / dọc).
 */
public class ComicReaderFragment extends Fragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_FOLDER = "folder";

    private FragmentComicReaderBinding binding;
    private boolean rightToLeft = false;
    private boolean vertical = false;

    public static ComicReaderFragment newInstance(String title, String folder) {
        ComicReaderFragment f = new ComicReaderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_FOLDER, folder);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentComicReaderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.tvTitle.setText(requireArguments().getString(ARG_TITLE));
        // Nút Quay lại trên màn hình: ghim màn hình có thể làm mất nút Back của hệ thống
        binding.btnBack.setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());

        File[] files = new File(requireArguments().getString(ARG_FOLDER, ""))
                .listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).matches(".+\\.(jpg|jpeg|png|webp|gif)"));
        if (files == null || files.length == 0) {
            Toast.makeText(requireContext(), "Truyện này chưa có trang nào", Toast.LENGTH_SHORT).show();
            return;
        }
        Arrays.sort(files, (a, b) -> ComicStorage.naturalCompare(a.getName(), b.getName()));
        List<File> pages = new ArrayList<>(Arrays.asList(files));

        binding.pager.setAdapter(new PageAdapter(pages));
        binding.pager.setOffscreenPageLimit(1);
        binding.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updatePageLabel(position, pages.size());
            }
        });
        updatePageLabel(0, pages.size());

        binding.btnDirection.setOnClickListener(v -> {
            rightToLeft = !rightToLeft;
            applyReadingMode();
        });
        binding.btnOrientation.setOnClickListener(v -> {
            vertical = !vertical;
            applyReadingMode();
        });
        applyReadingMode();
    }

    private void updatePageLabel(int position, int total) {
        if (binding != null) {
            binding.tvPage.setText(String.format(Locale.getDefault(), "%d/%d", position + 1, total));
        }
    }

    private void applyReadingMode() {
        int current = binding.pager.getCurrentItem();
        binding.pager.setOrientation(vertical ? ViewPager2.ORIENTATION_VERTICAL : ViewPager2.ORIENTATION_HORIZONTAL);
        // Hướng phải→trái chỉ áp dụng khi vuốt ngang
        binding.pager.setLayoutDirection(rightToLeft && !vertical
                ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        binding.pager.setCurrentItem(current, false);

        binding.btnDirection.setText(rightToLeft ? "Phải→Trái" : "Trái→Phải");
        binding.btnDirection.setEnabled(!vertical);
        binding.btnOrientation.setText(vertical ? "Vuốt dọc" : "Vuốt ngang");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /** Mỗi trang là một ImageView; Glide tự thu nhỏ ảnh lớn để không tràn bộ nhớ. */
    private static class PageAdapter extends RecyclerView.Adapter<PageAdapter.VH> {
        private final List<File> pages;

        PageAdapter(List<File> pages) {
            this.pages = pages;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(parent.getContext());
            iv.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setBackgroundColor(Color.BLACK);
            return new VH(iv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.image.setContentDescription("Trang " + (position + 1));
            Glide.with(holder.image).load(pages.get(position)).into(holder.image);
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final ImageView image;

            VH(@NonNull ImageView itemView) {
                super(itemView);
                this.image = itemView;
            }
        }
    }
}
