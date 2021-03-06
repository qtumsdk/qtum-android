package org.qtum.wallet.ui.fragment.qtum_cash_management_fragment;


import android.content.Context;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import org.qtum.wallet.R;
import org.qtum.wallet.dataprovider.rest_api.QtumService;
import org.qtum.wallet.datastorage.KeyStorage;
import org.qtum.wallet.model.DeterministicKeyWithBalance;
import org.qtum.wallet.model.gson.SendRawTransactionRequest;
import org.qtum.wallet.model.gson.SendRawTransactionResponse;
import org.qtum.wallet.model.gson.UnspentOutput;
import org.qtum.wallet.ui.base.base_fragment.BaseFragment;
import org.qtum.wallet.ui.base.base_fragment.BaseFragmentPresenterImpl;
import org.qtum.wallet.ui.fragment.send_fragment.SendFragment;
import org.qtum.wallet.utils.CurrentNetParams;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
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


public class AddressListFragmentPresenter extends BaseFragmentPresenterImpl{

    private AddressListFragmentView mAddressListFragmentView;
    private AddressListFragmentInteractor mAddressListFragmentInteractor;
    private Context mContext;
    public List<DeterministicKeyWithBalance> mKeyWithBalanceList = new ArrayList<>();
    private int balanceCountToReceive;
    public DeterministicKeyWithBalance keyWithBalanceFrom;

    AddressListFragmentPresenter(AddressListFragmentView addressListFragmentView){
        mAddressListFragmentView = addressListFragmentView;
        mContext = getView().getContext();
        mAddressListFragmentInteractor = new AddressListFragmentInteractor(mContext);
    }

    @Override
    public AddressListFragmentView getView() {
        return mAddressListFragmentView;
    }

    @Override
    public void onViewCreated() {
        super.onViewCreated();
        balanceCountToReceive = getInteractor().getKeyList().size();
        getView().setProgressDialog();
        for(final DeterministicKey deterministicKey : getInteractor().getKeyList()){
            final DeterministicKeyWithBalance deterministicKeyWithBalance = new DeterministicKeyWithBalance(deterministicKey);
            QtumService.newInstance().getUnspentOutputs(deterministicKeyWithBalance.getAddress())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<List<UnspentOutput>>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onNext(List<UnspentOutput> unspentOutputs) {
                            for(Iterator<UnspentOutput> iterator = unspentOutputs.iterator(); iterator.hasNext();){
                                UnspentOutput unspentOutput = iterator.next();
                                if(!unspentOutput.isOutputAvailableToPay()){
                                    iterator.remove();
                                }
                            }
                            Collections.sort(unspentOutputs, new Comparator<UnspentOutput>() {
                                @Override
                                public int compare(UnspentOutput unspentOutput, UnspentOutput t1) {
                                    return unspentOutput.getAmount().doubleValue() < t1.getAmount().doubleValue() ? 1 : unspentOutput.getAmount().doubleValue() > t1.getAmount().doubleValue() ? -1 : 0;
                                }
                            });
                            deterministicKeyWithBalance.setUnspentOutputList(unspentOutputs);
                            BigDecimal balance = new BigDecimal("0");
                            BigDecimal amount;
                            for(UnspentOutput unspentOutput : unspentOutputs){
                                amount = new BigDecimal(String.valueOf(unspentOutput.getAmount()));
                                balance = balance.add(amount);
                            }
                            deterministicKeyWithBalance.setBalance(balance);
                            mKeyWithBalanceList.add(deterministicKeyWithBalance);
                            balanceCountToReceive--;
                            if(balanceCountToReceive==0){
                                Collections.sort(mKeyWithBalanceList, new Comparator<DeterministicKeyWithBalance>() {
                                    @Override
                                    public int compare(DeterministicKeyWithBalance deterministicKeyWithBalance, DeterministicKeyWithBalance t1) {
                                        return deterministicKeyWithBalance.getAddress().hashCode() < t1.getAddress().hashCode() ? 1 : deterministicKeyWithBalance.getAddress().hashCode() > t1.getAddress().hashCode() ? -1 : 0;
                                    }
                                });
                                getView().dismissProgressDialog();
                                getView().updateAddressList(mKeyWithBalanceList);
                            }
                        }
                    });

        }

    }

    public void transfer(DeterministicKeyWithBalance keyWithBalanceTo, DeterministicKeyWithBalance keyWithBalanceFrom, String amountString, TransferListener listener){

        if(TextUtils.isEmpty(amountString)){
            getView().setAlertDialog(mContext.getResources().getString(R.string.error),
                    mContext.getResources().getString(R.string.enter_valid_amount_value),
                    mContext.getResources().getString(R.string.ok),
                    BaseFragment.PopUpType.error);
            return;
        }

        if(Float.valueOf(amountString) <= 0){
            getView().setAlertDialog(mContext.getResources().getString(R.string.error),
                    mContext.getResources().getString(R.string.transaction_amount_cant_be_zero),
                    mContext.getResources().getString(R.string.ok),
                    BaseFragment.PopUpType.error);
            return;
        }


        getView().getMainActivity().setIconChecked(3);
        Fragment fragment = SendFragment.newInstance(keyWithBalanceFrom.getAddress(),keyWithBalanceTo.getAddress(),amountString,"",getView().getContext());
        getView().getMainActivity().setRootFragment(fragment);
        getView().openRootFragment(fragment);


//        List<UnspentOutput> unspentOutputs = keyWithBalanceFrom.getUnspentOutputList();
//
//        Transaction transaction = new Transaction(CurrentNetParams.getNetParams());
//        Address addressToSend;
//        BigDecimal bitcoin = new BigDecimal(100000000);
//        try {
//            addressToSend = keyWithBalanceTo.getKey().toAddress(CurrentNetParams.getNetParams());
//        } catch (AddressFormatException a) {
//            listener.onError(mContext.getString(R.string.invalid_qtum_address));
//            return;
//        }
//        ECKey myKey = KeyStorage.getInstance().getCurrentKey();
//        BigDecimal amount = new BigDecimal(amountString);
//        BigDecimal fee = new BigDecimal("0.1");
//
//        BigDecimal amountFromOutput = new BigDecimal("0.0");
//        BigDecimal overFlow = new BigDecimal("0.0");
//        transaction.addOutput(Coin.valueOf((long)(amount.multiply(bitcoin).doubleValue())), addressToSend);
//
//        amount = amount.add(fee);
//
//        for (UnspentOutput unspentOutput : unspentOutputs) {
//            overFlow = overFlow.add(unspentOutput.getAmount());
//            if (overFlow.doubleValue() >= amount.doubleValue()) {
//                break;
//            }
//        }
//        if (overFlow.doubleValue() < amount.doubleValue()) {
//            listener.onError(mContext.getString(R.string.you_have_insufficient_funds_for_this_transaction));
//            return;
//        }
//        BigDecimal delivery = overFlow.subtract(amount);
//        if (delivery.doubleValue() != 0.0) {
//            transaction.addOutput(Coin.valueOf((long)(delivery.multiply(bitcoin).doubleValue())), myKey.toAddress(CurrentNetParams.getNetParams()));
//        }
//
//        for (UnspentOutput unspentOutput : unspentOutputs) {
//            if(unspentOutput.getAmount().doubleValue() != 0.0)
//                for (DeterministicKey deterministicKey : KeyStorage.getInstance().getKeyList(100)) {
//                    if (Hex.toHexString(deterministicKey.getPubKeyHash()).equals(unspentOutput.getPubkeyHash())) {
//                        Sha256Hash sha256Hash = new Sha256Hash(Utils.parseAsHexOrBase58(unspentOutput.getTxHash()));
//                        TransactionOutPoint outPoint = new TransactionOutPoint(CurrentNetParams.getNetParams(), unspentOutput.getVout(), sha256Hash);
//                        Script script = new Script(Utils.parseAsHexOrBase58(unspentOutput.getTxoutScriptPubKey()));
//                        transaction.addSignedInput(outPoint, script, deterministicKey, Transaction.SigHash.ALL, true);
//                        amountFromOutput = amountFromOutput.add(unspentOutput.getAmount());
//                        break;
//                    }
//                }
//            if (amountFromOutput.doubleValue() >= amount.doubleValue()) {
//                break;
//            }
//        }
//
//
//        transaction.getConfidence().setSource(TransactionConfidence.Source.SELF);
//        transaction.setPurpose(Transaction.Purpose.USER_PAYMENT);
//
//        byte[] bytes = transaction.unsafeBitcoinSerialize();
//
//        String transactionHex = Hex.toHexString(bytes);
//
//        sendTx(transactionHex,listener);
    }

    private void sendTx(String txHex, final TransferListener listener){
        QtumService.newInstance().sendRawTransaction(new SendRawTransactionRequest(txHex, 1))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<SendRawTransactionResponse>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        listener.onError(e.getLocalizedMessage());
                    }

                    @Override
                    public void onNext(SendRawTransactionResponse sendRawTransactionResponse) {
                        listener.onSuccess();
                    }
                });
    }

    public AddressListFragmentInteractor getInteractor() {
        return mAddressListFragmentInteractor;
    }

    public interface TransferListener{
        void onSuccess();
        void onError(String errorText);
    }
}
