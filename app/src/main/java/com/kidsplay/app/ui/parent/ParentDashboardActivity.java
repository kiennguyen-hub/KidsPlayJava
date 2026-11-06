package com.kidsplay.app.ui.parent;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.kidsplay.app.R;
import com.kidsplay.app.databinding.ActivityParentDashboardBinding;
import com.kidsplay.app.ui.auth.AuthActivity;
import com.kidsplay.app.utils.SessionManager;

/**
 * Khung chứa khu vực Phụ huynh. 4 tab: Trang chủ, Hồ sơ trẻ, Báo cáo, Cài đặt.
 * Các màn con (thông tin hồ sơ, thêm hồ sơ, quản lý nội dung...) được đẩy lên bằng navigateTo().
 */
public class ParentDashboardActivity extends AppCompatActivity {

    private ActivityParentDashboardBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Phải có phiên cục bộ VÀ phiên Firebase; thiếu một trong hai thì đăng nhập lại.
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn() || FirebaseAuth.getInstance().getCurrentUser() == null) {
            session.logout();
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        binding = ActivityParentDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment target;
            if (id == R.id.nav_home) {
                target = new HomeFragment();
            } else if (id == R.id.nav_kids) {
                target = new KidProfilesFragment();
            } else if (id == R.id.nav_reports) {
                target = new ReportsFragment();
            } else if (id == R.id.nav_settings) {
                target = new SettingsFragment();
            } else {
                return false;
            }
            // Đổi tab: xóa các màn con đang mở rồi hiện màn gốc của tab
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, target)
                    .commit();
            return true;
        });

        if (savedInstanceState == null) {
            binding.bottomNav.setSelectedItemId(R.id.nav_home);
        }

        // Thông báo trạng thái đồng bộ cloud (hiện ở mọi màn) và tự đồng bộ khi vào app
        ContentViewModel contentViewModel = new ViewModelProvider(this).get(ContentViewModel.class);
        contentViewModel.getCloudMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                contentViewModel.consumeCloudMessage();
            }
        });
        if (savedInstanceState == null) {
            contentViewModel.syncNow(true);
        }
    }

    /** Mở một màn con; nút Back quay lại màn trước. */
    public void navigateTo(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    public void selectTab(int menuId) {
        binding.bottomNav.setSelectedItemId(menuId);
    }

    /** Tô sáng một tab mà không đổi màn hình (ví dụ màn Thông tin hồ sơ thuộc tab Hồ sơ trẻ). */
    public void highlightTab(int menuId) {
        MenuItem item = binding.bottomNav.getMenu().findItem(menuId);
        if (item != null) item.setChecked(true);
    }

    public void setBottomNavVisible(boolean visible) {
        if (binding != null) binding.navContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
