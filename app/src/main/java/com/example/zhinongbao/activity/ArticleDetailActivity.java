package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.CommentAdapter;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.Article;
import com.example.zhinongbao.model.Comment;
import com.example.zhinongbao.mvp.articledetail.ArticleDetailContract;
import com.example.zhinongbao.mvp.articledetail.ArticleDetailPresenter;
import java.util.List;

public class ArticleDetailActivity extends BaseMvpActivity<ArticleDetailContract.Presenter>
        implements ArticleDetailContract.View {

    private int articleId;
    private Article article;
    private String currentUser;
    private List<Comment> comments;
    private CommentAdapter commentAdapter;

    // Views
    private TextView tvLikeCount, tvCommentCount, tvCommentCountBar;
    private ImageView ivLikeBtn;
    private LinearLayout llNoComments;
    private NestedScrollView nestedScroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        articleId = getIntent().getIntExtra("article_id", -1);
        new ArticleDetailPresenter(this, this, articleId).start();
    }

    // ─── Article binding ─────────────────────────────────────────────────────

    @Override
    public void showArticle(Article article, String currentUser, boolean following) {
        this.article = article;
        this.currentUser = currentUser;
        nestedScroll = findViewById(R.id.nestedScroll);

        // Toolbar
        ((TextView) findViewById(R.id.tvToolbarTitle)).setText(article.title);
        findViewById(R.id.tvBack).setOnClickListener(v -> finish());

        TextView tvDelete = findViewById(R.id.tvDeleteBtn);
        if (currentUser != null && article.author.equals(currentUser)) {
            tvDelete.setVisibility(View.VISIBLE);
            tvDelete.setOnClickListener(v -> {
                android.view.View view = getLayoutInflater().inflate(R.layout.dialog_confirm, null);
                android.widget.TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
                android.widget.TextView tvMessage = view.findViewById(R.id.tvDialogMessage);
                tvTitle.setText("删除作品");
                tvMessage.setText("确定要删除这篇稿件吗？删除后其他人将无法查看。");

                androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setView(view)
                        .create();

                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                }

                android.widget.TextView btnConfirm = view.findViewById(R.id.btnDialogConfirm);
                btnConfirm.setText("删除");
                btnConfirm.setBackgroundResource(R.drawable.bg_auth_button);

                view.findViewById(R.id.btnDialogCancel).setOnClickListener(btn -> dialog.dismiss());
                btnConfirm.setOnClickListener(btn -> {
                    dialog.dismiss();
                    presenter.deleteArticle();
                });
                dialog.show();
            });
        }

        android.webkit.WebView webView = findViewById(R.id.webViewContent);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        String authorName = article.authorNickname != null && !article.authorNickname.isEmpty() ? article.authorNickname
                : article.author;

        String avatarHtml = "";
        if (article.authorAvatarUri != null && article.authorAvatarUri.startsWith("data:image")) {
            avatarHtml = "<img class='avatar' src='" + article.authorAvatarUri + "' />";
        } else {
            String initial = (authorName != null && !authorName.isEmpty())
                    ? String.valueOf(authorName.charAt(0)).toUpperCase()
                    : "A";
            avatarHtml = "<div class='avatar-initial'>" + initial + "</div>";
        }

        boolean showFollow = (currentUser != null && !article.author.equals(currentUser));

        String followHtml = "";
        if (showFollow) {
            String btnText = following ? "已关注" : "+ 关注";
            String btnClass = following ? "follow-btn following" : "follow-btn";
            followHtml = "<div id='followBtn' class='" + btnClass + "' onclick='toggleFollow()'>" + btnText + "</div>";
        }

        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append(
                "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\" />")
                .append("<style>")
                .append("body { font-family: sans-serif; padding: 16px; margin: 0; background: #ffffff; }")
                .append(".title { font-size: 22px; font-weight: bold; color: #1A1A1A; line-height: 1.4; margin-bottom: 16px; }")
                .append(".author-row { display: flex; align-items: center; margin-bottom: 16px; }")
                .append(".avatar-container { width: 38px; height: 38px; border-radius: 19px; overflow: hidden; margin-right: 10px; background: #F04142; display: flex; justify-content: center; align-items: center; }")
                .append(".avatar { width: 100%; height: 100%; object-fit: cover; }")
                .append(".avatar-initial { color: white; font-size: 16px; font-weight: bold; }")
                .append(".author-info { flex: 1; display: flex; flex-direction: column; justify-content: center; }")
                .append(".author-name { font-size: 15px; font-weight: bold; color: #1F1F1F; }")
                .append(".author-time { font-size: 12px; color: #AAAAAA; margin-top: 2px; }")
                .append(".follow-btn { font-size: 13px; color: #2F80ED; padding: 5px 14px; border-radius: 15px; background: #F2F2F7; font-weight: bold; }")
                .append(".follow-btn.following { color: #999999; }")
                .append(".content { font-size: 16px; color: #333333; line-height: 1.8; }")
                .append(".content img { max-width: 100%; height: auto; display: block; margin: 12px 0; border-radius: 6px; }")
                .append(".read-count { font-size: 12px; color: #BBBBBB; margin-top: 24px; padding-bottom: 10px; }")
                .append("</style>")
                .append("<script>")
                .append("function toggleFollow() { Android.toggleFollow(); }")
                .append("function updateFollowBtn(following) { ")
                .append("  var btn = document.getElementById('followBtn');")
                .append("  if(following) { btn.className = 'follow-btn following'; btn.innerText = '已关注'; }")
                .append("  else { btn.className = 'follow-btn'; btn.innerText = '+ 关注'; }")
                .append("}")
                .append("</script>")
                .append("</head><body>")

                // Title
                .append("<div class='title'>").append(article.title).append("</div>")

                // Author Row
                .append("<div class='author-row'>")
                .append("  <div class='avatar-container'>").append(avatarHtml).append("</div>")
                .append("  <div class='author-info'>")
                .append("    <div class='author-name'>").append(authorName).append("</div>")
                .append("    <div class='author-time'>").append(article.time).append("</div>")
                .append("  </div>")
                .append(followHtml)
                .append("</div>")

                // Content
                .append("<div class='content'>")
                .append(article.content.replace("\n", "<br/>"));

        // Add internal images for seed articles
        if (article.coverUri == null) {
            String imageTag = "";
            switch (article.id) {
                case 5:
                    imageTag = "text1.png";
                    break;
                case 4:
                    imageTag = "text2.png";
                    break;
                case 3:
                    imageTag = "text3.png";
                    break;
                case 2:
                    imageTag = "text4.png";
                    break;
                case 1:
                    imageTag = "text5.png";
                    break;
            }
            if (!imageTag.isEmpty()) {
                htmlBuilder.append("<br/><img src=\"file:///android_res/mipmap/").append(imageTag.replace(".png", ""))
                        .append("\"/>");
            }
        } else if (!article.coverUri.isEmpty()) {
            String[] uris = article.coverUri.split(",");
            for (String uri : uris) {
                htmlBuilder.append("<br/><img src=\"").append(uri).append("\"/>");
            }
        }

        htmlBuilder.append("</div>")
                .append("<div class='read-count'>阅读 ").append(article.readCount).append(" 次</div>")
                .append("</body></html>");

        webView.loadDataWithBaseURL("file:///android_asset/", htmlBuilder.toString(), "text/html", "UTF-8", null);
        setupBottomBar();
    }

    private class WebAppInterface {
        @android.webkit.JavascriptInterface
        public void toggleFollow() {
            runOnUiThread(() -> presenter.toggleFollow());
        }
    }

    // ─── Comments ────────────────────────────────────────────────────────────

    private void setupComments(List<Comment> comments) {
        tvCommentCount = findViewById(R.id.tvCommentCount);
        tvCommentCountBar = findViewById(R.id.tvCommentCountBar);
        llNoComments = findViewById(R.id.llNoComments);

        this.comments = comments;
        updateCommentCountUI();

        commentAdapter = new CommentAdapter(comments, currentUser, article.author,
                new CommentAdapter.CommentInteractionDelegate() {
                    @Override
                    public void likeComment(int commentId) {
                        presenter.likeComment(commentId);
                    }

                    @Override
                    public void unlikeComment(int commentId) {
                        presenter.unlikeComment(commentId);
                    }
                });
        commentAdapter.setOnDeleteListener(comment -> {
            android.view.View view = getLayoutInflater().inflate(R.layout.dialog_confirm, null);
            android.widget.TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
            android.widget.TextView tvMessage = view.findViewById(R.id.tvDialogMessage);
            tvTitle.setText("删除评论");
            tvMessage.setText("确定删除这条评论吗？");

            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setView(view)
                    .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            android.widget.TextView btnConfirm = view.findViewById(R.id.btnDialogConfirm);
            btnConfirm.setText("删除");
            btnConfirm.setBackgroundResource(R.drawable.bg_auth_button);

            view.findViewById(R.id.btnDialogCancel).setOnClickListener(btn -> dialog.dismiss());
            btnConfirm.setOnClickListener(btn -> {
                dialog.dismiss();
                presenter.deleteComment(comment);
            });
            dialog.show();
        });

        RecyclerView rv = findViewById(R.id.rvComments);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setNestedScrollingEnabled(false);
        rv.setAdapter(commentAdapter);
    }

    @Override
    public void showComments(List<Comment> comments) {
        if (commentAdapter == null) {
            setupComments(comments);
            return;
        }
        this.comments.clear();
        this.comments.addAll(comments);
        commentAdapter.notifyDataSetChanged();
        updateCommentCountUI();
    }

    private void updateCommentCountUI() {
        int n = comments.size();
        tvCommentCount.setText(n + " 条");
        tvCommentCountBar.setText(String.valueOf(n));
        llNoComments.setVisibility(n == 0 ? View.VISIBLE : View.GONE);
    }

    // ─── Bottom bar (like + comment input) ───────────────────────────────────

    private void setupBottomBar() {
        ivLikeBtn = findViewById(R.id.tvLikeBtn);
        tvLikeCount = findViewById(R.id.tvLikeCount);
        refreshLikeUI();

        // Like toggle
        findViewById(R.id.layoutLike).setOnClickListener(v -> {
            boolean wasLiked = currentUser != null && presenter != null;
            presenter.toggleArticleLike();
            if (wasLiked) {
                // Heart-beat animation
                ivLikeBtn.animate().scaleX(1.35f).scaleY(1.35f).setDuration(130)
                        .withEndAction(() -> ivLikeBtn.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                        .start();
            }
        });

        // Comment submit via keyboard "Send" action
        EditText etComment = findViewById(R.id.etComment);
        etComment.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitComment(etComment);
                return true;
            }
            return false;
        });

        // Scroll to comments when clicking the comment icon
        findViewById(R.id.layoutCommentIcon).setOnClickListener(v -> {
            if (tvCommentCount != null) {
                nestedScroll.post(() -> {
                    // Scroll to the comment header
                    nestedScroll.smoothScrollTo(0, ((View) tvCommentCount.getParent()).getTop());
                });
            }
        });
    }

    private void refreshLikeUI() {
        if (presenter != null) {
            presenter.refreshLikeState();
        }
    }

    @Override
    public void showLikeState(boolean liked, int likeCount) {
        if (ivLikeBtn == null || tvLikeCount == null) {
            return;
        }
        ivLikeBtn.setImageResource(liked ? R.mipmap.dianzan : R.mipmap.weidianzan);
        tvLikeCount.setText(likeCount > 0 ? String.valueOf(likeCount) : "");
    }

    @Override
    public void showFollowState(boolean following) {
        android.webkit.WebView webView = findViewById(R.id.webViewContent);
        webView.evaluateJavascript("javascript:updateFollowBtn(" + following + ")", null);
    }

    private void submitComment(EditText et) {
        String text = et.getText().toString().trim();
        if (text.isEmpty())
            return;

        presenter.submitComment(text);
        et.setText("");

        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
        // Scroll to new comment
        nestedScroll.post(() -> nestedScroll.fullScroll(View.FOCUS_DOWN));
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void closePage() {
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ivLikeBtn != null) {
            refreshLikeUI();
        }
        if (presenter != null && comments != null && commentAdapter != null) {
            presenter.refreshComments();
        }
    }
}
