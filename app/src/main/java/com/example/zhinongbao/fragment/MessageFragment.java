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

/**
 * ============================================================
 * 【消息列表 / Message】View（Fragment 界面）
 * 整体逻辑：onCreateView 加载布局 fragment_message；onViewCreated 配置
 *   RecyclerView 并启动 Presenter；onResume 每次回到前台刷新会话列表；
 *   showConversations 由 Presenter 回调，把会话数据交给 ConversationAdapter 渲染，
 *   空列表时显示「暂无消息」提示。点击某行跳 ChatActivity（带上对方账号），
 *   长按某行弹确认框删除整段会话。
 * 数据来源：MessagePresenter 经 repository/MessageRepository 读取；
 *   Repository 内部经 ContentProvider 访问 SQLite。本类不直接碰数据库。
 * 配合的文件：接口约定 MessageContract；业务 MessagePresenter；
 *   列表适配器 adapter/ConversationAdapter；数据模型 model/ConversationItem；
 *   布局 res/layout/fragment_message.xml；跳转页面 ChatActivity；
 *   确认弹窗工具 utils/DialogUtils。
 * MVP 数据流位置：本类是 View；数据流为 View 到 Presenter 到 Repository 到
 *   ContentProvider 到 SQLite，再回调本类刷新界面。
 * 提示：在 IDE 里搜索「消息」可看本组相关文件。
 * ============================================================
 */
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
