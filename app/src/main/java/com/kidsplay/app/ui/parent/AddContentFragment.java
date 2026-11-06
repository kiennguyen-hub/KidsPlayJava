package com.kidsplay.app.ui.parent;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.kidsplay.app.data.repository.ContentRepository;
import com.kidsplay.app.databinding.FragmentAddContentBinding;
import com.kidsplay.app.utils.AgeUtils;
import com.kidsplay.app.utils.VideoStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * Form "Thêm truyện mới": ảnh/video, tên, mô tả, danh mục, độ tuổi, trạng thái.
 * Danh mục quyết định loại nội dung: Truyện tranh (nhiều ảnh), Truyện chữ, hoặc Video.
 */
public class AddContentFragment extends BaseParentFragment {

    private static final String[] CATEGORIES = {"Truyện tranh", "Truyện chữ", "Video"};
    private static final int KIND_COMIC = 0, KIND_STORY = 1, KIND_VIDEO = 2;

    private FragmentAddContentBinding binding;
    private ContentViewModel viewModel;
    private int kind = KIND_COMIC;
    private Uri pickedVideo;
    private final List<Uri> pickedPages = new ArrayList<>();

    private final ActivityResultLauncher<String> pagesLauncher =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris == null || uris.isEmpty() || binding == null) return;
                pickedPages.clear();
                pickedPages.addAll(uris);
                updateMediaInfo();
            });

    private final ActivityResultLauncher<String> videoLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null || binding == null) return;
                pickedVideo = uri;
                updateMediaInfo();
                if (binding.etTitle.getText().toString().trim().isEmpty()) {
                    String name = VideoStorage.queryName(requireContext(), uri);
                    if (name != null) {
                        int dot = name.lastIndexOf('.');
                        binding.etTitle.setText(dot > 0 ? name.substring(0, dot) : name);
                    }
                }
            });

    @Override
    protected boolean showsBottomNav() {
        return false;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddContentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(ContentViewModel.class);

        binding.btnBack.setOnClickListener(v -> goBack());
        binding.btnCancel.setOnClickListener(v -> goBack());
        binding.etCategory.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle("Danh mục")
                .setItems(CATEGORIES, (d, which) -> {
                    kind = which;
                    updateKindUi();
                })
                .show());
        binding.boxMedia.setOnClickListener(v -> {
            if (kind == KIND_VIDEO) videoLauncher.launch("video/*");
            else pagesLauncher.launch("image/*");
        });
        binding.btnSave.setOnClickListener(v -> save());
        updateKindUi();

        viewModel.getUploading().observe(getViewLifecycleOwner(), busy -> {
            boolean isBusy = Boolean.TRUE.equals(busy);
            binding.progress.setVisibility(isBusy ? View.VISIBLE : View.GONE);
            binding.btnSave.setEnabled(!isBusy);
            binding.btnSave.setAlpha(isBusy ? 0.6f : 1f);
        });
        viewModel.getMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        });
        viewModel.getDone().observe(getViewLifecycleOwner(), done -> {
            if (Boolean.TRUE.equals(done)) {
                viewModel.consumeEvents();
                goBack();
            }
        });
    }

    private void updateKindUi() {
        binding.etCategory.setText(CATEGORIES[kind]);
        boolean story = kind == KIND_STORY;
        binding.boxMedia.setVisibility(story ? View.GONE : View.VISIBLE);
        binding.tvMediaInfo.setVisibility(story ? View.GONE : View.VISIBLE);
        binding.etBody.setVisibility(story ? View.VISIBLE : View.GONE);
        binding.tvMediaLabel.setText(kind == KIND_VIDEO ? "Thêm video" : "Thêm ảnh");
        binding.btnSave.setText(kind == KIND_VIDEO ? "Lưu video" : "Lưu truyện");
        updateMediaInfo();
    }

    private void updateMediaInfo() {
        if (kind == KIND_VIDEO) {
            if (pickedVideo == null) {
                binding.tvMediaInfo.setText("Chưa chọn video");
            } else {
                String name = VideoStorage.queryName(requireContext(), pickedVideo);
                binding.tvMediaInfo.setText("Đã chọn: " + (name != null ? name : "1 video"));
            }
        } else if (kind == KIND_COMIC) {
            binding.tvMediaInfo.setText(pickedPages.isEmpty()
                    ? "Chọn nhiều ảnh, mỗi ảnh là một trang (đặt tên 001.jpg, 002.jpg... để xếp đúng thứ tự)"
                    : "Đã chọn " + pickedPages.size() + " trang (tự xếp theo tên file)");
        }
    }

    private void save() {
        String title = binding.etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            binding.etTitle.setError("Vui lòng nhập tên");
            return;
        }

        ContentRepository.Meta meta = new ContentRepository.Meta();
        meta.title = title;
        meta.description = binding.etDesc.getText().toString().trim();
        meta.category = CATEGORIES[kind];
        int[] range = AgeUtils.parseAgeRange(binding.etAge.getText().toString());
        meta.minAge = range[0];
        meta.maxAge = range[1];
        meta.published = binding.rbPublish.isChecked();

        if (kind == KIND_COMIC) {
            if (pickedPages.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng thêm ảnh các trang truyện", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.uploadComic(new ArrayList<>(pickedPages), meta);
        } else if (kind == KIND_VIDEO) {
            if (pickedVideo == null) {
                Toast.makeText(requireContext(), "Vui lòng chọn một video", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.uploadVideo(pickedVideo, meta);
        } else {
            String body = binding.etBody.getText().toString().trim();
            if (body.isEmpty()) {
                binding.etBody.setError("Vui lòng nhập nội dung truyện");
                return;
            }
            viewModel.addStory(meta, body);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
