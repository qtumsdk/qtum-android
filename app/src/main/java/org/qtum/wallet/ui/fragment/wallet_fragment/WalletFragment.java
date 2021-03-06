package org.qtum.wallet.ui.fragment.wallet_fragment;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import org.qtum.wallet.R;
import org.qtum.wallet.model.gson.history.History;
import org.qtum.wallet.ui.fragment_factory.Factory;
import org.qtum.wallet.ui.base.base_fragment.BaseFragment;
import org.qtum.wallet.utils.ClipboardUtils;
import org.qtum.wallet.utils.FontTextView;

import java.util.List;
import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnLongClick;

public abstract class WalletFragment extends BaseFragment implements WalletFragmentView, TransactionClickListener{
    protected WalletFragmentPresenterImpl mWalletFragmentPresenter;
    protected TransactionAdapter mTransactionAdapter;
    protected LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(getActivity());
    protected int visibleItemCount;
    protected int totalItemCount;
    protected int pastVisibleItems;
    protected boolean mLoadingFlag = false;
    @BindView(R.id.recycler_view) protected RecyclerView mRecyclerView;
    @BindView(R.id.swipe_refresh) protected SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.app_bar) protected AppBarLayout mAppBarLayout;
    @BindView(R.id.bt_qr_code) protected ImageButton mButtonQrCode;
    @BindView(R.id.tv_wallet_name) protected TextView mTextViewWalletName;
    @BindView(R.id.fade_divider_root) RelativeLayout fadeDividerRoot;
    @BindView(R.id.tv_public_key) protected FontTextView publicKeyValue;
    //HEADER
    @BindView(R.id.tv_balance) protected FontTextView balanceValue;
    @BindView(R.id.ll_balance) protected LinearLayout balanceLayout;
    @BindView(R.id.available_balance_title) protected FontTextView balanceTitle;
    @BindView(R.id.tv_unconfirmed_balance) protected FontTextView uncomfirmedBalanceValue;
    @BindView(R.id.unconfirmed_balance_title) protected FontTextView uncomfirmedBalanceTitle;
    //HEADER
    @BindView(R.id.balance_view) protected FrameLayout balanceView;
    @BindView(R.id.toolbar_layout) protected CollapsingToolbarLayout collapsingToolbar;
    @OnClick({R.id.ll_receive, R.id.iv_receive})
    public void onReceiveClick(){
        getPresenter().onReceiveClick();
    }

    @OnClick(R.id.iv_choose_address)
    public void onChooseAddressClick(){
        getPresenter().onChooseAddressClick();
    }

    @OnClick({R.id.bt_qr_code})
    public void onClick(View view) {
        getPresenter().onClickQrCode();
    }

    @OnLongClick(R.id.tv_public_key)
    public boolean onAddressLongClick(){
        ClipboardUtils.copyToClipBoard(getContext(), publicKeyValue.getText().toString(), new ClipboardUtils.CopyCallback() {
            @Override
            public void onCopyToClipBoard() {
                showToast(getString(R.string.copied));
            }
        });
        return true;
    }

    public static BaseFragment newInstance(Context context) {
        Bundle args = new Bundle();
        BaseFragment fragment = Factory.instantiateFragment(context, WalletFragment.class);
        fragment.setArguments(args);
        return fragment;
    }

    public static float convertDpToPixel(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

    protected float percents = 1;

    public int getTotalRange() {
        return mAppBarLayout.getTotalScrollRange();
    }

    @Override
    public void initializeViews() {
        super.initializeViews();
        mRecyclerView.setLayoutManager(mLinearLayoutManager);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(dy>0){
                    if(!mLoadingFlag) {
                        visibleItemCount = mLinearLayoutManager.getChildCount();
                        totalItemCount = mLinearLayoutManager.getItemCount();
                        pastVisibleItems = mLinearLayoutManager.findFirstVisibleItemPosition();
                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount-1) {
                            getPresenter().onLastItem(totalItemCount-1);
                        }
                    }
                }
            }
        });
        mSwipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(getContext(),R.color.colorAccent));
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getPresenter().onRefresh();
            }
        });
        getPresenter().notifyHeader();
    }

    @Override
    protected WalletFragmentPresenterImpl getPresenter() {
        return mWalletFragmentPresenter;
    }

    @Override
    protected void createPresenter() {
        mWalletFragmentPresenter = new WalletFragmentPresenterImpl(this);
    }

    @Override
    public void onTransactionClick(int adapterPosition) {
        getPresenter().openTransactionFragment(adapterPosition);
    }

    public void updateHistory(TransactionAdapter adapter) {
        mTransactionAdapter = adapter;
        mRecyclerView.setAdapter(mTransactionAdapter);
        mSwipeRefreshLayout.setRefreshing(false);
        mLoadingFlag = false;
    }

    protected TransactionClickListener getAdapterListener(){
        return this;
    }

    @Override
    public void setAdapterNull() {
    }

    @Override
    public void updatePubKey(String pubKey) {
        publicKeyValue.setText(pubKey);
    }

    @Override
    public void startRefreshAnimation() {
        mSwipeRefreshLayout.setRefreshing(true);
    }

    @Override
    public void stopRefreshRecyclerAnimation() {
        if(mSwipeRefreshLayout!=null) {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }
    @Override
    public void addHistory(int positionStart, int itemCount, List<History> historyList) {
        mTransactionAdapter.setHistoryList(historyList);
        mLoadingFlag = false;
        mTransactionAdapter.notifyItemRangeChanged(positionStart,itemCount);
    }

    @Override
    public void loadNewHistory() {
        mLoadingFlag = true;
        mTransactionAdapter.notifyItemChanged(totalItemCount-1);
    }

    @Override
    public void notifyNewHistory() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTransactionAdapter.notifyDataSetChanged();
            }
        });
    }

    public void initBalanceListener(){
        getPresenter().initBalanceListener();
    }

    @Override
    public void notifyConfirmHistory(final int notifyPosition) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTransactionAdapter.notifyItemChanged(notifyPosition);
            }
        });
    }
}