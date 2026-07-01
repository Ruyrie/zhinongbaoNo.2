package com.example.zhinongbao.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.zhinongbao.base.BaseMvpFragment;
import com.example.zhinongbao.activity.AddProductActivity;
import com.example.zhinongbao.activity.PurchaseMarketActivity;
import com.example.zhinongbao.R;
import com.example.zhinongbao.mvp.publish.PublishContract;
import com.example.zhinongbao.mvp.publish.PublishPresenter;

/**
 * ============================================================
 * 【发布 / Publish】View（Fragment）
 * 整体逻辑（关键步骤）：
 *   1) onCreateView 加载布局并创建 Presenter start()。
 *   2) 给两个入口设点击监听，分别调 presenter.onPublishProductClicked() /
 *      onPostPurchaseClicked()。
 *   3) Presenter 处理后回调 openAddProduct() / openPurchaseMarket() 完成跳转。
 * 数据来源：本页不涉及数据读写（纯导航入口），Presenter 也不访问 Repository。
 * 配合的文件：接口 PublishContract；业务 PublishPresenter；布局 fragment_publish.xml；
 *   跳转页面 AddProductActivity（发产品）、PurchaseMarketActivity（采购市场）。
 * 在 MVP 数据流中的位置：View 层（本页只做 View 到 Presenter 的导航，不走 Repository）。
 * 提示：在 IDE 里搜索「发布」可看本组相关文件。
 * ============================================================
 */
public class PublishFragment extends BaseMvpFragment<PublishContract.Presenter> implements PublishContract.View {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_publish, container, false);
        new PublishPresenter(this).start();

        view.findViewById(R.id.llPublishProduct).setOnClickListener(v ->
                presenter.onPublishProductClicked());

        view.findViewById(R.id.llPostPurchase).setOnClickListener(v ->
                presenter.onPostPurchaseClicked());

        return view;
    }

    @Override
    public void openAddProduct() {
        startActivity(new Intent(getContext(), AddProductActivity.class));
    }

    @Override
    public void openPurchaseMarket() {
        startActivity(new Intent(getContext(), PurchaseMarketActivity.class));
    }
}
