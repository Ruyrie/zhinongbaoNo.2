package com.example.zhinongbao.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.zhinongbao.base.BaseMvpFragment;
import com.example.zhinongbao.AddProductActivity;
import com.example.zhinongbao.PurchaseMarketActivity;
import com.example.zhinongbao.R;
import com.example.zhinongbao.mvp.publish.PublishContract;
import com.example.zhinongbao.mvp.publish.PublishPresenter;

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
