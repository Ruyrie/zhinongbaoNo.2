package com.example.zhinongbao.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.activity.ChatActivity;
import com.example.zhinongbao.R;
import com.example.zhinongbao.adapter.ConversationAdapter;
import com.example.zhinongbao.base.BaseMvpFragment;
import com.example.zhinongbao.model.ConversationItem;
import com.example.zhinongbao.mvp.message.MessageContract;
import com.example.zhinongbao.mvp.message.MessagePresenter;
import com.example.zhinongbao.utils.DialogUtils;
import java.util.List;

public class MessageFragment extends BaseMvpFragment<MessageContract.Presenter>
        implements MessageContract.View {

    private RecyclerView rvConversations;
    private TextView tvEmpty;
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
        rvConversations = view.findViewById(R.id.rvConversations);
        tvEmpty = view.findViewById(R.id.tvMsgEmpty);
        rvConversations.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvConversations.addItemDecoration(
                new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        new MessagePresenter(requireContext(), this).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (presenter != null) presenter.refresh();
    }

    @Override
    public void showConversations(List<ConversationItem> newItems) {
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
                DialogUtils.showConfirm(requireContext(), "删除对话",
                        "确认删除与 " + displayName + " 的全部消息？",
                        "取消", "删除", true, () -> {
                            presenter.deleteConversation(item.otherUser);
                            return true;
                        });
            });
            rvConversations.setAdapter(adapter);
        } else {
            items.clear();
            items.addAll(newItems);
            adapter.notifyDataSetChanged();
        }
    }
}
