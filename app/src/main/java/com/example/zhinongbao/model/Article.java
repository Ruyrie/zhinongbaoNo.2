package com.example.zhinongbao.model;

/* ============================================================
 * 【文章 / 农技学堂 / 头条 / Article】文章模型（Model，数据载体）
 * ============================================================
 *
 * 说明：
 *   - 末尾几个字段（authorNickname/authorAvatarUri）是「连表查询」时顺带带出来的
 *     作者信息，只为界面显示方便，不单独存数据库。
 *
 * 关于图片存储（coverUri 字段）：
 *   coverUri 一列同时存「封面图」和「内容配图」，约定如下：
 *     - 有专门上传封面：  "封面URI\n内容图1,内容图2,..."（换行 \n 分隔封面段与内容段）
 *     - 没有专门封面：    "内容图1,内容图2,..."（无换行；首张内容图兼作列表封面）
 *     - 没有任何图：       null
 *   用 getCoverImage()/getContentImages() 统一解析，避免各处自己 split 出错。
 *
 * 提示：在 IDE 里搜索「文章」可看文章相关文件（ArticleAdapter/文章详情/添加文章等）。
 * ============================================================ */

import java.util.ArrayList;
import java.util.List;

/** 文章模型 */
public class Article {
    public int id;          // 文章 id
    public String title;    // 标题
    public String content;  // 正文内容
    public String author;   // 作者用户名
    public String time;     // 发布时间
    public int readCount;   // 阅读量
    public String coverUri; // 封面图本地地址，null 表示使用默认图
    public String category; // 分类（如「热点新闻」）

    public boolean isDeleted; // 是否已被删除（占位标记）
    public String authorNickname; // 连表查询时附带的作者昵称（仅显示用）
    public String authorAvatarUri; // 连表查询时附带的作者头像（仅显示用）

    // 构造方法：创建文章对象，阅读量从 0 开始，分类默认「热点新闻」
    public Article(int id, String title, String content, String author, String time) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
        this.time = time;
        this.readCount = 0;
        this.category = "热点新闻";
    }

    /** 是否上传了「专门封面」（coverUri 里含换行分隔符即表示有独立封面段）。 */
    public boolean hasDedicatedCover() {
        return coverUri != null && coverUri.indexOf('\n') >= 0;
    }

    /**
     * 列表/缩略图用的封面图：
     *   有专门封面 → 用专门封面；没有专门封面 → 用第一张内容配图；都没有 → 返回 null。
     */
    public String getCoverImage() {
        if (coverUri == null || coverUri.isEmpty()) {
            return null;
        }
        int nl = coverUri.indexOf('\n');
        // 有换行：封面段在换行前；没换行：整串即内容段，取首张作封面
        String head = nl >= 0 ? coverUri.substring(0, nl) : coverUri;
        int comma = head.indexOf(',');
        String cover = (comma >= 0 ? head.substring(0, comma) : head).trim();
        return cover.isEmpty() ? null : cover;
    }

    /**
     * 详情页正文要展示的「内容配图」列表（不含专门封面）。
     *   有专门封面 → 只取换行后的内容段；没有专门封面 → 整串都是内容配图。
     */
    public List<String> getContentImages() {
        List<String> images = new ArrayList<>();
        if (coverUri == null || coverUri.isEmpty()) {
            return images;
        }
        int nl = coverUri.indexOf('\n');
        String contentPart = nl >= 0 ? coverUri.substring(nl + 1) : coverUri;
        for (String uri : contentPart.split(",")) {
            String trimmed = uri.trim();
            if (!trimmed.isEmpty()) {
                images.add(trimmed);
            }
        }
        return images;
    }
}
