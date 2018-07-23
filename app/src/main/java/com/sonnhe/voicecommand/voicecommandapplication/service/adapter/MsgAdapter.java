package com.sonnhe.voicecommand.voicecommandapplication.service.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sonnhe.voicecommand.voicecommandapplication.R;
import com.sonnhe.voicecommand.voicecommandapplication.model.Msg;

import java.util.List;

public class MsgAdapter extends RecyclerView.Adapter<MsgAdapter.ViewHolder> {
    private List<Msg> mMsgList;
    private OnClickCallBack mOnClickListener;

    public interface OnClickCallBack {
        void callback(int position, String text);
    }

    public MsgAdapter(List<Msg> msgList) {
        mMsgList = msgList;
    }

    public void setOnClickListener(OnClickCallBack onClickListener) {
        mOnClickListener = onClickListener;
    }

    @NonNull
    @Override
    public MsgAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.msg_item, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MsgAdapter.ViewHolder holder, int position) {
        Msg msg = mMsgList.get(position);
        holder.holderPosition = position;
        if (msg.getType() == Msg.TYPE_SENT) {
            //如果是收到的消息，则显示左边的消息布局，将右边的消息布局隐藏
            holder.mLeftLayout.setVisibility(View.VISIBLE);
            holder.mRightLayout.setVisibility(View.GONE);
            String content = msg.getContent();
//            String[] split = content.split("\n");
//            holder.mLeftMsg.setText(split[0]);
            holder.text = content;
            holder.mLeftMsg.setText(content);
        } else if (msg.getType() == Msg.TYPE_RECEIVED) {
            // 如果是发出的消息，则显示右边的消息布局，将左边的消息布局隐藏
            holder.mRightLayout.setVisibility(View.VISIBLE);
            holder.mLeftLayout.setVisibility(View.GONE);
            String content = msg.getContent();
//            String[] split = content.split("\n");
//            holder.mRightMsg.setText(split[0]);
            holder.text = content;
            holder.mRightMsg.setText(content);
        }
    }

    @Override
    public int getItemCount() {
        return mMsgList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        LinearLayout mLeftLayout;
        LinearLayout mRightLayout;
        TextView mLeftMsg;
        TextView mRightMsg;
        String text;
        int holderPosition;

        ViewHolder(@NonNull final View itemView) {
            super(itemView);
            mLeftLayout = itemView.findViewById(R.id.left_layout);
            mRightLayout = itemView.findViewById(R.id.right_layout);
            mLeftMsg = itemView.findViewById(R.id.left_msg);
            mRightMsg = itemView.findViewById(R.id.right_msg);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mOnClickListener != null) {
                mOnClickListener.callback(holderPosition, text);
            }
        }
    }
}
