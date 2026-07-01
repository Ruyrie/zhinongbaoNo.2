package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.ChatAdapter;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.ChatMessage;
import com.example.zhinongbao.mvp.chat.ChatContract;
import com.example.zhinongbao.mvp.chat.ChatPresenter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * ============================================================
 * 【聊天对话 / Chat】View（Activity 界面）
 * 整体逻辑：onCreate 读取传入的对方账号 other_user（缺省为客服 admin）与可选
 *   商品名 product_name，配置 RecyclerView、输入框、更多面板，启动 ChatPresenter；
 *   输入框有内容时显示「发送」按钮、无内容时显示「+」更多面板按钮；
 *   拍照/选图先把图片复制到应用缓存目录，再经 FileProvider 生成可分享的 Uri 发出；
 *   onResume 每次回到前台标记已读并刷新消息。showMessages 由 Presenter 回调，
 *   首次创建 ChatAdapter 并把撤回/删除回调转交 Presenter，之后仅刷新数据并滚到底部。
 * 数据来源：ChatPresenter 经 repository/MessageRepository 读写；
 *   Repository 内部经 ContentProvider 访问 SQLite。本类不直接碰数据库。
 * 配合的文件：接口约定 ChatContract；业务 ChatPresenter；
 *   消息适配器 adapter/ChatAdapter；数据模型 model/ChatMessage；
 *   布局 res/layout/activity_chat.xml 及气泡布局 item_chat_sent/received/recalled.xml。
 * MVP 数据流位置：本类是 View；数据流为 View 到 Presenter 到 Repository 到
 *   ContentProvider 到 SQLite，再回调本类刷新界面。
 * 提示：在 IDE 里搜索「聊天」可看本组相关文件。
 * ============================================================
 */
public class ChatActivity extends BaseMvpActivity<ChatContract.Presenter>
        implements ChatContract.View {

    /** 默认客服账号（商家） */
    public static final String SHOP_USERNAME = "admin";

    private RecyclerView rvMessages;
    private ChatAdapter adapter;
    private List<ChatMessage> messages;
    private EditText etInput;
    private Button btnSend;
    private TextView btnMore;
    private LinearLayout morePanel;
    private Uri currentCameraUri;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) {
                    return;
                }
                Uri localUri = copyImageToCache(uri);
                if (localUri == null) {
                    showToast("图片发送失败");
                    return;
                }
                hideMorePanel();
                presenter.sendImage(localUri.toString());
            });

    private final ActivityResultLauncher<Uri> takePicture =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && currentCameraUri != null) {
                    hideMorePanel();
                    presenter.sendImage(currentCameraUri.toString());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        String otherUser = getIntent().getStringExtra("other_user");
        if (otherUser == null) otherUser = SHOP_USERNAME;
        String productName = getIntent().getStringExtra("product_name");

        findViewById(R.id.ivChatBack).setOnClickListener(v -> finish());

        rvMessages = findViewById(R.id.rvChatMessages);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));

        etInput = findViewById(R.id.etChatInput);
        btnSend = findViewById(R.id.btnChatSend);
        btnMore = findViewById(R.id.btnChatMore);
        morePanel = findViewById(R.id.llChatMorePanel);

        btnSend.setOnClickListener(v -> sendMessage());
        btnMore.setOnClickListener(v -> toggleMorePanel());
        findViewById(R.id.btnChatCamera).setOnClickListener(v -> {
            currentCameraUri = createImageFile();
            takePicture.launch(currentCameraUri);
        });
        findViewById(R.id.btnChatAlbum).setOnClickListener(v -> pickImage.launch("image/*"));

        etInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateInputActions();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
        updateInputActions();

        new ChatPresenter(this, this, otherUser, productName).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (presenter != null) {
            presenter.markRead();
            presenter.refresh();
        }
    }

    @Override
    public void showTitle(String title) {
        ((TextView) findViewById(R.id.tvChatTitle)).setText(title);
    }

    @Override
    public void showMessages(List<ChatMessage> newMessages, String currentUser,
            String currentNickname, String otherNickname) {
        if (adapter == null) {
            messages = newMessages;
            adapter = new ChatAdapter(messages, currentUser, currentNickname, otherNickname,
                    new ChatAdapter.MessageActionListener() {
                        @Override
                        public void onRecall(ChatMessage message) {
                            presenter.recallMessage(message);
                        }

                        @Override
                        public void onDelete(ChatMessage message) {
                            presenter.deleteMessage(message);
                        }
                    });
            rvMessages.setAdapter(adapter);
        } else {
            messages.clear();
            messages.addAll(newMessages);
            adapter.notifyDataSetChanged();
        }
        if (!messages.isEmpty()) {
            rvMessages.scrollToPosition(messages.size() - 1);
        }
    }

    private void sendMessage() {
        String text = etInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        presenter.sendMessage(text);
    }

    private void updateInputActions() {
        boolean hasText = !TextUtils.isEmpty(etInput.getText().toString().trim());
        btnSend.setVisibility(hasText ? View.VISIBLE : View.GONE);
        btnMore.setVisibility(hasText ? View.GONE : View.VISIBLE);
        if (hasText) {
            hideMorePanel();
        }
    }

    private void toggleMorePanel() {
        morePanel.setVisibility(morePanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    private void hideMorePanel() {
        morePanel.setVisibility(View.GONE);
    }

    private Uri createImageFile() {
        File imageDir = new File(getCacheDir(), "images");
        if (!imageDir.exists()) {
            imageDir.mkdirs();
        }
        File imageFile = new File(imageDir, "chat_" + System.currentTimeMillis() + ".jpg");
        return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", imageFile);
    }

    private Uri copyImageToCache(Uri sourceUri) {
        File imageDir = new File(getCacheDir(), "images");
        if (!imageDir.exists() && !imageDir.mkdirs()) {
            return null;
        }
        File imageFile = new File(imageDir, "chat_" + System.currentTimeMillis() + ".jpg");
        try (InputStream is = getContentResolver().openInputStream(sourceUri);
             OutputStream os = new FileOutputStream(imageFile)) {
            if (is == null) {
                return null;
            }
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.flush();
            return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", imageFile);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void clearInput() {
        etInput.setText("");
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void closePage() {
        finish();
    }
}
