package com.example.zhinongbao.data;

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

        private static final String DB_NAME = "zhinongbao.db";
        private static final int DB_VERSION = 19;

        private static AppDatabase instance;

        public static AppDatabase getInstance(Context ctx) {
                if (instance == null)
                        instance = new AppDatabase(ctx.getApplicationContext());
                return instance;
        }

        private AppDatabase(Context ctx) {
                super(ctx, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
                super.onOpen(db);
                if (!db.isReadOnly()) {
                        ensureSeedData(db);
                }
        }

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
                                "is_default INTEGER DEFAULT 0)");

                // 文章点赞表（UNIQUE 防止重复）
                db.execSQL("CREATE TABLE article_likes (" +
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
                db.execSQL("CREATE TABLE chat_messages (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "from_user TEXT NOT NULL," +
                                "to_user TEXT NOT NULL," +
                                "content TEXT NOT NULL," +
                                "timestamp INTEGER NOT NULL," +
                                "is_read INTEGER DEFAULT 0)");
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
                db.execSQL("CREATE INDEX idx_follows_following ON follows(`following`)");
                db.execSQL("CREATE INDEX idx_product_comments_product ON product_comments(product_id)");
                db.execSQL("CREATE INDEX idx_product_footprints_user ON product_footprints(username, viewed_at)");
                db.execSQL("CREATE INDEX idx_store_footprints_user ON store_footprints(username, viewed_at)");
        }

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
        }

        private void ensureSeedData(SQLiteDatabase db) {
                db.beginTransaction();
                try {
                        ensureAdminUser(db);
                        ensureTestSellerUser(db);
                        ensureDefaultProducts(db);
                        ensureDefaultArticles(db);
                        ensureExpiredReturnTestOrder(db);
                        db.setTransactionSuccessful();
                } finally {
                        db.endTransaction();
                }
        }

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

        @SuppressWarnings("unused")
        private boolean isTableEmpty(SQLiteDatabase db, String table) {
                try (Cursor cursor = db.rawQuery("SELECT 1 FROM " + table + " LIMIT 1", null)) {
                        return cursor == null || !cursor.moveToFirst();
                }
        }
}
