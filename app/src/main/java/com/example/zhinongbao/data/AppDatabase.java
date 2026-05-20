package com.example.zhinongbao.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLite 数据库帮助类，管理全部业务数据表。
 * 替代原有的 SharedPreferences 持久化方案。
 */
public class AppDatabase extends SQLiteOpenHelper {

        private static final String DB_NAME = "zhinongbao.db";
        private static final int DB_VERSION = 15;

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
                                "refund_previous_status TEXT)");

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
        }
}
