package com.example.zhinongbao.data;

/* ============================================================
 * 【数据库 / Database】SQLite 数据库总管（整个 App 数据的「仓库地基」）
 * ============================================================
 * 这个文件是干什么的：
 *   它负责「创建数据库、建所有的表、版本升级、塞入初始演示数据」。
 *   App 里所有业务数据（用户、商品、订单、文章、聊天、采购…）都存在这一个
 *   名为 zhinongbao.db 的 SQLite 数据库里。
 *
 * 关键概念：
 *   - SQLite：手机自带的小型数据库，数据以「表(table)」形式保存，类似 Excel 表格。
 *   - SQLiteOpenHelper：安卓官方帮手类，继承它就能管理数据库的创建和升级。
 *   - 单例 getInstance：全 App 只创建一个数据库对象，避免重复打开浪费资源。
 *   - 三个核心回调方法（系统自动调）：
 *       onCreate ：数据库第一次创建时调用 → 在这里 CREATE TABLE 建好所有表。
 *       onUpgrade：DB_VERSION 变大时调用 → 用 ALTER/CREATE 做「数据迁移」，
 *                  保证老用户升级 App 后不丢数据。
 *       onOpen   ：每次打开数据库都会调 → 这里补充「种子数据」（admin 账号、示例商品等）。
 *   - DB_VERSION = 20：数据库结构改一次就把这个号 +1，系统据此触发 onUpgrade。
 *
 * 注意：业务代码一般不直接用本类，而是通过 ZhiNongBaoProvider（ContentProvider）
 *       间接读写。本类只管「建库建表和初始化」。
 *
 * 提示：在 IDE 里搜索「数据库」可看数据层文件（本类 + ZhiNongBaoProvider）。
 * ============================================================ */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLite 数据库帮助类，管理全部业务数据表。
 * 替代原有的 SharedPreferences 持久化方案。
 */
public class AppDatabase extends SQLiteOpenHelper {

        private static final String DB_NAME = "zhinongbao.db";  // 数据库文件名
        private static final int DB_VERSION = 24;               // 数据库版本号（改表结构就 +1）

        private static AppDatabase instance;                    // 单例对象（全 App 共用一个）

        // 获取数据库单例：第一次调用时创建，之后直接返回同一个对象
        public static AppDatabase getInstance(Context ctx) {
                if (instance == null)
                        instance = new AppDatabase(ctx.getApplicationContext());
                return instance;
        }

        // 私有构造方法：禁止外部直接 new，必须通过 getInstance（保证单例）
        private AppDatabase(Context ctx) {
                super(ctx, DB_NAME, null, DB_VERSION);
        }

        // onOpen：每次打开数据库都会调用。这里在「可写」时补齐种子数据（admin、示例商品等）
        @Override
        public void onOpen(SQLiteDatabase db) {
                super.onOpen(db);
                if (!db.isReadOnly()) {
                        ensureSeedData(db);
                }
        }

        // onCreate：数据库「第一次」创建时调用，在这里用 SQL 语句把所有表建好
        @Override
        public void onCreate(SQLiteDatabase db) {
                // 用户表（含昵称和头像）
                db.execSQL("CREATE TABLE users (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "username TEXT UNIQUE NOT NULL," +
                                "password TEXT NOT NULL," +
                                "nickname TEXT," +
                                "avatar_uri TEXT," +
                                "signature TEXT," +
                                "phone TEXT UNIQUE," +
                                "store_name TEXT," +
                                "store_phone TEXT," +
                                "role INTEGER DEFAULT 0)");

                // 文章表
                db.execSQL("CREATE TABLE articles (" +
                                "id INTEGER PRIMARY KEY," +
                                "title TEXT NOT NULL," +
                                "content TEXT NOT NULL," +
                                "author TEXT NOT NULL," +
                                "time TEXT NOT NULL," +
                                "read_count INTEGER DEFAULT 0," +
                                "cover_uri TEXT," +
                                "category TEXT DEFAULT '热点新闻')");

                // 商品表
                db.execSQL("CREATE TABLE products (" +
                                "id INTEGER PRIMARY KEY," +
                                "name TEXT NOT NULL," +
                                "`desc` TEXT NOT NULL," +
                                "cover_uri TEXT," +
                                "price REAL NOT NULL," +
                                "category TEXT DEFAULT '推荐'," +
                                "brand TEXT," +
                                "origin TEXT," +
                                "spec TEXT," +
                                "package_type TEXT," +
                                "view_count INTEGER DEFAULT 0," +
                                "status INTEGER DEFAULT 0," +
                                "seller TEXT)");

                // 购物车（每用户独立）
                db.execSQL("CREATE TABLE cart (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "username TEXT NOT NULL," +
                                "product_id INTEGER NOT NULL," +
                                "name TEXT NOT NULL," +
                                "price REAL NOT NULL," +
                                "quantity INTEGER NOT NULL DEFAULT 1," +
                                "UNIQUE(username, product_id))");

                // 订单表
                db.execSQL("CREATE TABLE orders (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "order_id TEXT UNIQUE NOT NULL," +
                                "username TEXT NOT NULL," +
                                "product_id INTEGER NOT NULL," +
                                "name TEXT NOT NULL," +
                                "price REAL NOT NULL," +
                                "quantity INTEGER NOT NULL," +
                                "time TEXT NOT NULL," +
                                "status TEXT NOT NULL," +
                                "seller TEXT," +
                                "order_type TEXT DEFAULT 'retail'," +
                                "purchase_request_id INTEGER DEFAULT -1," +
                                "ship_type TEXT DEFAULT 'express'," +
                                "ship_name TEXT," +
                                "ship_no TEXT," +
                                "ship_phone TEXT," +
                                "proof_images TEXT," +
                                "unit_price REAL DEFAULT 0," +
                                "discount REAL DEFAULT 0," +
                                "refund_amount REAL DEFAULT 0," +
                                "refund_reason TEXT," +
                                "refund_requested_at INTEGER DEFAULT 0," +
                                "refund_previous_status TEXT," +
                                "completed_at INTEGER DEFAULT 0," +
                                "receiver_name TEXT," +
                                "receiver_phone TEXT," +
                                "receiver_address TEXT)");

                db.execSQL("CREATE TABLE addresses (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "username TEXT NOT NULL," +
                                "receiver_name TEXT NOT NULL," +
                                "phone TEXT NOT NULL," +
                                "address TEXT NOT NULL," +
                                "is_default INTEGER DEFAULT 0," +
                                "tag TEXT)");

                // 文章点赞表（UNIQUE 防止重复）
                db.execSQL("CREATE TABLE article_likes (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "username TEXT NOT NULL," +
                                "article_id INTEGER NOT NULL," +
                                "UNIQUE(username, article_id))");

                db.execSQL("CREATE TABLE circle_likes (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "username TEXT NOT NULL," +
                                "article_id INTEGER NOT NULL," +
                                "UNIQUE(username, article_id))");

                db.execSQL("CREATE TABLE circle_favorites (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "username TEXT NOT NULL," +
                                "article_id INTEGER NOT NULL," +
                                "UNIQUE(username, article_id))");

                db.execSQL("CREATE TABLE product_favorites (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "username TEXT NOT NULL," +
                                "product_id INTEGER NOT NULL," +
                                "UNIQUE(username, product_id))");

                db.execSQL("CREATE TABLE product_footprints (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "username TEXT NOT NULL," +
                                "product_id INTEGER NOT NULL," +
                                "viewed_at INTEGER NOT NULL," +
                                "UNIQUE(username, product_id))");

                db.execSQL("CREATE TABLE store_footprints (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "username TEXT NOT NULL," +
                                "seller TEXT NOT NULL," +
                                "viewed_at INTEGER NOT NULL," +
                                "UNIQUE(username, seller))");

                // 评论表
                db.execSQL("CREATE TABLE comments (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "article_id INTEGER NOT NULL," +
                                "username TEXT NOT NULL," +
                                "content TEXT NOT NULL," +
                                "time TEXT NOT NULL)");

                // 评论点赞表
                db.execSQL("CREATE TABLE comment_likes (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "username TEXT NOT NULL," +
                                "comment_id INTEGER NOT NULL," +
                                "UNIQUE(username, comment_id))");

                // 关注关系表
                db.execSQL("CREATE TABLE follows (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "follower TEXT NOT NULL," +
                                "`following` TEXT NOT NULL," +
                                "UNIQUE(follower, `following`))");

                // 商品评价表
                db.execSQL("CREATE TABLE product_comments (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "product_id INTEGER NOT NULL," +
                                "username TEXT NOT NULL," +
                                "content TEXT NOT NULL," +
                                "images TEXT," +
                                "time TEXT NOT NULL)");

                // 私信聊天表
                //   recalled        ：是否已撤回（1=已撤回，内容清空，双方都只看到「撤回了一条消息」）
                //   deleted_by_from ：发送方是否在自己这一侧删除了本条（删除只影响自己，对方仍可见）
                //   deleted_by_to   ：接收方是否在自己这一侧删除了本条
                db.execSQL("CREATE TABLE chat_messages (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "from_user TEXT NOT NULL," +
                                "to_user TEXT NOT NULL," +
                                "content TEXT NOT NULL," +
                                "timestamp INTEGER NOT NULL," +
                                "is_read INTEGER DEFAULT 0," +
                                "recalled INTEGER DEFAULT 0," +
                                "deleted_by_from INTEGER DEFAULT 0," +
                                "deleted_by_to INTEGER DEFAULT 0)");
                db.execSQL("CREATE INDEX idx_chat_users ON chat_messages(from_user, to_user)");

                // 采购需求表
                db.execSQL("CREATE TABLE purchase_requests (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "buyer_user TEXT NOT NULL," +
                                "product_name TEXT NOT NULL," +
                                "category TEXT," +
                                "quantity REAL NOT NULL," +
                                "unit TEXT NOT NULL," +
                                "target_price REAL NOT NULL," +
                                "description TEXT," +
                                "images TEXT," +
                                "timestamp INTEGER NOT NULL)");

                // 商家报价表
                db.execSQL("CREATE TABLE purchase_quotes (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "request_id INTEGER NOT NULL," +
                                "seller_user TEXT NOT NULL," +
                                "price REAL NOT NULL," +
                                "description TEXT," +
                                "images TEXT," +
                                "timestamp INTEGER NOT NULL," +
                                "status TEXT DEFAULT 'pending'," +
                                "reply_desc TEXT," +
                                "UNIQUE(request_id, seller_user))");

                // 创建常用查询索引
                db.execSQL("CREATE INDEX idx_articles_author ON articles(author)");
                db.execSQL("CREATE INDEX idx_comments_article ON comments(article_id)");
                db.execSQL("CREATE INDEX idx_article_likes_article ON article_likes(article_id)");
                db.execSQL("CREATE INDEX idx_circle_likes_article ON circle_likes(article_id)");
                db.execSQL("CREATE INDEX idx_circle_favorites_user ON circle_favorites(username, id)");
                db.execSQL("CREATE INDEX idx_follows_following ON follows(`following`)");
                db.execSQL("CREATE INDEX idx_product_comments_product ON product_comments(product_id)");
                db.execSQL("CREATE INDEX idx_product_footprints_user ON product_footprints(username, viewed_at)");
                db.execSQL("CREATE INDEX idx_store_footprints_user ON store_footprints(username, viewed_at)");
        }

        // onUpgrade：App 升级、数据库版本号变大时调用，逐版本「打补丁」升级表结构。
        // 写法套路：if (oldVersion < N) { ...第 N 版新增的表/字段... }
        // 这样无论用户从哪个旧版本升上来，都会依次执行需要的改动，老数据不丢失。
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                if (oldVersion < 2) {
                        db.execSQL("ALTER TABLE users ADD COLUMN signature TEXT");
                        db.execSQL("ALTER TABLE users ADD COLUMN phone TEXT");
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_phone ON users(phone)");
                }
                if (oldVersion < 3) {
                        db.execSQL("ALTER TABLE products ADD COLUMN cover_uri TEXT");
                }
                if (oldVersion < 4) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS product_comments (" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                        "product_id INTEGER NOT NULL," +
                                        "username TEXT NOT NULL," +
                                        "content TEXT NOT NULL," +
                                        "images TEXT," +
                                        "time TEXT NOT NULL)");
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_product_comments_product ON product_comments(product_id)");
                }
                if (oldVersion < 5) {
                        db.execSQL("ALTER TABLE users ADD COLUMN role INTEGER DEFAULT 0");
                }
                if (oldVersion < 6) {
                        db.execSQL("ALTER TABLE articles ADD COLUMN category TEXT DEFAULT '热点新闻'");
                }
                if (oldVersion < 7) {
                        db.execSQL("ALTER TABLE products ADD COLUMN category TEXT DEFAULT '推荐'");
                }
                if (oldVersion < 8) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS chat_messages (" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                        "from_user TEXT NOT NULL," +
                                        "to_user TEXT NOT NULL," +
                                        "content TEXT NOT NULL," +
                                        "timestamp INTEGER NOT NULL," +
                                        "is_read INTEGER DEFAULT 0)");
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chat_users ON chat_messages(from_user, to_user)");
                }
                if (oldVersion < 9) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS purchase_requests (" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                        "buyer_user TEXT NOT NULL," +
                                        "product_name TEXT NOT NULL," +
                                        "category TEXT," +
                                        "quantity REAL NOT NULL," +
                                        "unit TEXT NOT NULL," +
                                        "target_price REAL NOT NULL," +
                                        "description TEXT," +
                                        "timestamp INTEGER NOT NULL)");
                        db.execSQL("CREATE TABLE IF NOT EXISTS purchase_quotes (" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                        "request_id INTEGER NOT NULL," +
                                        "seller_user TEXT NOT NULL," +
                                        "price REAL NOT NULL," +
                                        "description TEXT," +
                                        "timestamp INTEGER NOT NULL," +
                                        "UNIQUE(request_id, seller_user))");
                }
                if (oldVersion < 10) {
                        db.execSQL("ALTER TABLE products ADD COLUMN seller TEXT");
                }
                if (oldVersion < 11) {
                        db.execSQL("ALTER TABLE orders ADD COLUMN seller TEXT");
                        db.execSQL("ALTER TABLE orders ADD COLUMN order_type TEXT DEFAULT 'retail'");
                        db.execSQL("ALTER TABLE orders ADD COLUMN purchase_request_id INTEGER DEFAULT -1");
                        db.execSQL("ALTER TABLE orders ADD COLUMN ship_type TEXT DEFAULT 'express'");
                        db.execSQL("ALTER TABLE orders ADD COLUMN ship_name TEXT");
                        db.execSQL("ALTER TABLE orders ADD COLUMN ship_no TEXT");
                        db.execSQL("ALTER TABLE orders ADD COLUMN ship_phone TEXT");
                        db.execSQL("ALTER TABLE orders ADD COLUMN proof_images TEXT");
                        db.execSQL("ALTER TABLE orders ADD COLUMN unit_price REAL DEFAULT 0");
                        db.execSQL("ALTER TABLE orders ADD COLUMN discount REAL DEFAULT 0");
                        db.execSQL("ALTER TABLE orders ADD COLUMN refund_amount REAL DEFAULT 0");
                        db.execSQL("ALTER TABLE orders ADD COLUMN refund_reason TEXT");
                        db.execSQL("ALTER TABLE purchase_quotes ADD COLUMN status TEXT DEFAULT 'pending'");
                        db.execSQL("ALTER TABLE purchase_quotes ADD COLUMN reply_desc TEXT");
                }
                if (oldVersion < 12) {
                        db.execSQL("ALTER TABLE users ADD COLUMN store_name TEXT");
                        db.execSQL("ALTER TABLE users ADD COLUMN store_phone TEXT");
                        db.execSQL("ALTER TABLE products ADD COLUMN view_count INTEGER DEFAULT 0");
                        db.execSQL("CREATE TABLE IF NOT EXISTS product_favorites (" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                        "username TEXT NOT NULL," +
                                        "product_id INTEGER NOT NULL," +
                                        "UNIQUE(username, product_id))");
                        db.execSQL("CREATE TABLE IF NOT EXISTS product_footprints (" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                        "username TEXT NOT NULL," +
                                        "product_id INTEGER NOT NULL," +
                                        "viewed_at INTEGER NOT NULL," +
                                        "UNIQUE(username, product_id))");
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_product_footprints_user ON product_footprints(username, viewed_at)");
                }
                if (oldVersion < 13) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS store_footprints (" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                        "username TEXT NOT NULL," +
                                        "seller TEXT NOT NULL," +
                                        "viewed_at INTEGER NOT NULL," +
                                        "UNIQUE(username, seller))");
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_store_footprints_user ON store_footprints(username, viewed_at)");
                }
                if (oldVersion < 14) {
                        db.execSQL("ALTER TABLE orders ADD COLUMN refund_requested_at INTEGER DEFAULT 0");
                }
                if (oldVersion < 15) {
                        db.execSQL("ALTER TABLE orders ADD COLUMN refund_previous_status TEXT");
                }
                if (oldVersion < 16) {
                        db.execSQL("ALTER TABLE orders ADD COLUMN receiver_name TEXT");
                        db.execSQL("ALTER TABLE orders ADD COLUMN receiver_phone TEXT");
                        db.execSQL("ALTER TABLE orders ADD COLUMN receiver_address TEXT");
                        db.execSQL("CREATE TABLE IF NOT EXISTS addresses (" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                        "username TEXT NOT NULL," +
                                        "receiver_name TEXT NOT NULL," +
                                        "phone TEXT NOT NULL," +
                                        "address TEXT NOT NULL," +
                                        "is_default INTEGER DEFAULT 0)");
                }
                if (oldVersion < 19) {
                        db.execSQL("ALTER TABLE orders ADD COLUMN completed_at INTEGER DEFAULT 0");
                }
                if (oldVersion < 17) {
                        db.execSQL("ALTER TABLE products ADD COLUMN brand TEXT");
                        db.execSQL("ALTER TABLE products ADD COLUMN origin TEXT");
                        db.execSQL("ALTER TABLE products ADD COLUMN spec TEXT");
                        db.execSQL("ALTER TABLE products ADD COLUMN package_type TEXT");
                }
                if (oldVersion < 18) {
                        db.execSQL("ALTER TABLE purchase_quotes ADD COLUMN images TEXT");
                }
                if (oldVersion < 20) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS circle_likes (" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                        "username TEXT NOT NULL," +
                                        "article_id INTEGER NOT NULL," +
                                        "UNIQUE(username, article_id))");
                        db.execSQL("CREATE TABLE IF NOT EXISTS circle_favorites (" +
                                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                        "username TEXT NOT NULL," +
                                        "article_id INTEGER NOT NULL," +
                                        "UNIQUE(username, article_id))");
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_circle_likes_article ON circle_likes(article_id)");
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_circle_favorites_user ON circle_favorites(username, id)");
                        db.execSQL("INSERT OR IGNORE INTO circle_likes(username, article_id) " +
                                        "SELECT username, article_id FROM article_likes " +
                                        "WHERE article_id IN (SELECT id FROM articles WHERE category='农友圈')");
                        db.execSQL("DELETE FROM article_likes " +
                                        "WHERE article_id IN (SELECT id FROM articles WHERE category='农友圈')");
                }
                if (oldVersion < 21) {
                        // 聊天消息新增「撤回」「双方各自删除」标记，支持撤回与单侧删除
                        db.execSQL("ALTER TABLE chat_messages ADD COLUMN recalled INTEGER DEFAULT 0");
                        db.execSQL("ALTER TABLE chat_messages ADD COLUMN deleted_by_from INTEGER DEFAULT 0");
                        db.execSQL("ALTER TABLE chat_messages ADD COLUMN deleted_by_to INTEGER DEFAULT 0");
                }
                if (oldVersion < 22) {
                        // 收货地址新增「地址标签」字段（家/公司/学校…）
                        db.execSQL("ALTER TABLE addresses ADD COLUMN tag TEXT");
                }
                if (oldVersion < 23) {
                        // 采购需求新增「需求图片」字段（买家发布时上传，逗号分隔多张）
                        db.execSQL("ALTER TABLE purchase_requests ADD COLUMN images TEXT");
                }
                if (oldVersion < 24) {
                        // 商品新增「上下架状态」字段（0=在售，1=已下架）；下架改为软状态，便于重新上架
                        db.execSQL("ALTER TABLE products ADD COLUMN status INTEGER DEFAULT 0");
                }
        }

        // 种子数据：保证 App 一打开就有可用的演示账号/商品/文章，方便登录体验。
        // beginTransaction/endTransaction = 「事务」：把多步操作打包，要么全成功要么全回滚，
        // 中途出错不会留下半截脏数据（类似「批处理，保证整体一致」）。
        private void ensureSeedData(SQLiteDatabase db) {
                db.beginTransaction();
                try {
                        ensureAdminUser(db);            // 内置管理员账号 admin/123456
                        ensureTestSellerUser(db);       // 内置测试卖家账号
                        ensureDefaultProducts(db);      // 内置 10 个示例商品
                        ensureDefaultArticles(db);      // 内置 5 篇示例文章
                        ensureExpiredReturnTestOrder(db); // 内置一条「已完成」测试订单
                        ensureAdminSalesDemoOrders(db);   // 内置 admin 的本月/往月已完成订单，验证营收统计
                        db.setTransactionSuccessful(); // 标记事务成功（不调用则会整体回滚）
                } finally {
                        db.endTransaction();
                }
        }

        // 保证存在管理员账号 admin（密码 123456，角色 2=买卖兼具）。
        // insertWithOnConflict(...CONFLICT_IGNORE)：已存在就忽略不报错；随后再 update 保证信息最新。
        private void ensureAdminUser(SQLiteDatabase db) {
                ContentValues values = new ContentValues();
                values.put("username", "admin");
                values.put("password", "123456");
                values.put("nickname", "admin");
                values.put("phone", "13800138000");
                values.put("store_name", "admin的店铺");
                values.put("store_phone", "13800138000");
                values.put("role", 2);
                db.insertWithOnConflict("users", null, values, SQLiteDatabase.CONFLICT_IGNORE);

                ContentValues update = new ContentValues();
                update.put("password", "123456");
                update.put("role", 2);
                update.put("nickname", "admin");
                update.put("store_name", "admin的店铺");
                update.put("store_phone", "13800138000");
                db.update("users", update, "username=?", new String[] { "admin" });
        }

        // 保证存在测试卖家账号 test_seller（角色 1=卖家），方便体验卖家相关功能
        private void ensureTestSellerUser(SQLiteDatabase db) {
                ContentValues values = new ContentValues();
                values.put("username", "test_seller");
                values.put("password", "123456");
                values.put("nickname", "测试卖家");
                values.put("phone", "13900139000");
                values.put("store_name", "测试卖家的店铺");
                values.put("store_phone", "13900139000");
                values.put("role", 1);
                db.insertWithOnConflict("users", null, values, SQLiteDatabase.CONFLICT_IGNORE);

                ContentValues update = new ContentValues();
                update.put("password", "123456");
                update.put("role", 1);
                update.put("nickname", "测试卖家");
                update.put("phone", "13900139000");
                update.put("store_name", "测试卖家的店铺");
                update.put("store_phone", "13900139000");
                db.update("users", update, "username=?", new String[] { "test_seller" });
        }

        // 插入 10 个示例商品（大米、木耳、蜂蜜……），让商城一打开就有货可看
        private void ensureDefaultProducts(SQLiteDatabase db) {
                insertProduct(db, 1, "东北大米（5kg）", "东北黑土地稻米，米香浓郁，适合家庭日常主食。", 45,
                                "米面粮油", "十月稻田", "东北", "5kg", "袋装");
                insertProduct(db, 2, "有机黑木耳（250g）", "肉厚爽脆，泡发率高，适合凉拌和炖汤。", 38,
                                "水果蔬菜", "北货郎", "黑龙江", "250g", "袋装");
                insertProduct(db, 3, "农家蜂蜜（500g）", "农家成熟蜜，口感清甜，瓶装便携。", 68,
                                "推荐", "深山土蜜", "山东", "500g", "瓶装");
                insertProduct(db, 4, "绿色蔬菜礼盒", "精选时令新鲜蔬菜组合，产自有机农场，当日采摘，新鲜直达。", 99,
                                "水果蔬菜", "支农宝精选", "本地农场", "礼盒装", "礼盒");
                insertProduct(db, 5, "优质冬虫夏草（10g）", "精选干货，适合煲汤滋补。", 880,
                                "推荐", "高原甄选", "青海", "10g", "礼盒");
                insertProduct(db, 6, "农家红薯（5kg）", "软糯香甜，适合蒸烤煮粥。", 29.9,
                                "水果蔬菜", "农家直供", "山东", "5kg", "箱装");
                insertProduct(db, 7, "新鲜铁棍山药（2.5kg）", "粉糯细腻，适合煲汤和清炒。", 55,
                                "水果蔬菜", "正宗铁棍山药", "河南焦作", "2.5kg", "箱装");
                insertProduct(db, 8, "野生羊肚菌（100g）", "香味浓郁，适合炖汤和宴席菜。", 128,
                                "水果蔬菜", "山珍优选", "云南", "100g", "袋装");
                insertProduct(db, 9, "鲜货鹿茸菇（250g）", "口感脆嫩，适合火锅和炒菜。", 45,
                                "水果蔬菜", "鲜菌直采", "福建", "250g", "袋装");
                insertProduct(db, 10, "散养土鹅蛋（10枚）", "农家散养鹅蛋，蛋香浓郁。", 65,
                                "推荐", "农家散养", "山东", "10枚", "盒装");

                ContentValues update = new ContentValues();
                update.put("seller", "admin");
                db.update("products", update, "(seller IS NULL OR seller='') AND id BETWEEN 1 AND 10", null);
        }

        // 插入一条「8天前已完成」的测试订单：用来验证「超过7天只能联系客服退货」等逻辑，
        // 同时让卖家销售流水有数据可看。8天前 = 当前时间 - 8×24小时（毫秒）。
        private void ensureExpiredReturnTestOrder(SQLiteDatabase db) {
                long completedAt = System.currentTimeMillis() - 8L * 24 * 60 * 60 * 1000;
                ContentValues values = new ContentValues();
                values.put("order_id", "TEST_EXPIRED_RETURN_001");
                values.put("username", "admin");
                values.put("product_id", 7);
                values.put("name", "新鲜铁棍山药（2.5kg）");
                values.put("price", 55.00);
                values.put("quantity", 1);
                values.put("time", "2026-05-14 10:00");
                values.put("status", "completed");
                values.put("seller", "test_seller");
                values.put("order_type", "retail");
                values.put("purchase_request_id", -1);
                values.put("ship_type", "express");
                values.put("ship_name", "顺丰快递");
                values.put("ship_no", "SFTEST20260514001");
                values.put("ship_phone", "");
                values.put("proof_images", "");
                values.put("unit_price", 55.00);
                values.put("discount", 0);
                values.put("refund_amount", 0);
                values.put("refund_reason", "");
                values.put("refund_requested_at", 0);
                values.putNull("refund_previous_status");
                values.put("receiver_name", "测试用户");
                values.put("receiver_phone", "13800138000");
                values.put("receiver_address", "测试地址：用于验证超过七天联系客服按钮，并检查累计销售流水");
                values.put("completed_at", completedAt);
                db.insertWithOnConflict("orders", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }

        // 内置 admin 作为「卖家」的几笔【已完成】订单，用来验证「本月营收」与「累计营收」两个统计：
        //   - 本月 2 笔：同时计入「本月营收」和「累计营收」；
        //   - 往月 2 笔：只计入「累计营收」。
        // 这样「累计营收」会明显大于「本月营收」，可直观证明两个统计各自生效。
        // 日期按当前系统时间动态生成，保证「本月」永远落在当月（营收按 time 的 yyyy-MM 过滤）。
        private void ensureAdminSalesDemoOrders(SQLiteDatabase db) {
                long demoTodayBase = todayAtMillis(9, 0);
                long firstOfThisMonth = monthOffsetMillis(0, 1, 10, 0);   // 本月 1 号
                long lastMonth = monthOffsetMillis(1, 15, 14, 0);         // 上月 15 号
                long twoMonthsAgo = monthOffsetMillis(2, 10, 11, 0);      // 上上月 10 号

                // 本月（含今日）：计入「今日/本月/累计营收」，同时让「订单流水」有足够条数演示分页
                insertAdminSale(db, "DEMO_ADMIN_SALE_M1", 3, "农家蜂蜜（500g）", 68.00, 2, demoTodayBase);
                insertAdminSale(db, "DEMO_ADMIN_SALE_T1", 1, "东北大米（5kg）", 45.00, 1,
                                demoTodayBase + 5L * 60 * 1000);
                insertAdminSale(db, "DEMO_ADMIN_SALE_T2", 2, "有机黑木耳（250g）", 38.00, 2,
                                demoTodayBase + 10L * 60 * 1000);
                insertAdminSale(db, "DEMO_ADMIN_SALE_T3", 6, "农家红薯（5kg）", 29.90, 1,
                                demoTodayBase + 15L * 60 * 1000);
                insertAdminSale(db, "DEMO_ADMIN_SALE_T4", 9, "鲜货鹿茸菇（250g）", 45.00, 1,
                                demoTodayBase + 20L * 60 * 1000);
                insertAdminSale(db, "DEMO_ADMIN_SALE_M2", 10, "散养土鹅蛋（10枚）", 65.00, 1, firstOfThisMonth);

                // 往月：只计入「累计营收」
                insertAdminSale(db, "DEMO_ADMIN_SALE_P1", 1, "东北大米（5kg）", 45.00, 3, lastMonth);
                insertAdminSale(db, "DEMO_ADMIN_SALE_P2", 8, "野生羊肚菌（100g）", 128.00, 1, twoMonthsAgo);
        }

        // 生成「相对当前时间往前 monthsAgo 个月、指定日/时/分」的毫秒时间戳
        private long monthOffsetMillis(int monthsAgo, int dayOfMonth, int hour, int minute) {
                java.util.Calendar c = java.util.Calendar.getInstance();
                c.add(java.util.Calendar.MONTH, -monthsAgo);
                c.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth);
                c.set(java.util.Calendar.HOUR_OF_DAY, hour);
                c.set(java.util.Calendar.MINUTE, minute);
                c.set(java.util.Calendar.SECOND, 0);
                c.set(java.util.Calendar.MILLISECOND, 0);
                return c.getTimeInMillis();
        }

        private long todayAtMillis(int hour, int minute) {
                java.util.Calendar c = java.util.Calendar.getInstance();
                c.set(java.util.Calendar.HOUR_OF_DAY, hour);
                c.set(java.util.Calendar.MINUTE, minute);
                c.set(java.util.Calendar.SECOND, 0);
                c.set(java.util.Calendar.MILLISECOND, 0);
                return c.getTimeInMillis();
        }

        // 工具方法：插入一笔 admin 卖出的【已完成】零售订单（演示营收用）
        private void insertAdminSale(SQLiteDatabase db, String orderId, int productId, String name,
                        double unitPrice, int quantity, long timeMillis) {
                String time = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                                .format(new java.util.Date(timeMillis));
                ContentValues values = new ContentValues();
                values.put("order_id", orderId);
                values.put("username", "test_seller");   // 买家（示例）
                values.put("product_id", productId);
                values.put("name", name);
                values.put("price", unitPrice);
                values.put("quantity", quantity);
                values.put("time", time);
                values.put("status", "completed");
                values.put("seller", "admin");           // 卖家=admin，用于演示 admin 的本月/累计营收
                values.put("order_type", "retail");
                values.put("purchase_request_id", -1);
                values.put("ship_type", "express");
                values.put("ship_name", "顺丰快递");
                values.put("ship_no", "SF" + orderId);
                values.put("ship_phone", "");
                values.put("proof_images", "");
                values.put("unit_price", unitPrice);
                values.put("discount", 0);
                values.put("refund_amount", 0);
                values.put("refund_reason", "");
                values.put("refund_requested_at", 0);
                values.putNull("refund_previous_status");
                values.put("receiver_name", "示例买家");
                values.put("receiver_phone", "13900139000");
                values.put("receiver_address", "演示地址：用于验证本月营收与累计营收统计");
                values.put("completed_at", timeMillis);
                db.insertWithOnConflict("orders", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }

        // 工具方法：把一件商品的各字段装进 ContentValues 后插入 products 表（已存在则忽略）
        private void insertProduct(SQLiteDatabase db, int id, String name, String desc, double price, String category,
                        String brand, String origin, String spec, String packageType) {
                ContentValues values = new ContentValues();
                values.put("id", id);
                values.put("name", name);
                values.put("desc", desc);
                values.put("cover_uri", "");
                values.put("price", price);
                values.put("category", category);
                values.put("brand", brand);
                values.put("origin", origin);
                values.put("spec", spec);
                values.put("package_type", packageType);
                values.put("view_count", 0);
                values.put("seller", "admin");
                db.insertWithOnConflict("products", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        }

        // 插入 5 篇示例文章（春耕、果树管理等），让「头条/农技学堂」一打开就有内容
        private void ensureDefaultArticles(SQLiteDatabase db) {
                insertArticle(db, 1, "春耕备耕正当时，科学管理促增收",
                                "当前正值春耕关键期，建议农户根据土壤墒情安排播种，做好底肥管理和病虫害预防。通过测土配方、合理密植和水肥一体化，可以有效提升作物长势。",
                                "热点新闻");
                insertArticle(db, 2, "果树花期管理要点",
                                "果树花期需重点关注授粉、疏花疏果和水肥供应。遇到低温天气要及时采取防寒措施，花后根据坐果情况调整枝梢负载。",
                                "专家咨询");
                insertArticle(db, 3, "支农宝助力农产品上行",
                                "支农宝持续连接乡村产地与城市消费市场，帮助农户展示优质农产品、对接采购需求，推动农产品销售更高效。",
                                "支农宝新闻");
                insertArticle(db, 4, "返乡创业可以从这些农业项目开始",
                                "特色种植、农产品初加工、乡村电商和采摘体验都是较适合小规模起步的方向。建议先做市场调研，再逐步扩大投入。",
                                "创业项目");
                insertArticle(db, 5, "农产品保鲜运输小技巧",
                                "叶菜类应注意预冷和保湿，果品类要避免挤压，干货类需防潮密封。合理包装能减少运输损耗，提升到货品质。",
                                "热点新闻");
        }

        // 工具方法：把一篇文章插入 articles 表（作者统一为 admin，已存在则忽略）
        private void insertArticle(SQLiteDatabase db, int id, String title, String content, String category) {
                ContentValues values = new ContentValues();
                values.put("id", id);
                values.put("title", title);
                values.put("content", content);
                values.put("author", "admin");
                values.put("time", "2026-05-22 09:00");
                values.put("read_count", 0);
                values.putNull("cover_uri");
                values.put("category", category);
                db.insertWithOnConflict("articles", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        }

        // 工具方法：判断某张表是不是空的（查一行看有没有数据）。当前暂未使用，故标注 unused。
        @SuppressWarnings("unused")
        private boolean isTableEmpty(SQLiteDatabase db, String table) {
                try (Cursor cursor = db.rawQuery("SELECT 1 FROM " + table + " LIMIT 1", null)) {
                        return cursor == null || !cursor.moveToFirst();
                }
        }
}
