package com.example.zhinongbao.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.zhinongbao.AddProductActivity;
import com.example.zhinongbao.PurchaseMarketActivity;
import com.example.zhinongbao.R;

public class PublishFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_publish, container, false);

        view.findViewById(R.id.llPublishProduct).setOnClickListener(v ->
                startActivity(new Intent(getContext(), AddProductActivity.class)));

        view.findViewById(R.id.llPostPurchase).setOnClickListener(v ->
                startActivity(new Intent(getContext(), PurchaseMarketActivity.class)));

        return view;
    }
}
