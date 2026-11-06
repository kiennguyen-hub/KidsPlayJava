package com.kidsplay.app.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.kidsplay.app.R;
import com.kidsplay.app.data.local.entities.ContentEntity;
import com.kidsplay.app.databinding.ItemContentBinding;
import com.kidsplay.app.utils.AgeUtils;
import com.kidsplay.app.utils.ComicStorage;

import java.io.File;
import java.util.Locale;
import java.util.Objects;

/**
 * Danh sách nội dung dùng chung. Có onLongClick (chế độ phụ huynh): hiện thêm trạng thái
 * Xuất bản/Ẩn và dấu đồng bộ cloud. Không có onLongClick (chế độ trẻ): chỉ hiện thông tin cơ bản.
 */
public class ContentAdapter extends ListAdapter<ContentEntity, ContentAdapter.VH> {

    public interface OnItemClick { void onClick(ContentEntity item); }
    public interface OnItemLongClick { void onLongClick(ContentEntity item); }

    private final OnItemClick onClick;
    @Nullable private final OnItemLongClick onLongClick;

    public ContentAdapter(OnItemClick onClick, @Nullable OnItemLongClick onLongClick) {
        super(DIFF);
        this.onClick = onClick;
        this.onLongClick = onLongClick;
    }

    private static final DiffUtil.ItemCallback<ContentEntity> DIFF = new DiffUtil.ItemCallback<ContentEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull ContentEntity a, @NonNull ContentEntity b) {
            return a.id == b.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ContentEntity a, @NonNull ContentEntity b) {
            return a.title.equals(b.title) && a.minAge == b.minAge && a.maxAge == b.maxAge
                    && a.published == b.published && a.type.equals(b.type)
                    && Objects.equals(a.cloudId, b.cloudId);
        }
    };

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemContentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ContentEntity item = getItem(position);
        boolean video = ContentEntity.TYPE_VIDEO.equals(item.type);
        boolean comic = ContentEntity.TYPE_COMIC.equals(item.type);
        boolean parentMode = onLongClick != null;

        holder.binding.tvTitle.setText(item.title);

        // Ảnh bìa: trang đầu của truyện tranh; các loại khác dùng biểu tượng
        File cover = comic ? firstPage(item.filePath) : null;
        if (cover != null) {
            holder.binding.ivCover.setVisibility(View.VISIBLE);
            holder.binding.tvIcon.setVisibility(View.GONE);
            Glide.with(holder.binding.ivCover).load(cover).centerCrop().into(holder.binding.ivCover);
        } else {
            holder.binding.ivCover.setVisibility(View.GONE);
            Glide.with(holder.binding.ivCover).clear(holder.binding.ivCover);
            holder.binding.tvIcon.setVisibility(View.VISIBLE);
            holder.binding.tvIcon.setText(video ? "🎬" : comic ? "📚" : "📖");
        }

        String kind = video ? "Video" : comic ? "Truyện tranh" : "Truyện chữ";
        String detail = kind + " • " + AgeUtils.rangeLabel(item.minAge, item.maxAge);
        if (video && item.durationMs > 0) {
            long sec = item.durationMs / 1000;
            detail += String.format(Locale.getDefault(), " • %d:%02d", sec / 60, sec % 60);
        }
        if (comic && item.pageCount > 0) {
            detail += String.format(Locale.getDefault(), " • %d trang", item.pageCount);
        }
        if (parentMode) detail += item.cloudId != null ? " • ☁" : " • chỉ trong máy";
        holder.binding.tvDetail.setText(detail);

        if (parentMode) {
            holder.binding.tvStatus.setVisibility(View.VISIBLE);
            holder.binding.tvStatus.setText(item.published ? "Xuất bản" : "Ẩn");
            holder.binding.tvStatus.setBackgroundResource(item.published ? R.drawable.bg_badge_green : R.drawable.bg_badge_grey);
            holder.binding.tvStatus.setTextColor(holder.itemView.getContext()
                    .getColor(item.published ? R.color.kp_green_dark : R.color.kp_grey));
        } else {
            holder.binding.tvStatus.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> onClick.onClick(item));
        if (onLongClick != null) {
            holder.itemView.setOnLongClickListener(v -> {
                onLongClick.onLongClick(item);
                return true;
            });
        } else {
            holder.itemView.setOnLongClickListener(null);
        }
    }

    private static File firstPage(String folder) {
        if (folder == null) return null;
        File[] pages = new File(folder).listFiles((d, n) ->
                n.toLowerCase(Locale.ROOT).matches(".+\\.(jpg|jpeg|png|webp|gif)"));
        if (pages == null || pages.length == 0) return null;
        File first = pages[0];
        for (File f : pages) {
            if (ComicStorage.naturalCompare(f.getName(), first.getName()) < 0) first = f;
        }
        return first;
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemContentBinding binding;

        VH(ItemContentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
