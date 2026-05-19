package com.example.zhinongbao;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.ChatAdapter;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.model.ChatMessage;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    /** 默认客服账号（商家） */
    public static final String SHOP_USERNAME = "admin";

    private DataManager dm;
    private String currentUser;
    private String currentNickname;
    private String otherUser;
    private String otherNickname;
    private RecyclerView rvMessages;
    private ChatAdapter adapter;
    private List<ChatMessage> messages;
    private EditText etInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        dm = DataManager.getInstance(this);
        currentUser = dm.getLoggedUser();
        otherUser = getIntent().getStringExtra("other_user");
        if (otherUser == null) otherUser = SHOP_USERNAME;

        String productName = getIntent().getStringExtra("product_name");
        currentNickname = dm.getNickname(currentUser);
        if (currentNickname == null || currentNickname.isEmpty()) currentNickname = currentUser;
        otherNickname = dm.getNickname(otherUser);
        if (otherNickname == null || otherNickname.isEmpty()) otherNickname = otherUser;
        String title = otherNickname;
        if (productName != null && !productName.isEmpty()) {
            title = title + " · " + productName;
        }
        ((TextView) findViewById(R.id.tvChatTitle)).setText(title);

        findViewById(R.id.ivChatBack).setOnClickListener(v -> finish());

        rvMessages = findViewById(R.id.rvChatMessages);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));

        etInput = findViewById(R.id.etChatInput);
        Button btnSend = findViewById(R.id.btnChatSend);

        loadMessages();

        btnSend.setOnClickListener(v -> sendMessage());
        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Mark messages from other user as read
        dm.markMessagesRead(otherUser, currentUser);
        loadMessages();
    }

    private void loadMessages() {
        List<ChatMessage> newMessages = dm.getMessages(currentUser, otherUser);
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

        dm.sendMessage(currentUser, otherUser, text);
        etInput.setText("");
        loadMessages();
    }
}
