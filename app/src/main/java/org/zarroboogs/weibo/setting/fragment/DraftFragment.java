
package org.zarroboogs.weibo.setting.fragment;

import org.zarroboogs.utils.Constants;
import org.zarroboogs.weibo.BeeboApplication;
import org.zarroboogs.weibo.R;
import org.zarroboogs.weibo.activity.WriteCommentActivity;
import org.zarroboogs.weibo.activity.WriteReplyToCommentActivity;
import org.zarroboogs.weibo.activity.WriteRepostActivity;
import org.zarroboogs.weibo.activity.WriteWeiboActivity;
import org.zarroboogs.weibo.asynctask.MyAsyncTask;
import org.zarroboogs.weibo.bean.AccountBean;
import org.zarroboogs.weibo.bean.CommentDraftBean;
import org.zarroboogs.weibo.bean.DraftListViewItemBean;
import org.zarroboogs.weibo.bean.ReplyDraftBean;
import org.zarroboogs.weibo.bean.RepostDraftBean;
import org.zarroboogs.weibo.bean.StatusDraftBean;
import org.zarroboogs.weibo.db.DraftDBManager;
import org.zarroboogs.weibo.db.table.DraftTable;
import org.zarroboogs.weibo.db.task.AccountDBTask;
import org.zarroboogs.weibo.support.utils.ThemeUtility;

import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: qii Date: 12-10-22
 */
public class DraftFragment extends ListFragment {

    private DraftAdapter adapter;

    private List<DraftListViewItemBean> list = new ArrayList<DraftListViewItemBean>();

    private DBTask task;

    private RemoveDraftDBTask removeTask;

    @Override
    public void onDetach() {
        super.onDetach();
        if (task != null) {
            task.cancel(true);
        }

        if (removeTask != null) {
            removeTask.cancel(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (task == null || task.getStatus() == MyAsyncTask.Status.FINISHED) {
            task = new DBTask();
            task.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);

        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adapter = new DraftAdapter();
        setListAdapter(adapter);

        getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        getListView().setMultiChoiceModeListener(new DraftMultiChoiceModeListener());
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DraftListViewItemBean item = list.get(position);
                Intent intent;
                switch (item.getType()) {
                    case DraftTable.TYPE_WEIBO:
                        AccountBean accountBean = AccountDBTask.getAccount(item.getStatusDraftBean().getAccountId());
                        intent = new Intent(getActivity(), WriteWeiboActivity.class);
                        intent.setAction(WriteWeiboActivity.ACTION_DRAFT);
                        intent.putExtra("draft", item.getStatusDraftBean());
                        intent.putExtra(Constants.ACCOUNT, accountBean);
                        startActivity(intent);
                        break;

                    case DraftTable.TYPE_REPOST:
                        accountBean = AccountDBTask.getAccount(item.getRepostDraftBean().getAccountId());
                        RepostDraftBean repostDraftBean = list.get(position).getRepostDraftBean();
                        intent = new Intent(getActivity(), WriteRepostActivity.class);
                        intent.setAction(WriteRepostActivity.ACTION_DRAFT);
                        intent.putExtra("draft", repostDraftBean);
                        intent.putExtra(Constants.ACCOUNT, accountBean);
                        startActivity(intent);
                        break;
                    case DraftTable.TYPE_COMMENT:
                        CommentDraftBean commentDraftBean = list.get(position).getCommentDraftBean();
                        intent = new Intent(getActivity(), WriteCommentActivity.class);
                        intent.setAction(WriteCommentActivity.ACTION_DRAFT);
                        intent.putExtra("draft", commentDraftBean);
                        startActivity(intent);
                        break;
                    case DraftTable.TYPE_REPLY:
                        ReplyDraftBean replyDraftBean = list.get(position).getReplyDraftBean();
                        intent = new Intent(getActivity(), WriteReplyToCommentActivity.class);
                        intent.setAction(WriteReplyToCommentActivity.ACTION_DRAFT);
                        intent.putExtra("draft", replyDraftBean);
                        startActivity(intent);
                        break;
                }

            }
        });
    }

    class DraftMultiChoiceModeListener implements AbsListView.MultiChoiceModeListener {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.contextual_menu_draftfragment, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int itemId = item.getItemId();
			if (itemId == R.id.menu_remove) {
				if (removeTask == null || removeTask.getStatus() == MyAsyncTask.Status.FINISHED) {
				    removeTask = new RemoveDraftDBTask();
				    removeTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
				}
				mode.finish();
				return true;
			}
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            mode.setTitle(String.format(getString(R.string.have_selected),
                    String.valueOf(getListView().getCheckedItemCount())));
            adapter.notifyDataSetChanged();
        }
    }

    private class RemoveDraftDBTask extends MyAsyncTask<Void, List<DraftListViewItemBean>, List<DraftListViewItemBean>> {

        Set<String> set = new HashSet<String>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            long[] ids = getListView().getCheckedItemIds();
            for (long id : ids) {
                set.add(String.valueOf(id));
            }
        }

        @Override
        protected List<DraftListViewItemBean> doInBackground(Void... params) {
            return DraftDBManager.getInstance().removeAndGet(set, BeeboApplication.getInstance().getAccountBean().getUid());
        }

        @Override
        protected void onPostExecute(List<DraftListViewItemBean> result) {
            list = result;
            adapter.notifyDataSetChanged();
        }
    }

    class DBTask extends MyAsyncTask<Void, List<DraftListViewItemBean>, List<DraftListViewItemBean>> {

        @Override
        protected List<DraftListViewItemBean> doInBackground(Void... params) {
            List<DraftListViewItemBean> set = DraftDBManager.getInstance().getDraftList(
                    BeeboApplication.getInstance().getAccountBean().getUid());

            return set;
        }

        @Override
        protected void onPostExecute(List<DraftListViewItemBean> set) {
            super.onPostExecute(set);
            list.clear();
            list.addAll(set);
            adapter.notifyDataSetChanged();
        }
    }

    class DraftAdapter extends BaseAdapter {

        int checkedBG;

        int defaultBG;

        public DraftAdapter() {
            defaultBG = getResources().getColor(R.color.transparent);
            checkedBG = ThemeUtility.getColor(getActivity(), R.attr.listview_checked_color);
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return Long.valueOf(list.get(position).getId());
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View view = getActivity().getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
            TextView tv = (TextView) view;
            tv.setBackgroundColor(defaultBG);
            if (getListView().getCheckedItemPositions().get(position)) {
                tv.setBackgroundColor(checkedBG);
            }

            int type = list.get(position).getType();
            switch (type) {
                case DraftTable.TYPE_WEIBO:
                    StatusDraftBean bean = list.get(position).getStatusDraftBean();
                    tv.setText(bean.getContent());
                    break;
                case DraftTable.TYPE_REPOST:
                    RepostDraftBean repostDraftBean = list.get(position).getRepostDraftBean();
                    tv.setText(repostDraftBean.getContent());
                    break;
                case DraftTable.TYPE_COMMENT:
                    CommentDraftBean commentDraftBean = list.get(position).getCommentDraftBean();
                    tv.setText(commentDraftBean.getContent());
                    break;
                case DraftTable.TYPE_REPLY:
                    ReplyDraftBean replyDraftBean = list.get(position).getReplyDraftBean();
                    tv.setText(replyDraftBean.getContent());
                    break;
            }

            return view;
        }
    }
}
