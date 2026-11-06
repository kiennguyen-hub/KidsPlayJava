package com.kidsplay.app.ui.kid;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kidsplay.app.databinding.FragmentStoryTextBinding;

/** Hiển thị truyện chữ do phụ huynh thêm vào thư viện. */
public class StoryTextFragment extends Fragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_BODY = "body";

    private FragmentStoryTextBinding binding;

    public static StoryTextFragment newInstance(String title, String body) {
        StoryTextFragment f = new StoryTextFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_BODY, body);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStoryTextBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.tvStoryTitle.setText(requireArguments().getString(ARG_TITLE));
        // Nút Quay lại trên màn hình: ghim màn hình có thể làm mất nút Back của hệ thống
        binding.btnBack.setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());
        binding.tvStoryBody.setText(requireArguments().getString(ARG_BODY));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
