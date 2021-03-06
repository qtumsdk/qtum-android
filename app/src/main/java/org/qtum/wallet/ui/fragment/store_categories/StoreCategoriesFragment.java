package org.qtum.wallet.ui.fragment.store_categories;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import org.qtum.wallet.R;
import org.qtum.wallet.ui.fragment_factory.Factory;
import org.qtum.wallet.ui.base.base_fragment.BaseFragment;
import org.qtum.wallet.utils.SearchBar;
import org.qtum.wallet.utils.SearchBarListener;

import butterknife.BindView;
import butterknife.OnClick;


public abstract class StoreCategoriesFragment extends BaseFragment implements StoreCategoriesView, SearchBarListener {

    private StoreCategoriesPresenter presenter;

    protected StoreCategoriesAdapter adapter;

    @OnClick(R.id.ibt_back)
    public void onBackClick(){
        getActivity().onBackPressed();
    }

    @BindView(R.id.content_list)
    protected
    RecyclerView contentList;


    @BindView(R.id.search_bar)
    SearchBar searchBar;

    public static BaseFragment newInstance(Context context) {
        Bundle args = new Bundle();
        BaseFragment fragment = Factory.instantiateFragment(context, StoreCategoriesFragment.class);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void initializeViews() {
        super.initializeViews();
        contentList.setLayoutManager(new LinearLayoutManager(getContext()));
        searchBar.setListener(this);
    }

    @Override
    protected void createPresenter() {
        presenter = new StoreCategoriesPresenter(this);
    }

    @Override
    protected StoreCategoriesPresenter getPresenter() {
        return presenter;
    }

    @Override
    public void onActivate() {

    }

    @Override
    public void onDeactivate() {

    }

    @Override
    public void onRequestSearch(String filter) {
        adapter.updateItems(presenter.getFilter(filter));
    }
}
