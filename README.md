# 支农宝 NO.2

支农宝是一款面向农业生产、农产品销售与采购对接的 Android 应用。项目以本地 SQLite 数据库为核心数据源，围绕买家、卖家和双角色用户提供农产品商城、农技内容、农友圈、采购需求、订单管理、私信沟通等功能，适合作为 Android 课程设计、实训项目或农业电商类移动端原型。

## 项目概览

- 应用包名：`com.example.zhinongbao`
- 项目类型：原生 Android 应用
- 开发语言：Java
- 构建方式：Gradle Kotlin DSL
- 架构风格：MVP + Repository + ContentProvider + SQLite
- 启动页面：`LoginActivity`
- 主界面：`MainActivity`，根据当前角色切换买家/卖家首页与底部导航

## 主要功能

### 账号与角色

- 用户登录、注册、忘记密码、重置密码、修改密码
- 支持用户名或手机号登录
- 支持买家、卖家、双角色账号
- 支持头像、昵称、签名、手机号、店铺信息维护
- 登录状态和当前角色使用 `SharedPreferences` 保存

### 买家端

- 农产品商城浏览、分类展示、商品搜索
- 商品详情、收藏、浏览足迹、商品评价
- 购物车管理、订单创建、订单详情、确认收货、退款相关状态处理
- 收货地址管理
- 采购需求发布与采购市场浏览

### 卖家端

- 店铺首页与店铺信息展示
- 我的货品管理
- 采购需求报价与采购管理
- 卖家订单管理
- 销售流水分析

### 内容与社区

- 农技学堂、热点新闻、专家咨询、创业项目等文章内容
- 文章详情、评论、点赞、收藏
- 农友圈动态发布、点赞、收藏、我的动态
- 关注、粉丝列表、私信聊天、消息会话

## 技术栈

| 类别 | 技术 |
| --- | --- |
| Android SDK | `compileSdk 36.1`，`minSdk 31`，`targetSdk 36` |
| JDK | Java 11 |
| Gradle | Gradle Wrapper `9.2.1` |
| Android Gradle Plugin | `9.0.1` |
| UI 组件 | AppCompat、Material Components、ConstraintLayout、RecyclerView |
| 图片裁剪 | `com.github.yalantis:ucrop:2.2.8` |
| 本地存储 | SQLite、ContentProvider、SharedPreferences |
| 测试依赖 | JUnit、AndroidX Test、Espresso |

## 目录结构

```text
.
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       ├── java/com/example/zhinongbao/
│       │   ├── activity/       # 页面 Activity
│       │   ├── adapter/        # RecyclerView 等列表适配器
│       │   ├── base/           # MVP 基类
│       │   ├── data/           # SQLite 数据库帮助类
│       │   ├── fragment/       # 首页底部导航 Fragment
│       │   ├── model/          # 业务模型
│       │   ├── mvp/            # Contract 与 Presenter
│       │   ├── provider/       # ContentProvider
│       │   ├── repository/     # 数据访问层
│       │   └── view/           # 自定义 View
│       └── res/                # 布局、图片、主题、XML 配置
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 运行方式

1. 使用 Android Studio 打开项目根目录。
2. 等待 Gradle 同步完成，确认本机已配置 Android SDK。
3. 选择 `app` 运行配置。
4. 连接 Android 12 及以上设备，或创建 API 31 及以上模拟器。
5. 点击 Run 安装并启动应用。

也可以在命令行构建调试包：

```powershell
.\gradlew.bat assembleDebug
```

构建产物通常位于：

```text
app/build/outputs/apk/debug/
```

## 默认测试账号

应用启动并打开数据库时会自动补齐基础测试数据。

| 账号 | 密码 | 角色 | 说明 |
| --- | --- | --- | --- |
| `admin` | `123456` | 双角色 | 可在买家/卖家身份间切换 |
| `test_seller` | `123456` | 卖家 | 用于卖家店铺、订单、采购报价等流程测试 |

## 数据说明

核心数据库类为 `AppDatabase`，数据库名为 `zhinongbao.db`，当前版本号为 `20`。主要数据表包括：

- `users`：用户、角色、手机号、店铺信息
- `articles`：文章、农技内容、农友圈内容
- `products`：商品信息
- `cart`：购物车
- `orders`：订单、物流、退款、收货信息
- `addresses`：收货地址
- `comments`、`comment_likes`：文章评论与评论点赞
- `article_likes`、`circle_likes`、`circle_favorites`：文章/农友圈互动
- `product_comments`、`product_favorites`、`product_footprints`：商品评价、收藏和足迹
- `store_footprints`：店铺浏览足迹
- `follows`：关注关系
- `chat_messages`：私信聊天
- `purchase_requests`、`purchase_quotes`：采购需求与卖家报价

新业务数据建议通过 `Repository -> ContentResolver -> ZhiNongBaoProvider -> AppDatabase` 链路读写，避免在 Activity、Fragment 或 Presenter 中直接操作 SQLite。

## 开发约定

项目采用 MVP 分层，新页面建议按以下方式组织：

1. 在 `mvp/<feature>/` 下创建 `<Feature>Contract.java`，定义 View 与 Presenter 接口。
2. 在 `mvp/<feature>/` 下创建 `<Feature>Presenter.java`，处理业务流程和数据组装。
3. 在 `repository/` 中封装对应业务的数据访问逻辑。
4. Activity 或 Fragment 只负责控件绑定、页面跳转、弹窗和渲染。
5. 列表展示优先使用 `RecyclerView` 与 `adapter/` 下的适配器模式。

## 常用入口

| 功能 | 文件 |
| --- | --- |
| 登录启动页 | `app/src/main/java/com/example/zhinongbao/activity/LoginActivity.java` |
| 主界面与底部导航 | `app/src/main/java/com/example/zhinongbao/activity/MainActivity.java` |
| 数据库 | `app/src/main/java/com/example/zhinongbao/data/AppDatabase.java` |
| ContentProvider | `app/src/main/java/com/example/zhinongbao/provider/ZhiNongBaoProvider.java` |
| MVP 约定 | `app/src/main/java/com/example/zhinongbao/mvp/README.md` |
| 用户数据 | `app/src/main/java/com/example/zhinongbao/repository/UserRepository.java` |
| 商品数据 | `app/src/main/java/com/example/zhinongbao/repository/ProductRepository.java` |
| 订单数据 | `app/src/main/java/com/example/zhinongbao/repository/OrderRepository.java` |
| 采购数据 | `app/src/main/java/com/example/zhinongbao/repository/PurchaseRepository.java` |

## 注意事项

- `local.properties` 保存本机 SDK 路径，不应提交到远程仓库。
- `build/`、`.gradle/`、`.idea/` 等目录属于本地构建或 IDE 产物。
- 若修改数据库表结构，需要同步更新 `AppDatabase` 的 `onCreate` 和 `onUpgrade`。
- 若新增页面，需要在 `AndroidManifest.xml` 中注册对应 Activity。
- 若新增文件读写或图片选择能力，需要同步检查 `FileProvider` 与 `res/xml/file_paths.xml` 配置。
