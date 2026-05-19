package com.example.zhinongbao.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.zhinongbao.CartActivity;
import com.example.zhinongbao.FollowListActivity;
import com.example.zhinongbao.FootprintActivity;
import com.example.zhinongbao.MyArticlesActivity;
import com.example.zhinongbao.MyFavoritesActivity;
import com.example.zhinongbao.MyOrdersActivity;
import com.example.zhinongbao.ProductFavoritesActivity;
import com.example.zhinongbao.ProfileEditActivity;
import com.example.zhinongbao.R;
import com.example.zhinongbao.SettingsActivity;
import com.example.zhinongbao.data.DataManager;
import java.io.IOException;
import java.io.InputStream;

public class MineFragment extends Fragment {

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                        @Nullable ViewGroup container,
                        @Nullable Bundle savedInstanceState) {
                return inflater.inflate(R.layout.fragment_mine, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
                bindViews(view);
        }

        @Override
        public void onResume() {
                super.onResume();
                if (getView() != null)
                        bindViews(getView());
        }

        private void bindViews(View view) {
                DataManager dm = DataManager.getInstance(requireContext());
                String username = dm.getLoggedUser();

                ((TextView) view.findViewById(R.id.tvMineUsername)).setText("用户名： " + username);
                ((TextView) view.findViewById(R.id.tvMineNickname)).setText(dm.getNickname(username));
                ((TextView) view.findViewById(R.id.tvMineSignature)).setText(dm.getSignature(username));

                // Avatar
                ImageView ivAvatar = view.findViewById(R.id.ivMineAvatar);
                TextView tvInitial = view.findViewById(R.id.tvAvatarInitial);
                String avatarUri = dm.getAvatarUri(username);
                if (avatarUri != null) {
                        try {
                                if (avatarUri.startsWith("data:image")) {
                                        com.example.zhinongbao.utils.ImageUtils.setAvatarFromBase64(ivAvatar,
                                                        avatarUri);
                                } else {
                                        ivAvatar.setImageURI(Uri.parse(avatarUri));
                                }
                                ivAvatar.setVisibility(View.VISIBLE);
                                tvInitial.setVisibility(View.GONE);
                        } catch (Exception e) {
                                ivAvatar.setVisibility(View.GONE);
                                tvInitial.setVisibility(View.VISIBLE);
                        }
                } else {
                        String initial = (!username.isEmpty())
                                        ? String.valueOf(username.charAt(0)).toUpperCase()
                                        : "我";
                        tvInitial.setText(initial);
                        ivAvatar.setVisibility(View.GONE);
                        tvInitial.setVisibility(View.VISIBLE);
                }

                // Stats: 粉丝 / 关注 / 获赞
                ((TextView) view.findViewById(R.id.tvStatFollowers))
                                .setText(String.valueOf(dm.getFollowersCount(username)));
                ((TextView) view.findViewById(R.id.tvStatFollowing))
                                .setText(String.valueOf(dm.getFollowingCount(username)));
                ((TextView) view.findViewById(R.id.tvStatLikes))
                                .setText(String.valueOf(dm.getTotalLikesReceived(username)));

                // Load order status icons from assets
                loadAssetImage(view.findViewById(R.id.ivOrderPending), "daifukuan.png");
                loadAssetImage(view.findViewById(R.id.ivOrderShipping), "daifahuo.png");
                loadAssetImage(view.findViewById(R.id.ivOrderReceiving), "daishouhuo.png");
                loadAssetImage(view.findViewById(R.id.ivOrderReviewing), "daipingjia.png");
                loadAssetImage(view.findViewById(R.id.ivOrderRefund), "tuikuanshouhou.png");

                // Load footprint icon from assets
                loadAssetImage(view.findViewById(R.id.ivFootprintIcon), "zuji.png");

                // Role switch button
                View btnSwitch = view.findViewById(R.id.btnSwitchToSeller);
                if (dm.getUserRole(username) == com.example.zhinongbao.model.User.ROLE_BOTH) {
                        btnSwitch.setVisibility(View.VISIBLE);
                        btnSwitch.setOnClickListener(v -> {
                                dm.setActiveRole(com.example.zhinongbao.model.User.ROLE_SELLER);
                                startActivity(new Intent(getContext(), MainActivity.class));
                                getActivity().finish();
                        });
                } else {
                        btnSwitch.setVisibility(View.GONE);
                }

                // Profile click
                view.findViewById(R.id.layoutProfile).setOnClickListener(
                                v -> startActivity(new Intent(getContext(), ProfileEditActivity.class)));

                // Quick action row
                view.findViewById(R.id.quickCart).setOnClickListener(
                                v -> startActivity(new Intent(getContext(), CartActivity.class)));
                view.findViewById(R.id.quickFavorites).setOnClickListener(
                                v -> startActivity(new Intent(getContext(), ProductFavoritesActivity.class)));
                view.findViewById(R.id.quickFootprint).setOnClickListener(
                                v -> startActivity(new Intent(getContext(), FootprintActivity.class)));

                // Orders card: all status icons → MyOrdersActivity with filter
                view.findViewById(R.id.tvAllOrders).setOnClickListener(
                                v -> startActivity(new Intent(getContext(), MyOrdersActivity.class)));
                view.findViewById(R.id.orderPending).setOnClickListener(v -> {
                        Intent i = new Intent(getContext(), MyOrdersActivity.class);
                        i.putExtra("filter", "pending");
                        startActivity(i);
                });
                view.findViewById(R.id.orderShipping).setOnClickListener(v -> {
                        Intent i = new Intent(getContext(), MyOrdersActivity.class);
                        i.putExtra("filter", "shipping");
                        startActivity(i);
                });
                view.findViewById(R.id.orderReceiving).setOnClickListener(v -> {
                        Intent i = new Intent(getContext(), MyOrdersActivity.class);
                        i.putExtra("filter", "receiving");
                        startActivity(i);
                });
                view.findViewById(R.id.orderReviewing).setOnClickListener(v -> {
                        Intent i = new Intent(getContext(), MyOrdersActivity.class);
                        i.putExtra("filter", "reviewing");
                        startActivity(i);
                });
                view.findViewById(R.id.orderRefund).setOnClickListener(v -> {
                        Intent i = new Intent(getContext(), MyOrdersActivity.class);
                        i.putExtra("filter", "refund");
                        startActivity(i);
                });

                // Stats row click handlers
                view.findViewById(R.id.statFollowers).setOnClickListener(v -> {
                        Intent i = new Intent(getContext(), FollowListActivity.class);
                        i.putExtra("type", "followers");
                        i.putExtra("username", username);
                        startActivity(i);
                });
                view.findViewById(R.id.statFollowing).setOnClickListener(v -> {
                        Intent i = new Intent(getContext(), FollowListActivity.class);
                        i.putExtra("type", "following");
                        i.putExtra("username", username);
                        startActivity(i);
                });
                view.findViewById(R.id.statLikes).setOnClickListener(v -> {
                        Intent i = new Intent(getContext(), FollowListActivity.class);
                        i.putExtra("type", "likes");
                        i.putExtra("username", username);
                        startActivity(i);
                });

                // Menu items
                view.findViewById(R.id.tvMyArticles).setOnClickListener(
                                v -> startActivity(new Intent(getContext(), MyArticlesActivity.class)));
                view.findViewById(R.id.tvMyFavorites).setOnClickListener(
                                v -> startActivity(new Intent(getContext(), MyFavoritesActivity.class)));
                view.findViewById(R.id.tvSettings).setOnClickListener(
                                v -> startActivity(new Intent(getContext(), SettingsActivity.class)));
        }

        private void loadAssetImage(ImageView iv, String filename) {
                try {
                        InputStream is = requireContext().getAssets().open("pic/" + filename);
                        Bitmap bmp = BitmapFactory.decodeStream(is);
                        iv.setImageBitmap(bmp);
                        is.close();
                } catch (IOException ignored) {
                }
        }
}
