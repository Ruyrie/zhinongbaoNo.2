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
import com.example.zhinongbao.activity.CartActivity;
import com.example.zhinongbao.activity.FollowListActivity;
import com.example.zhinongbao.activity.FootprintActivity;
import com.example.zhinongbao.activity.MainActivity;
import com.example.zhinongbao.activity.MyArticlesActivity;
import com.example.zhinongbao.activity.MyFavoritesActivity;
import com.example.zhinongbao.activity.MyOrdersActivity;
import com.example.zhinongbao.activity.ProductFavoritesActivity;
import com.example.zhinongbao.activity.ProfileEditActivity;
import com.example.zhinongbao.R;
import com.example.zhinongbao.activity.SettingsActivity;
import com.example.zhinongbao.base.BaseMvpFragment;
import com.example.zhinongbao.mvp.mine.MineContract;
import com.example.zhinongbao.mvp.mine.MinePresenter;
import java.io.IOException;
import java.io.InputStream;

public class MineFragment extends BaseMvpFragment<MineContract.Presenter> implements MineContract.View {

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                        @Nullable ViewGroup container,
                        @Nullable Bundle savedInstanceState) {
                return inflater.inflate(R.layout.fragment_mine, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
                bindStaticViews(view);
                new MinePresenter(requireContext(), this).start();
        }

        @Override
        public void onResume() {
                super.onResume();
                if (getView() != null)
                        presenter.start();
        }

        private void bindStaticViews(View view) {
                loadAssetImage(view.findViewById(R.id.ivOrderPending), "daifukuan.png");
                loadAssetImage(view.findViewById(R.id.ivOrderShipping), "daifahuo.png");
                loadAssetImage(view.findViewById(R.id.ivOrderReceiving), "daishouhuo.png");
                loadAssetImage(view.findViewById(R.id.ivOrderReviewing), "daipingjia.png");
                loadAssetImage(view.findViewById(R.id.ivOrderRefund), "tuikuanshouhou.png");
                loadAssetImage(view.findViewById(R.id.ivFootprintIcon), "zuji.png");

                view.findViewById(R.id.layoutProfile).setOnClickListener(
                                v -> startActivity(new Intent(getContext(), ProfileEditActivity.class)));
                view.findViewById(R.id.quickCart).setOnClickListener(
                                v -> startActivity(new Intent(getContext(), CartActivity.class)));
                view.findViewById(R.id.quickFavorites).setOnClickListener(
                                v -> startActivity(new Intent(getContext(), ProductFavoritesActivity.class)));
                view.findViewById(R.id.quickFootprint).setOnClickListener(
                                v -> startActivity(new Intent(getContext(), FootprintActivity.class)));
                view.findViewById(R.id.tvAllOrders).setOnClickListener(
                                v -> startActivity(new Intent(getContext(), MyOrdersActivity.class)));
                view.findViewById(R.id.orderPending).setOnClickListener(v -> openOrders("pending"));
                view.findViewById(R.id.orderShipping).setOnClickListener(v -> openOrders("shipping"));
                view.findViewById(R.id.orderReceiving).setOnClickListener(v -> openOrders("receiving"));
                view.findViewById(R.id.orderReviewing).setOnClickListener(v -> openOrders("reviewing"));
                view.findViewById(R.id.orderRefund).setOnClickListener(v -> openOrders("refundable"));
                view.findViewById(R.id.tvMyArticles).setOnClickListener(
                                v -> startActivity(new Intent(getContext(), MyArticlesActivity.class)));
                view.findViewById(R.id.tvMyFavorites).setOnClickListener(
                                v -> startActivity(new Intent(getContext(), MyFavoritesActivity.class)));
                view.findViewById(R.id.tvSettings).setOnClickListener(
                                v -> startActivity(new Intent(getContext(), SettingsActivity.class)));
        }

        @Override
        public void renderUser(String username, String nickname, String signature, String avatarUri,
                        int followers, int following, int likes, boolean sellerActive, boolean canSwitchSeller) {
                View view = getView();
                if (view == null)
                        return;

                ((TextView) view.findViewById(R.id.tvMineUsername)).setText("用户名： " + username);
                ((TextView) view.findViewById(R.id.tvMineNickname)).setText(nickname);
                ((TextView) view.findViewById(R.id.tvMineSignature)).setText(signature);

                // Avatar
                ImageView ivAvatar = view.findViewById(R.id.ivMineAvatar);
                TextView tvInitial = view.findViewById(R.id.tvAvatarInitial);
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
                                .setText(String.valueOf(followers));
                ((TextView) view.findViewById(R.id.tvStatFollowing))
                                .setText(String.valueOf(following));
                ((TextView) view.findViewById(R.id.tvStatLikes))
                                .setText(String.valueOf(likes));

                // Role switch button
                TextView btnSwitch = view.findViewById(R.id.btnSwitchToSeller);
                btnSwitch.setText(sellerActive ? "切换到买家" : "切换到卖家");
                btnSwitch.setVisibility(canSwitchSeller ? View.VISIBLE : View.GONE);
                btnSwitch.setOnClickListener(v -> presenter.switchRole());

                // Stats row click handlers
                view.findViewById(R.id.statFollowers).setOnClickListener(v -> {
                        Intent i = new Intent(getContext(), FollowListActivity.class);
                        i.putExtra("type", "followers");
                        i.putExtra("username", presenter.getCurrentUser());
                        startActivity(i);
                });
                view.findViewById(R.id.statFollowing).setOnClickListener(v -> {
                        Intent i = new Intent(getContext(), FollowListActivity.class);
                        i.putExtra("type", "following");
                        i.putExtra("username", presenter.getCurrentUser());
                        startActivity(i);
                });
                view.findViewById(R.id.statLikes).setOnClickListener(v -> {
                        Intent i = new Intent(getContext(), FollowListActivity.class);
                        i.putExtra("type", "likes");
                        i.putExtra("username", presenter.getCurrentUser());
                        startActivity(i);
                });

        }

        @Override
        public void restartMain() {
                Intent intent = new Intent(requireContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
        }

        private void openOrders(String filter) {
                Intent i = new Intent(getContext(), MyOrdersActivity.class);
                i.putExtra("filter", filter);
                startActivity(i);
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
