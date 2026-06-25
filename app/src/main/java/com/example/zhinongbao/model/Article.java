package com.example.zhinongbao.model;

/* ============================================================
 * 【文章 / 农技学堂 / 头条 / Article】文章模型（Model，数据载体）
 * ============================================================
 * 这个文件是干什么的：
 *   装一篇文章/资讯：标题、正文、作者、时间、阅读量、封面、分类。
 *   用于「头条资讯」「农技学堂」「我的文章」等内容展示。
 *
 * 说明：
 *   - 末尾几个字段（authorNickname/authorAvatarUri）是「连表查询」时顺带带出来的
 *     作者信息，只为界面显示方便，不单独存数据库。
 *
 * 提示：在 IDE 里搜索「文章」可看文章相关文件（ArticleAdapter/文章详情/添加文章等）。
 * ============================================================ */

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
}
