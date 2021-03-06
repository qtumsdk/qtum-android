package org.qtum.wallet.ui.fragment.transaction_fragment.dark;

import android.view.View;

import org.qtum.wallet.R;
import org.qtum.wallet.ui.fragment.transaction_fragment.transaction_detail_fragment.TransactionDetailFragment;
import org.qtum.wallet.ui.fragment.transaction_fragment.TransactionFragment;
import org.qtum.wallet.utils.FontTextView;

import java.util.List;

import butterknife.BindView;

/**
 * Created by kirillvolkov on 11.07.17.
 */

public class TransactionFragmentDark extends TransactionFragment {

    @BindView(R.id.tv_time_type)
    FontTextView mTextViewTimeType;

    @Override
    protected int getLayout() {
        return R.layout.fragment_transaction;
    }

    @Override
    public void setUpTransactionData(String value, String receivedTime, List<String> from, List<String> to, boolean confirmed) {
        setTransactionData(value, receivedTime);
        notConfFlag.setVisibility((!confirmed)? View.VISIBLE : View.INVISIBLE);

        if (Double.valueOf(value) > 0) {
            mTextViewTimeType.setText(R.string.received_time);
        } else {
            mTextViewTimeType.setText(R.string.sent_time);
        }
    }

    @Override
    public void initializeViews() {
        super.initializeViews();
        switch (getArguments().getInt(TransactionDetailFragment.ACTION)){
            case TransactionDetailFragment.ACTION_FROM:
                mTextViewTimeType.setText(R.string.received_time);
                break;
            case TransactionDetailFragment.ACTION_TO:
                mTextViewTimeType.setText(R.string.sent_time);
                break;
        }
    }
}
