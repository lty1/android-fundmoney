package neo.smemo.info.activity;

import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import neo.smemo.info.R;
import neo.smemo.info.action.FundAction;
import neo.smemo.info.adapter.FundAdapter;
import neo.smemo.info.base.BaseAction;
import neo.smemo.info.base.BaseFragmentActivity;
import neo.smemo.info.bean.FundBean;
import neo.smemo.info.model.FundComparator;
import neo.smemo.info.util.LogHelper;
import neo.smemo.info.util.system.SystemUtil;
import neo.smemo.info.util.view.AnnotationActionBar;
import neo.smemo.info.util.view.AnnotationView;

/**
 * 主页
 * Created by suzhenpeng on 2015/9/23.
 */
@AnnotationActionBar(abLayout = R.layout.actionbar_index, abTitle = R.string.app_name)
@AnnotationView(R.layout.activity_main)
public class MainActivity extends BaseFragmentActivity {

    @AnnotationView(R.id.recyclerView)
    private RecyclerView recyclerView;
    @AnnotationView(R.id.swipeRefresh)
    private SwipeRefreshLayout swipeRefreshLayout;
    private FundAdapter fundAdapter;

    private ArrayList<FundBean> fundBeanArrayList;
    private MyHandler handler;
    private static final int REQUEST_SUCCESS = 1;

    private boolean isNewrokLoadDone = false;

    private int sort = 1;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        handler = new MyHandler(this);
//        生成actionbar，布局和标题。方法同@AnnotationActionBar相同
//        initActionBar(R.layout.actionbar_index, R.string.app_name);

        fundBeanArrayList = new ArrayList<>();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        fundAdapter = new FundAdapter(this, fundBeanArrayList);
        recyclerView.setAdapter(fundAdapter);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(true);
                loadData();
            }
        });

        loadLocalData();
        loadData();
    }

    /**
     * 加载本地数据库
     */
    private void loadLocalData() {
        FundAction.getFundListByDb(new BaseAction.ActionSuccessResponse() {
            @Override
            public void success(Object object) {
                //如果网络数据优先于本地数据库加载，不再加载本地数据库
                if (isNewrokLoadDone) return;
                ArrayList<FundBean> tmp = (ArrayList<FundBean>) object;
                fundBeanArrayList.clear();
                for (FundBean bean : tmp)
                    fundBeanArrayList.add(bean);
                LogHelper.Log_I(LOG_TAG, "获取本地数据" + tmp.size() + "条");
                SystemUtil.sendMessage(handler, REQUEST_SUCCESS);
            }

            @Override
            public void failure(int code, String message) {

            }
        });
    }

    /**
     * 加载网络数据
     */
    private void loadData() {
        FundAction.getFundList(new BaseAction.ActionSuccessResponse() {
            @Override
            public void success(Object object) {
                ArrayList<FundBean> tmp = (ArrayList<FundBean>) object;
                fundBeanArrayList.clear();
                for (FundBean bean : tmp)
                    fundBeanArrayList.add(bean);
                SystemUtil.sendMessage(handler, REQUEST_SUCCESS);
                isNewrokLoadDone = true;
            }

            @Override
            public void failure(int code, String message) {
                swipeRefreshLayout.setRefreshing(false);
                //显示错误信息
                showMessage(message);
            }
        });
    }

    @Override
    public void onLeftClick() {
        super.onLeftClick();
        finish();
    }

    @Override
    public void onRightClick() {
        super.onRightClick();
        Comparator<FundBean> comp = new FundComparator(sort == 1 ? FundComparator.SORT_BIG : FundComparator.SORT_SMALL);
        sort = sort * -1;
        Collections.sort(fundBeanArrayList, comp);
        fundAdapter.notifyDataSetChanged();
    }

    static class MyHandler extends Handler {
        WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mActivity.get().handleMessage(msg);
        }
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case REQUEST_SUCCESS:
                swipeRefreshLayout.setRefreshing(false);
                fundAdapter.notifyDataSetChanged();
                break;
        }
    }
}
