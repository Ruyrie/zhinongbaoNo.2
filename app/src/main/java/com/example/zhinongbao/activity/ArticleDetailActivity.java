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

/**
 * ============================================================
 * 【文章详情 / Article Detail】View（Activity）
 * 整体逻辑（混合渲染，重点）：
 *   1) 正文区域用 WebView + HTML5 渲染：读取模板 assets/article_detail.html，
 *      把标题/作者/正文/配图等替换进 {{占位符}} 后，用 loadDataWithBaseURL 显示；
 *      网页里的「关注」按钮通过 JS 桥 Android.toggleFollow() 回调到原生。
 *   2) 顶部标题栏、评论列表(RecyclerView)、底部输入栏是原生控件（非网页）。
 * 数据来源：本类不直接碰数据库，所有数据由 Presenter 提供（Presenter 再向
 *   ArticleRepository → ContentProvider → SQLite 取数）。
 * 配合的文件：接口 ArticleDetailContract；业务 ArticleDetailPresenter；
 *   评论适配器 adapter/CommentAdapter；布局 activity_article_detail.xml；
 *   正文网页模板 assets/article_detail.html；模型 model/Article、model/Comment。
 * 在 MVP 中的位置：View 层。
 * 提示：在 IDE 里搜索「文章详情」可看本组相关文件。
 * ============================================================
 */
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

        // 取上个页面传来的文章 id，创建 Presenter 并启动（由它去加载数据后回调 showArticle）
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

        // 正文 HTML：正文文本 + 配图，最终替换到模板的 {{CONTENT}} 占位符
        StringBuilder contentHtml = new StringBuilder();
        contentHtml.append(article.content == null ? "" : article.content.replace("\n", "<br/>"));

        // 正文配图：详情页只展示「内容配图」，专门封面不在正文重复显示
        if (article.coverUri == null) {
            // 种子文章：按固定 id 映射内置插图
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
                contentHtml.append("<br/><img src=\"file:///android_res/mipmap/").append(imageTag.replace(".png", ""))
                        .append("\"/>");
            }
        } else {
            // 用户文章：只取内容配图（有专门封面时自动跳过封面段）
            for (String uri : article.getContentImages()) {
                contentHtml.append("<br/><img src=\"").append(uri).append("\"/>");
            }
        }

        // 读取 assets 下的 HTML5 模板，用文章数据替换占位符后交给 WebView 渲染
        String html = loadAssetText("article_detail.html")
                .replace("{{TITLE}}", nz(article.title))
                .replace("{{AVATAR}}", avatarHtml)
                .replace("{{AUTHOR_NAME}}", nz(authorName))
                .replace("{{TIME}}", nz(article.time))
                .replace("{{FOLLOW_BTN}}", followHtml)
                .replace("{{CONTENT}}", contentHtml.toString())
                .replace("{{READ_COUNT}}", String.valueOf(article.readCount));

        webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
        setupBottomBar();
    }

    private class WebAppInterface {
        @android.webkit.JavascriptInterface
        public void toggleFollow() {
            runOnUiThread(() -> presenter.toggleFollow());
        }
    }

    // ─── HTML5 模板辅助 ───────────────────────────────────────────────────────

    /** 读取 assets 目录下的文本文件（HTML 模板），失败时返回空串。 */
    private String loadAssetText(String fileName) {
        try (java.io.InputStream is = getAssets().open(fileName);
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toString("UTF-8");
        } catch (java.io.IOException e) {
            return "";
        }
    }

    /** null 安全：null 转空串，避免 String.replace 抛 NPE。 */
    private String nz(String s) {
        return s == null ? "" : s;
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

        // 输入评论时：隐藏「点赞/评论」图标，改为显示「发送」按钮；失焦后还原
        View layoutLike = findViewById(R.id.layoutLike);
        View layoutCommentIcon = findViewById(R.id.layoutCommentIcon);
        View btnSendComment = findViewById(R.id.btnSendComment);
        etComment.setOnFocusChangeListener((v, hasFocus) -> {
            layoutLike.setVisibility(hasFocus ? View.GONE : View.VISIBLE);
            layoutCommentIcon.setVisibility(hasFocus ? View.GONE : View.VISIBLE);
            btnSendComment.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
        });
        btnSendComment.setOnClickListener(v -> submitComment(etComment));

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
        // 发送后清除焦点，触发焦点监听还原「点赞/评论」图标
        et.clearFocus();

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
