package com.kidsplay.app.ui.kid;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kidsplay.app.databinding.FragmentVideoPlayerBinding;

/** Phát video đã lưu trong bộ nhớ riêng của app bằng VideoView + MediaController. */
public class VideoPlayerFragment extends Fragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_PATH = "path";

    private FragmentVideoPlayerBinding binding;

    public static VideoPlayerFragment newInstance(String title, String path) {
        VideoPlayerFragment f = new VideoPlayerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_PATH, path);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVideoPlayerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.tvVideoTitle.setText(requireArguments().getString(ARG_TITLE));
        // Nút Quay lại trên màn hình: ghim màn hình có thể làm mất nút Back của hệ thống
        binding.btnBack.setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());

        MediaController controller = new MediaController(requireContext());
        controller.setAnchorView(binding.videoView);
        binding.videoView.setMediaController(controller);
        binding.videoView.setVideoPath(requireArguments().getString(ARG_PATH));
        binding.videoView.setOnPreparedListener(mp -> binding.videoView.start());
        binding.videoView.setOnErrorListener((mp, what, extra) -> {
            Toast.makeText(requireContext(), "Không phát được video này", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (binding != null) binding.videoView.pause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) binding.videoView.stopPlayback();
        binding = null;
    }
}
