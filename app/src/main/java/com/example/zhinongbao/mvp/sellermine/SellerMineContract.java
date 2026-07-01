package com.example.zhinongbao.mvp.sellermine;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Article;

import java.util.List;

/**
 * ============================================================
 * 【卖家我的 / Seller Mine】Contract（接口约定）
 * 约定内容：
 *   - View：渲染卖家资料与营业额、渲染订单角标数量、渲染资讯列表、重启回买家版首页。
 *   - Presenter：切回买家身份、资讯点赞状态查询/切换、点赞数与评论数查询。
 * 整体逻辑：View 只负责画界面，具体取数据与业务判断交给 Presenter。
 * 数据来源：Presenter 走 UserRepository / OrderRepository / ArticleRepository，
 *   这些 Repository 内部经 ContentProvider 访问 SQLite；本接口不碰数据库。
 * 配合的文件：View 实现 = fragment/SellerMineFragment；Presenter 实现 = SellerMinePresenter；
 *   模型 = model/Article、model/Order、model/User。
 * 在 MVP 数据流中的位置：View 与 Presenter 之间的「契约」层。
 * 提示：在 IDE 里搜索「卖家我的」可看本组相关文件。
 * ============================================================
 */
public interface SellerMineContract {
    interface View extends BaseView<Presenter> {
        // 渲染卖家资料头部与今日/本月/累计营业额
        void renderSeller(String username, String storeName, String nickname, String avatarUri,
                double todayRevenue, double monthRevenue, double totalRevenue);
        void renderOrderBadges(int pendingCount, int paidCount, int shippedCount, int refundCount); // 订单各状态角标数
        void renderNews(List<Article> articles); // 渲染下方资讯列表
        void restartMain(); // 切换身份后重启首页
    }

    interface Presenter extends BasePresenter {
        void switchToBuyer();                  // 切换到买家身份
        boolean isArticleLiked(int articleId); // 该资讯是否已点赞
        void toggleArticleLike(int articleId); // 点赞 / 取消点赞
        int getArticleLikeCount(int articleId);// 取点赞数
        int getCommentCount(int articleId);    // 取评论数
    }
}
