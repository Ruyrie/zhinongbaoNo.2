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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 粉丝列表 / 关注列表通用界面。
 * 通过 Intent extra "type" ("followers" | "following") 和 "username" 传参。
 */
public class FollowListActivity extends BaseMvpActivity<FollowListContract.Presenter> implements FollowListContract.View {

    private RecyclerView rv;
    private View tabContainer;
    private TextView tvUserFollowTab;
    private TextView tvStoreFollowTab;
    private String type;
    private List<String> allUsers = Collections.emptyList();
    private List<String> userFollows = Collections.emptyList();
    private List<String> storeFollows = Collections.emptyList();
    private String currentUser;

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
        tabContainer = findViewById(R.id.llFollowTabs);
        tvUserFollowTab = findViewById(R.id.tvUserFollowTab);
        tvStoreFollowTab = findViewById(R.id.tvStoreFollowTab);
        tvUserFollowTab.setOnClickListener(v -> showFollowingTab(false));
        tvStoreFollowTab.setOnClickListener(v -> showFollowingTab(true));

        new FollowListPresenter(this, this, type, username).start();
    }

    @Override
    public void showUsers(List<String> users, String currentUser) {
        this.allUsers = users == null ? Collections.emptyList() : users;
        this.currentUser = currentUser;
        if ("following".equals(type)) {
            tabContainer.setVisibility(View.VISIBLE);
            showFollowingTab(false);
        } else {
            tabContainer.setVisibility(View.GONE);
            rv.setAdapter(new FollowUserAdapter(this.allUsers, currentUser, presenter, false));
        }
    }

    @Override
    public void showFollowing(List<String> userFollows, List<String> storeFollows, String currentUser) {
        this.userFollows = userFollows == null ? Collections.emptyList() : userFollows;
        this.storeFollows = storeFollows == null ? Collections.emptyList() : storeFollows;
        this.currentUser = currentUser;
        tabContainer.setVisibility(View.VISIBLE);
        showFollowingTab(false);
    }

    private void showFollowingTab(boolean storeTab) {
        updateFollowTabStyle(storeTab);
        rv.setAdapter(new FollowUserAdapter(storeTab ? storeFollows : userFollows,
                currentUser, presenter, storeTab));
    }

    private void updateFollowTabStyle(boolean storeTab) {
        tvUserFollowTab.setTextColor(storeTab ? 0xFF666666 : 0xFF2F80ED);
        tvStoreFollowTab.setTextColor(storeTab ? 0xFF2F80ED : 0xFF666666);
        tvUserFollowTab.setTypeface(android.graphics.Typeface.DEFAULT,
                storeTab ? android.graphics.Typeface.NORMAL : android.graphics.Typeface.BOLD);
        tvStoreFollowTab.setTypeface(android.graphics.Typeface.DEFAULT,
                storeTab ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    // ─── Inner adapter ───────────────────────────────────────────────────────

    static class FollowUserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final List<String> users;
        private final String currentUser;
        private final FollowListContract.Presenter presenter;
        private final boolean storeRows;
        private final java.util.List<Row> rows = new java.util.ArrayList<>();

        FollowUserAdapter(List<String> users, String currentUser, FollowListContract.Presenter presenter,
                boolean storeRows) {
            this.users = users;
            this.currentUser = currentUser;
            this.presenter = presenter;
            this.storeRows = storeRows;
            buildRows();
        }

        private void buildRows() {
            rows.clear();
            for (String user : users) {
                rows.add(Row.user(user, storeRows));
            }
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_follow_user, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Row row = rows.get(position);
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
            refreshToggle(h, row);

            h.tvToggle.setOnClickListener(v -> {
                presenter.toggleFollow(username, row.store);
                refreshToggle(h, row);
            });
        }

        private void refreshToggle(VH h, Row row) {
            boolean following = presenter.isFollowing(row.username, row.store);
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
            boolean store;
            String username;

            static Row user(String username, boolean store) {
                Row row = new Row();
                row.username = username;
                row.store = store;
                return row;
            }
        }
    }
}
