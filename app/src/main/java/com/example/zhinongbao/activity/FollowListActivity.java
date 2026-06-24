package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.mvp.followlist.FollowListContract;
import com.example.zhinongbao.mvp.followlist.FollowListPresenter;
import java.util.List;

/**
 * 粉丝列表 / 关注列表通用界面。
 * 通过 Intent extra "type" ("followers" | "following") 和 "username" 传参。
 */
public class FollowListActivity extends BaseMvpActivity<FollowListContract.Presenter> implements FollowListContract.View {

    private RecyclerView rv;
    private String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow_list);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        type = getIntent().getStringExtra("type"); // "followers" | "following" | "likes"
        String username = getIntent().getStringExtra("username");

        if (username == null) {
            finish();
            return;
        }

        boolean isFollowers = "followers".equals(type);
        boolean isLikes = "likes".equals(type);

        // Toolbar title
        TextView tvTitle = findViewById(R.id.tvFollowTitle);
        if (isLikes) {
            tvTitle.setText("获赞");
        } else {
            tvTitle.setText(isFollowers ? "粉丝" : "关注");
        }
        findViewById(R.id.tvFollowBack).setOnClickListener(v -> finish());

        rv = findViewById(R.id.rvFollowList);
        rv.setLayoutManager(new LinearLayoutManager(this));

        new FollowListPresenter(this, this, type, username).start();
    }

    @Override
    public void showUsers(List<String> users, String currentUser) {
        rv.setAdapter(new FollowUserAdapter(users, currentUser, presenter, "following".equals(type)));
    }

    // ─── Inner adapter ───────────────────────────────────────────────────────

    static class FollowUserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final List<String> users;
        private final String currentUser;
        private final FollowListContract.Presenter presenter;
        private final boolean splitFollowing;
        private final java.util.List<Row> rows = new java.util.ArrayList<>();
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_USER = 1;

        FollowUserAdapter(List<String> users, String currentUser, FollowListContract.Presenter presenter,
                boolean splitFollowing) {
            this.users = users;
            this.currentUser = currentUser;
            this.presenter = presenter;
            this.splitFollowing = splitFollowing;
            buildRows();
        }

        private void buildRows() {
            rows.clear();
            if (!splitFollowing) {
                for (String user : users) {
                    rows.add(Row.user(user, false));
                }
                return;
            }

            java.util.List<String> userFollows = new java.util.ArrayList<>();
            java.util.List<String> storeFollows = new java.util.ArrayList<>();
            for (String user : users) {
                if (presenter.isStoreAccount(user)) {
                    storeFollows.add(user);
                } else {
                    userFollows.add(user);
                }
            }

            rows.add(Row.header("用户关注"));
            for (String user : userFollows) {
                rows.add(Row.user(user, false));
            }
            rows.add(Row.header("店铺关注"));
            for (String store : storeFollows) {
                rows.add(Row.user(store, true));
            }
        }

        @Override
        public int getItemViewType(int position) {
            return rows.get(position).header ? TYPE_HEADER : TYPE_USER;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                TextView title = new TextView(parent.getContext());
                title.setLayoutParams(new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(parent, 44)));
                title.setGravity(android.view.Gravity.CENTER_VERTICAL);
                title.setPadding(dp(parent, 16), dp(parent, 10), dp(parent, 16), 0);
                title.setTextColor(0xFF666666);
                title.setTextSize(14);
                title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
                title.setBackgroundColor(0xFFF2F2F7);
                return new HeaderVH(title);
            }
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_follow_user, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Row row = rows.get(position);
            if (holder instanceof HeaderVH) {
                ((HeaderVH) holder).title.setText(row.title);
                return;
            }
            VH h = (VH) holder;
            String username = row.username;

            // Fetch user info for avatar and nickname
            String nickname = presenter.getNickname(username);
            String avatarUri = presenter.getAvatarUri(username);

            // Avatar initial
            String initial = (nickname != null && !nickname.isEmpty())
                    ? String.valueOf(nickname.charAt(0)).toUpperCase()
                    : (username.isEmpty() ? "U" : String.valueOf(username.charAt(0)).toUpperCase());
            h.tvAvatar.setText(initial);

            if (avatarUri != null) {
                try {
                    if (avatarUri.startsWith("data:image")) {
                        com.example.zhinongbao.utils.ImageUtils.setAvatarFromBase64(h.ivAvatar, avatarUri);
                    } else {
                        h.ivAvatar.setImageURI(android.net.Uri.parse(avatarUri));
                    }
                    h.ivAvatar.setVisibility(View.VISIBLE);
                    h.tvAvatar.setVisibility(View.GONE);
                } catch (Exception e) {
                    h.ivAvatar.setVisibility(View.GONE);
                    h.tvAvatar.setVisibility(View.VISIBLE);
                }
            } else {
                h.ivAvatar.setVisibility(View.GONE);
                h.tvAvatar.setVisibility(View.VISIBLE);
            }

            h.tvUsername.setText(row.store ? presenter.getStoreName(username)
                    : (nickname != null && !nickname.isEmpty() ? nickname : username));
            h.itemView.setOnClickListener(v -> openFollowTarget(h, row));

            // Hide follow button for self
            if (username.equals(currentUser)) {
                h.tvToggle.setVisibility(View.GONE);
                return;
            }

            h.tvToggle.setVisibility(View.VISIBLE);
            refreshToggle(h, username);

            h.tvToggle.setOnClickListener(v -> {
                presenter.toggleFollow(username);
                refreshToggle(h, username);
            });
        }

        private void refreshToggle(VH h, String username) {
            boolean following = presenter.isFollowing(username);
            h.tvToggle.setText(following ? "已关注" : "关注");
            h.tvToggle.setTextColor(following ? 0xFF999999 : 0xFF2F80ED);
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        private void openFollowTarget(VH h, Row row) {
            Intent intent;
            if (row.store) {
                intent = new Intent(h.itemView.getContext(), SellerStoreActivity.class);
                intent.putExtra("seller", row.username);
                intent.putExtra("public_store", true);
            } else {
                intent = new Intent(h.itemView.getContext(), MyArticlesActivity.class);
                intent.putExtra("author", row.username);
            }
            h.itemView.getContext().startActivity(intent);
        }

        private int dp(View view, int value) {
            return (int) (value * view.getResources().getDisplayMetrics().density + 0.5f);
        }

        static class HeaderVH extends RecyclerView.ViewHolder {
            TextView title;

            HeaderVH(View v) {
                super(v);
                title = (TextView) v;
            }
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvAvatar, tvUsername, tvToggle;
            android.widget.ImageView ivAvatar;

            VH(View v) {
                super(v);
                tvAvatar = v.findViewById(R.id.tvFollowUserAvatar);
                ivAvatar = v.findViewById(R.id.ivFollowUserAvatar);
                tvUsername = v.findViewById(R.id.tvFollowUsername);
                tvToggle = v.findViewById(R.id.tvFollowToggle);
            }
        }

        static class Row {
            boolean header;
            boolean store;
            String title;
            String username;

            static Row header(String title) {
                Row row = new Row();
                row.header = true;
                row.title = title;
                return row;
            }

            static Row user(String username, boolean store) {
                Row row = new Row();
                row.username = username;
                row.store = store;
                return row;
            }
        }
    }
}
