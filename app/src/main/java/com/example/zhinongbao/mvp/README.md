# MVP 编写约定

新页面按以下结构实现：

1. `mvp/<feature>/<Feature>Contract.java`：定义 `View` 与 `Presenter`。
2. `mvp/<feature>/<Feature>Presenter.java`：只处理业务流程，不持有 Android View 控件。
3. `repository/<Feature>Repository.java`：通过 `ContentResolver` 调用 `ZhiNongBaoProvider` 读写业务表。
4. `Activity/Fragment`：只负责控件绑定、弹窗、页面跳转、渲染 Presenter 返回的数据。

业务数据必须走 `ContentProvider`，不要在新 Presenter 或 Activity 中直接访问 SQLite。
