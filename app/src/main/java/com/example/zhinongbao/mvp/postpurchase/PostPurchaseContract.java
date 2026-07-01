package com.example.zhinongbao.mvp.postpurchase;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.PurchaseRequest;

/**
 * ============================================================
 * 【发布采购需求 / PostPurchase】Contract（接口约定）
 * 约定内容：
 *   - View：编辑模式下回填已有需求、弹提示、关闭页面。
 *   - Presenter：加载待编辑需求、发布新需求、保存编辑。
 * 配合的文件：View 实现 = PostPurchaseActivity；Presenter 实现 = PostPurchasePresenter；
 *   数据访问 = repository/PurchaseRepository（经 ContentProvider 访问 SQLite）；
 *   模型 = model/PurchaseRequest。
 * MVP 数据流位置：本文件是 View 与 Presenter 之间的「合同」。
 * 提示：在 IDE 里搜索「采购」可看本组相关文件。
 * ============================================================
 */
public interface PostPurchaseContract {
    // View：Presenter 用这些方法更新发布/编辑界面
    interface View extends BaseView<Presenter> {
        void showExistingRequest(PurchaseRequest request); // 编辑模式：把原需求回填到表单
        void showToast(String message);                    // 弹提示
        void closePage();                                  // 关闭本页
    }

    // Presenter：View（用户操作）用这些方法触发业务
    interface Presenter extends BasePresenter {
        void loadRequest(long requestId);                  // 加载待编辑的需求
        void submit(String name, String category, String qty, String unit, String targetPrice, String desc,
                String images);                            // 发布新需求
        void submitEdit(long requestId, String name, String category, String qty, String unit,
                String targetPrice, String desc, String images); // 保存对已有需求的编辑
    }
}
