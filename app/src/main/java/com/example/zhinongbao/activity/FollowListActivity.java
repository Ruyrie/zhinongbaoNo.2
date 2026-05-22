package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow_list);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        String type = getIntent().getStringExtra("type"); // "followers" | "following" | "likes"
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
        rv.setAdapter(new FollowUserAdapter(users, currentUser, presenter));
    }

    // ─── Inner adapter ───────────────────────────────────────────────────────

    static class FollowUserAdapter extends RecyclerView.Adapter<FollowUserAdapter.VH> {

        private final List<String> users;
        private final String currentUser;
        private final FollowListContract.Presenter presenter;

        FollowUserAdapter(List<String> users, String currentUser, FollowListContract.Presenter presenter) {
            this.users = users;
            this.currentUser = currentUser;
            this.presenter = presenter;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_follow_user, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            String username = users.get(position);

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

            h.tvUsername.setText(nickname != null && !nickname.isEmpty() ? nickname : username);

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
            return users.size();
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
    }
}
