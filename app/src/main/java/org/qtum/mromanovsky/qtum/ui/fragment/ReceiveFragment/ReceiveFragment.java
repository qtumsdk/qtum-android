package org.qtum.mromanovsky.qtum.ui.fragment.ReceiveFragment;


import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.qtum.mromanovsky.qtum.R;
import org.qtum.mromanovsky.qtum.ui.fragment.BaseFragment.BaseFragment;

import butterknife.BindView;
import butterknife.OnClick;

public class ReceiveFragment extends BaseFragment implements ReceiveFragmentView {

    private ReceiveFragmentPresenterImpl mReceiveFragmentPresenter;

    @BindView(R.id.iv_qr_code)
    ImageView mImageViewQrCode;
    @BindView(R.id.et_amount)
    TextInputEditText mTextInputEditTextAmount;
    @BindView(R.id.tv_single_string)
    TextView mTextViewAddress;
    @BindView(R.id.bt_copy_wallet_address)
    Button mButtonCopyWalletAddress;
    @BindView(R.id.bt_choose_another_address)
    Button mButtonChooseAnotherAddress;
    @BindView(R.id.ibt_back)
    ImageButton mImageButtonBack;
    @BindView(R.id.tv_total_balance_number)
    TextView mTextViewTotalBalanceNumber;
    @BindView(R.id.rl_receive)
    RelativeLayout mRelativeLayoutBase;
    @BindView(R.id.cl_receive)
    CoordinatorLayout mCoordinatorLayout;

    @OnClick({R.id.bt_copy_wallet_address,R.id.bt_choose_another_address,R.id.ibt_back})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bt_copy_wallet_address:
                getPresenter().onCopyWalletAddressClick();
                break;
            case R.id.bt_choose_another_address:
                getPresenter().onChooseAnotherAddressClick();
                break;
            case R.id.ibt_back:
                getFragmentActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                getActivity().onBackPressed();
                break;
        }
    }

    public static ReceiveFragment newInstance() {

        Bundle args = new Bundle();

        ReceiveFragment fragment = new ReceiveFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void createPresenter() {
        mReceiveFragmentPresenter = new ReceiveFragmentPresenterImpl(this);
    }

    @Override
    protected ReceiveFragmentPresenterImpl getPresenter() {
        return mReceiveFragmentPresenter;
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_receive;
    }

    @Override
    public void initializeViews() {
        mTextInputEditTextAmount.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if(i == EditorInfo.IME_ACTION_DONE) {
                    getPresenter().changeAmount(mTextInputEditTextAmount.getText().toString());
                    mRelativeLayoutBase.requestFocus();
                    return false;
                }
                return false;
            }
        });

        mRelativeLayoutBase.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b) {
                    getPresenter().changeAmount(mTextInputEditTextAmount.getText().toString());
                    hideKeyBoard();
                }
            }
        });
    }

    @Override
    public void setSoftMode() {
        super.setSoftMode();
        getFragmentActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    }

    @Override
    public void setQrCode(Bitmap bitmap) {
        mImageViewQrCode.setImageBitmap(bitmap);
    }

    @Override
    public void setAddressInTV(String s) {
        mTextViewAddress.setText(s);
    }

    @Override
    public void setBalance(String balance) {
        mTextViewTotalBalanceNumber.setText(String.valueOf(balance)+" QTUM");
    }

    @Override
    public void showToast() {
        Snackbar.make(mCoordinatorLayout, "Coped", Snackbar.LENGTH_SHORT).show();
    }

}