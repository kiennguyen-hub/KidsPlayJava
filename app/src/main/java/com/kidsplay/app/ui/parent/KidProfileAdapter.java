package com.kidsplay.app.ui.parent;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kidsplay.app.data.local.entities.KidProfileEntity;
import com.kidsplay.app.databinding.ItemKidProfileBinding;
import com.kidsplay.app.utils.AvatarUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class KidProfileAdapter extends RecyclerView.Adapter<KidProfileAdapter.VH> {

    public interface Listener {
        void onOpen(KidProfileEntity kid);   // bấm vào thẻ: xem thông tin hồ sơ
        void onEnter(KidProfileEntity kid);  // vào chế độ trẻ em
        void onEdit(KidProfileEntity kid);   // nút Sửa
    }

    private final List<KidProfileEntity> items = new ArrayList<>();
    private final Listener listener;

    public KidProfileAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<KidProfileEntity> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemKidProfileBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        KidProfileEntity kid = items.get(position);
        holder.binding.tvName.setText(kid.name);
        holder.binding.tvInfo.setText(String.format(Locale.getDefault(), "%d tuổi · Giới hạn %d phút",
                kid.currentAge(), kid.dailyLimitMinutes));
        AvatarUtils.bind(holder.binding.ivAvatar, kid.avatarPath, kid.id);

        holder.binding.getRoot().setOnClickListener(v -> listener.onOpen(kid));
        holder.binding.btnEnter.setOnClickListener(v -> listener.onEnter(kid));
        holder.binding.btnEdit.setOnClickListener(v -> listener.onEdit(kid));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemKidProfileBinding binding;

        VH(ItemKidProfileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
