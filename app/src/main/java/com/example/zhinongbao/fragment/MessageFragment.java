package com.example.zhinongbao.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.ChatActivity;
import com.example.zhinongbao.R;
import com.example.zhinongbao.adapter.ConversationAdapter;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.model.ConversationItem;
import java.util.List;

public class MessageFragment extends Fragment {

    private RecyclerView rvConversations;
    private TextView tvEmpty;
    private DataManager dm;
    private ConversationAdapter adapter;
    private List<ConversationItem> items;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_message, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        dm = DataManager.getInstance(requireContext());
        rvConversations = view.findViewById(R.id.rvConversations);
        tvEmpty = view.findViewById(R.id.tvMsgEmpty);
        rvConversations.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvConversations.addItemDecoration(
                new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        loadConversations();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dm != null) loadConversations();
    }

    private void loadConversations() {
        String username = dm.getLoggedUser();
        if (username == null) return;

        List<ConversationItem> newItems = dm.getConversations(username);

        if (newItems.isEmpty()) {
            rvConversations.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvConversations.setVisibility(View.VISIBLE);
        }

        if (adapter == null) {
            items = newItems;
            adapter = new ConversationAdapter(items, item -> {
                Intent intent = new Intent(requireContext(), ChatActivity.class);
                intent.putExtra("other_user", item.otherUser);
                startActivity(intent);
            });
            adapter.setOnItemLongPressListener((item, position) -> {
                String displayName = (item.displayName != null && !item.displayName.isEmpty())
                        ? item.displayName : item.otherUser;
                new AlertDialog.Builder(requireContext())
                        .setTitle("删除对话")
                        .setMessage("确认删除与 " + displayName + " 的全部消息？")
                        .setPositiveButton("删除", (d, w) -> {
                            String currentUser = dm.getLoggedUser();
                            dm.deleteConversation(currentUser, item.otherUser);
                            loadConversations();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            });
            rvConversations.setAdapter(adapter);
        } else {
            items.clear();
            items.addAll(newItems);
            adapter.notifyDataSetChanged();
        }
    }
}
