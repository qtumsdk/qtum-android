package org.qtum.wallet.ui.fragment.token_cash_management_fragment;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import org.qtum.wallet.R;
import org.qtum.wallet.dataprovider.services.update_service.listeners.TokenBalanceChangeListener;
import org.qtum.wallet.dataprovider.rest_api.QtumService;
import org.qtum.wallet.datastorage.KeyStorage;
import org.qtum.wallet.model.DeterministicKeyWithTokenBalance;
import org.qtum.wallet.model.contract.ContractMethodParameter;
import org.qtum.wallet.model.contract.Token;
import org.qtum.wallet.model.gson.SendRawTransactionRequest;
import org.qtum.wallet.model.gson.SendRawTransactionResponse;
import org.qtum.wallet.model.gson.UnspentOutput;
import org.qtum.wallet.model.gson.token_balance.Balance;
import org.qtum.wallet.model.gson.token_balance.TokenBalance;
import org.qtum.wallet.ui.fragment.qtum_cash_management_fragment.AddressListFragmentPresenter;
import org.qtum.wallet.ui.base.base_fragment.BaseFragment;
import org.qtum.wallet.ui.base.base_fragment.BaseFragmentPresenterImpl;
import org.qtum.wallet.ui.fragment.send_fragment.SendFragment;
import org.qtum.wallet.ui.fragment.send_fragment.SendFragmentInteractorImpl;
import org.qtum.wallet.utils.ContractBuilder;
import org.qtum.wallet.utils.CurrentNetParams;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.script.Script;
import org.spongycastle.util.encoders.Hex;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by kirillvolkov on 03.08.17.
 */

public class AdressesListFragmentTokenPresenter extends BaseFragmentPresenterImpl implements Runnable {

    AdressesListFragmentTokenView view;
    public List<DeterministicKeyWithTokenBalance> items = new ArrayList<>();
    private Token token;
    private String currency;
    List<DeterministicKey> addrs;
    public DeterministicKeyWithTokenBalance keyWithTokenBalanceFrom;

    TokenBalance tokenBalance;

    public AdressesListFragmentTokenPresenter(AdressesListFragmentTokenView view){
        this.view = view;
    }

    @Override
    public AdressesListFragmentTokenView getView() {
        return view;
    }

    public List<DeterministicKey> getAdresses(){
        if(addrs == null){
            addrs = KeyStorage.getInstance().getKeyList(10);
        }
        return addrs;
    }

    @Override
    public void onViewCreated() {
        super.onViewCreated();

        getView().getSocketInstance().addTokenBalanceChangeListener(token.getContractAddress(), new TokenBalanceChangeListener() {
            @Override
            public void onBalanceChange(TokenBalance tokenBalance) {
                getView().getSocketInstance().removeTokenBalanceChangeListener(tokenBalance.getContractAddress());
                AdressesListFragmentTokenPresenter.this.tokenBalance = tokenBalance;
                processTokenBalances(tokenBalance);
                getView().getHandler().post(AdressesListFragmentTokenPresenter.this);
            }
        });

    }

    @Override
    public void onPause(Context context) {
        getView().getHandler().removeCallbacks(this);
        super.onPause(context);
    }

    @Override
    public void onResume(Context context) {
        super.onResume(context);
    }

    private void processTokenBalances(TokenBalance balance){
        for (DeterministicKey item : getAdresses()){
            DeterministicKeyWithTokenBalance deterministicKeyWithTokenBalance = new DeterministicKeyWithTokenBalance(item);
            items.add(deterministicKeyWithTokenBalance);
            processTokenBalace(deterministicKeyWithTokenBalance, balance);
        }
    }

    private void processTokenBalace(DeterministicKeyWithTokenBalance deterministicKeyWithTokenBalance, TokenBalance balance){
        for (Balance bal : balance.getBalances()){
            if(deterministicKeyWithTokenBalance.getAddress().equals(bal.getAddress())){
                deterministicKeyWithTokenBalance.addBalance(bal.getBalance());
            }
        }
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public void setCurrency(String currency){
        this.currency = currency;
    }

    public String getCurrency() {
        return currency;
    }


    @Override
    public void run() {
        if(items != null && !TextUtils.isEmpty(currency)) {
            getView().updateAddressList(items, currency);
        }
    }

    public void transfer(DeterministicKeyWithTokenBalance keyWithBalanceTo, final DeterministicKeyWithTokenBalance keyWithTokenBalanceFrom, String amountString, final AddressListFragmentPresenter.TransferListener transferListener) {

        if(TextUtils.isEmpty(amountString)){
            getView().setAlertDialog(getView().getContext().getResources().getString(R.string.error),
                    getView().getContext().getResources().getString(R.string.enter_valid_amount_value),
                    getView().getContext().getResources().getString(R.string.ok),
                    BaseFragment.PopUpType.error);
            return;
        }

        if(Float.valueOf(amountString) <= 0){
            getView().setAlertDialog(getView().getContext().getResources().getString(R.string.error),
                    getView().getContext().getResources().getString(R.string.transaction_amount_cant_be_zero),
                    getView().getContext().getResources().getString(R.string.ok),
                    BaseFragment.PopUpType.error);
            return;
        }

        getView().hideTransferDialog();

        if (tokenBalance == null || tokenBalance.getBalanceForAddress(keyWithTokenBalanceFrom.getAddress()) == null || tokenBalance.getBalanceForAddress(keyWithTokenBalanceFrom.getAddress()).getBalance().floatValue() < Float.valueOf(amountString)){
            getView().dismissProgressDialog();
            getView().setAlertDialog(getView().getContext().getString(R.string.error), getView().getContext().getString(R.string.you_have_insufficient_funds_for_this_transaction), "Ok", BaseFragment.PopUpType.error);
            return;
        }

        getView().getMainActivity().setIconChecked(3);
        Fragment fragment = SendFragment.newInstance(keyWithTokenBalanceFrom.getAddress(),keyWithBalanceTo.getAddress(),amountString,token.getContractAddress(),getView().getContext());
        getView().getMainActivity().setRootFragment(fragment);
        getView().openRootFragment(fragment);

    }

    public int getDecimalUnits() {
        return token.getDecimalUnits();
    }
}
