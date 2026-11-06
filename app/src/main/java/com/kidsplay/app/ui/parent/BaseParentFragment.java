package com.kidsplay.app.ui.parent;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/** Lớp cha cho các Fragment khu Phụ huynh: quyết định có hiện thanh điều hướng dưới hay không. */
public abstract class BaseParentFragment extends Fragment {

    /** Các form (thêm hồ sơ, thêm truyện) ẩn thanh điều hướng dưới, giống thiết kế. */
    protected boolean showsBottomNav() {
        return true;
    }

    @NonNull
    protected ParentDashboardActivity host() {
        return (ParentDashboardActivity) requireActivity();
    }

    @Override
    public void onResume() {
        super.onResume();
        host().setBottomNavVisible(showsBottomNav());
    }

    /** Nút quay lại trên màn hình: bấm giống nút Back của hệ thống. */
    protected void goBack() {
        requireActivity().getOnBackPressedDispatcher().onBackPressed();
    }
}
