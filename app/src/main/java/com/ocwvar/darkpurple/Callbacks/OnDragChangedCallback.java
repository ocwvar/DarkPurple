package com.ocwvar.darkpurple.Callbacks;

import android.support.v7.widget.RecyclerView;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Callbacks
 * Data: 2016/8/16 16:46
 * Project: DarkPurple
 * RecyclerView 项目被拖动响应回调
 */
public interface OnDragChangedCallback {

    /**
     * 当Item被拖动更换位置的回调
     *
     * @param originalPosition Item原本位置
     * @param targetPosition   Item要转移到的位置
     */
    void onItemPositionChange(RecyclerView.ViewHolder viewHolder, int originalPosition, int targetPosition);

    /**
     * 当Item被Swipe动作删除的时候的回调
     *
     * @param position 被操作的Item位置
     */
    void onItemDelete(int position);

}
