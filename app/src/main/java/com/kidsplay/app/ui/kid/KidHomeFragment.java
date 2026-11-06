package com.kidsplay.app.ui.kid;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.kidsplay.app.data.local.entities.ContentEntity;
import com.kidsplay.app.databinding.FragmentKidHomeBinding;
import com.kidsplay.app.data.local.entities.KidProfileEntity;
import com.kidsplay.app.ui.common.ContentAdapter;
import com.kidsplay.app.ui.parent.ParentDashboardActivity;
import com.kidsplay.app.utils.KidModeHelper;
import com.kidsplay.app.utils.PinHasher;

import java.io.File;

/** Danh sách nội dung phù hợp độ tuổi của bé + nút vào đọc truyện cảm biến. */
public class KidHomeFragment extends Fragment {

    private static final String ARG_KID_ID = "kid_id";

    private FragmentKidHomeBinding binding;

    public static KidHomeFragment newInstance(long kidId) {
        KidHomeFragment f = new KidHomeFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_KID_ID, kidId);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentKidHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        long kidId = requireArguments().getLong(ARG_KID_ID, -1);
        KidHomeViewModel viewModel = new ViewModelProvider(this).get(KidHomeViewModel.class);
        viewModel.setKidId(kidId);

        ContentAdapter adapter = new ContentAdapter(this::openContent, null);
        binding.rvContents.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvContents.setAdapter(adapter);

        viewModel.getKid().observe(getViewLifecycleOwner(), kid -> {
            if (kid != null) {
                binding.tvGreeting.setText("Chào " + kid.name + "! Hôm nay bé muốn xem gì?");
                int remaining = Math.max(0, kid.dailyLimitMinutes - kid.usedMinutesToday);
                binding.tvRemaining.setText("⏱ Hôm nay bé còn " + remaining + " phút");
            }
        });

        viewModel.getContents().observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
            binding.tvEmpty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        });

        binding.btnParentExit.setOnClickListener(v -> showParentPinDialog(viewModel.getKid().getValue(), kidId));

        binding.btnReadStory.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), StoryReaderActivity.class);
            intent.putExtra("KID_ID", kidId);
            startActivity(intent);
        });
    }

    /** Phụ huynh nhập PIN để thoát Kid Mode và quay về trang quản lý. */
    private void showParentPinDialog(@Nullable KidProfileEntity kid, long kidId) {
        if (kid == null) return;
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        new AlertDialog.Builder(requireContext())
                .setTitle("Nhập mã PIN Phụ huynh")
                .setView(input)
                .setPositiveButton("Thoát Kid Mode", (d, w) -> {
                    String entered = input.getText().toString();
                    android.app.Activity activity = requireActivity();
                    java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                        String pinHash = com.kidsplay.app.data.local.AppDatabase
                                .getDatabase(activity.getApplicationContext()).parentDao().getPinHash(kid.parentId);
                        boolean ok = pinHash == null || pinHash.isEmpty() || PinHasher.matches(pinHash, entered);
                        activity.runOnUiThread(() -> {
                            if (activity.isFinishing() || activity.isDestroyed()) return;
                            if (ok) {
                                KidModeHelper.disableKidMode(activity);
                                Intent intent = new Intent(activity, ParentDashboardActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                activity.startActivity(intent);
                                activity.finish();
                            } else {
                                KidModeHelper.logExitAttempt(activity, kidId);
                                Toast.makeText(activity, "Mã PIN không chính xác!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void openContent(ContentEntity item) {
        KidModeActivity host = (KidModeActivity) requireActivity();
        if (ContentEntity.TYPE_VIDEO.equals(item.type)) {
            if (item.filePath == null || !new File(item.filePath).exists()) {
                Toast.makeText(requireContext(), "Không tìm thấy file video", Toast.LENGTH_SHORT).show();
                return;
            }
            host.openFragment(VideoPlayerFragment.newInstance(item.title, item.filePath));
        } else if (ContentEntity.TYPE_COMIC.equals(item.type)) {
            if (item.filePath == null || !new File(item.filePath).isDirectory()) {
                Toast.makeText(requireContext(), "Không tìm thấy các trang truyện", Toast.LENGTH_SHORT).show();
                return;
            }
            host.openFragment(ComicReaderFragment.newInstance(item.title, item.filePath));
        } else {
            host.openFragment(StoryTextFragment.newInstance(item.title, item.body));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
