package org.qtum.wallet.ui.fragment.receive_fragment;

import android.graphics.Bitmap;
import android.support.v4.app.Fragment;

import org.qtum.wallet.ui.base.base_fragment.BaseFragmentView;


interface ReceiveFragmentView extends BaseFragmentView {
    void setQrCode(Bitmap bitmap);
    void showSpinner();
    void hideSpinner();
    void setUpAddress(String s);
    void updateBalance(String balance, String unconfirmedBalance);
    void showToast();
    void openFragmentForResult(Fragment fragment);
    Bitmap getQrCode();
    String getTokenBalance();
}
