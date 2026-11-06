package com.kidsplay.app.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Một nội dung trong thư viện: video (file lưu trong bộ nhớ riêng của app)
 * hoặc truyện chữ (nội dung lưu thẳng trong cột body).
 */
@Entity(tableName = "contents")
public class ContentEntity {
    public static final String TYPE_VIDEO = "VIDEO";
    public static final String TYPE_STORY = "STORY";
    public static final String TYPE_COMIC = "COMIC";

    @PrimaryKey(autoGenerate = true)
    public long id = 0;
    public long parentId;   // phụ huynh sở hữu nội dung này
    public String type;
    public String title;
    public String body;        // nội dung truyện (STORY)
    public String filePath;    // file video (VIDEO) hoặc thư mục chứa các trang ảnh (COMIC)
    public int pageCount;      // số trang (COMIC)
    public String description;  // mô tả ngắn
    public String category;     // danh mục hiển thị
    public int maxAge = 99;     // độ tuổi tối đa phù hợp (minAge là tối thiểu)
    public boolean published = true; // false = ẩn, trẻ không thấy
    public String cloudId;     // id tài liệu trên Firestore; null = chưa đồng bộ lên cloud
    public int minAge;         // độ tuổi tối thiểu phù hợp
    public long durationMs;
    public long createdAt = System.currentTimeMillis();

    public ContentEntity(String type, String title, String body, String filePath, int minAge, long durationMs) {
        this.type = type;
        this.title = title;
        this.body = body;
        this.filePath = filePath;
        this.minAge = minAge;
        this.durationMs = durationMs;
    }
}
