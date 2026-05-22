package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.ChatAdapter;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.ChatMessage;
import com.example.zhinongbao.mvp.chat.ChatContract;
import com.example.zhinongbao.mvp.chat.ChatPresenter;
import java.util.List;

public class ChatActivity extends BaseMvpActivity<ChatContract.Presenter>
        implements ChatContract.View {

    /** 默认客服账号（商家） */
    public static final String SHOP_USERNAME = "admin";

    private RecyclerView rvMessages;
    private ChatAdapter adapter;
    private List<ChatMessage> messages;
    private EditText etInput;

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
        Button btnSend = findViewById(R.id.btnChatSend);

        btnSend.setOnClickListener(v -> sendMessage());
        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

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
            adapter = new ChatAdapter(messages, currentUser, currentNickname, otherNickname);
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
