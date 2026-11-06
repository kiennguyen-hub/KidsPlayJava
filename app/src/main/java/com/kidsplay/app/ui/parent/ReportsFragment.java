package com.kidsplay.app.ui.parent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kidsplay.app.R;
import com.kidsplay.app.data.local.AppDatabase;
import com.kidsplay.app.data.local.KidDao;
import com.kidsplay.app.data.local.entities.KidProfileEntity;
import com.kidsplay.app.databinding.FragmentReportsBinding;
import com.kidsplay.app.databinding.ItemReportKidBinding;
import com.kidsplay.app.utils.AvatarUtils;
import com.kidsplay.app.utils.UsageTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/** Tab "Báo cáo": thời gian đã dùng hôm nay, số lần cố thoát và yêu cầu thêm giờ của từng bé. */
public class ReportsFragment extends BaseParentFragment {

    private static class Row {
        final KidProfileEntity kid;
        final int usedToday;
        final int exitAttempts;
        final int extraRequests;

        Row(KidProfileEntity kid, int usedToday, int exitAttempts, int extraRequests) {
            this.kid = kid;
            this.usedToday = usedToday;
            this.exitAttempts = exitAttempts;
            this.extraRequests = extraRequests;
        }
    }

    private FragmentReportsBinding binding;
    private ReportAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentReportsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBack.setOnClickListener(v -> host().selectTab(R.id.nav_home));
        adapter = new ReportAdapter();
        binding.rvReports.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvReports.setAdapter(adapter);

        ParentViewModel viewModel = new ViewModelProvider(this).get(ParentViewModel.class);
        android.app.Activity activity = requireActivity();
        KidDao dao = AppDatabase.getDatabase(activity.getApplicationContext()).kidDao();

        viewModel.getAllKids().observe(getViewLifecycleOwner(), kids -> {
            binding.tvEmpty.setVisibility(kids == null || kids.isEmpty() ? View.VISIBLE : View.GONE);
            if (kids == null) return;
            // Đếm nhật ký ở luồng nền rồi cập nhật danh sách
            Executors.newSingleThreadExecutor().execute(() -> {
                String today = UsageTracker.today();
                List<Row> rows = new ArrayList<>();
                for (KidProfileEntity k : kids) {
                    int used = today.equals(k.lastResetDate) ? k.usedMinutesToday : 0;
                    rows.add(new Row(k, used,
                            dao.countLogs(k.id, "EXIT_ATTEMPT"),
                            dao.countLogs(k.id, "EXTRA_TIME_REQUEST")));
                }
                activity.runOnUiThread(() -> {
                    if (binding != null) adapter.submit(rows);
                });
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.VH> {
        private final List<Row> rows = new ArrayList<>();

        void submit(List<Row> newRows) {
            rows.clear();
            rows.addAll(newRows);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(ItemReportKidBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Row r = rows.get(position);
            AvatarUtils.bind(h.binding.ivAvatar, r.kid.avatarPath, r.kid.id);
            h.binding.tvName.setText(r.kid.name);
            h.binding.tvUsage.setText(String.format(Locale.getDefault(),
                    "Hôm nay: %d / %d phút", r.usedToday, r.kid.dailyLimitMinutes));
            int percent = r.kid.dailyLimitMinutes <= 0 ? 0
                    : Math.min(100, r.usedToday * 100 / r.kid.dailyLimitMinutes);
            h.binding.pbUsage.setProgress(percent);
            h.binding.tvStats.setText(String.format(Locale.getDefault(),
                    "Cố thoát khỏi ứng dụng: %d lần  ·  Xin thêm giờ: %d lần", r.exitAttempts, r.extraRequests));
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final ItemReportKidBinding binding;

            VH(ItemReportKidBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
