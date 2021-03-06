package org.qtum.wallet.ui.fragment.contract_confirm_fragment;

import android.content.Context;
import android.support.v4.app.FragmentManager;

import org.qtum.wallet.R;
import org.qtum.wallet.dataprovider.rest_api.QtumService;
import org.qtum.wallet.datastorage.QtumNetworkState;
import org.qtum.wallet.model.contract.ContractMethodParameter;
import org.qtum.wallet.model.contract.Contract;
import org.qtum.wallet.model.contract.Token;
import org.qtum.wallet.model.gson.SendRawTransactionRequest;
import org.qtum.wallet.model.gson.SendRawTransactionResponse;
import org.qtum.wallet.model.gson.UnspentOutput;
import org.qtum.wallet.datastorage.KeyStorage;
import org.qtum.wallet.model.ContractTemplate;
import org.qtum.wallet.ui.base.base_fragment.BaseFragment;
import org.qtum.wallet.ui.base.base_fragment.BaseFragmentPresenterImpl;
import org.qtum.wallet.utils.ContractBuilder;
import org.qtum.wallet.datastorage.TinyDB;

import org.bitcoinj.script.Script;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public class ContractConfirmPresenterImpl extends BaseFragmentPresenterImpl implements ContractConfirmPresenter {

    private ContractConfirmView view;
    private ContractConfirmInteractorImpl interactor;
    private Context mContext;


    private String mContractTemplateUiid;

    private double minFee;
    private double maxFee = 0.2;

    private int minGasPrice;
    private int maxGasPrice = 120;

    private int minGasLimit = 100000;
    private int maxGasLimit = 5000000;


    private List<ContractMethodParameter> mContractMethodParameterList;

//    public void testClick(){
//        ContractBuilder contractBuilder = new ContractBuilder();
//        contractBuilder.testContractParameters();
//    }


    @Override
    public void initializeViews() {
        super.initializeViews();
        minFee = QtumNetworkState.newInstance().getFeePerKb().getFeePerKb().doubleValue();
        getView().updateFee(minFee,maxFee);
        minGasPrice = QtumNetworkState.newInstance().getDGPInfo().getMingasprice();
        getView().updateGasPrice(minGasPrice, maxGasPrice);
        getView().updateGasLimit(minGasLimit, maxGasLimit);
    }

    public void setContractMethodParameterList(List<ContractMethodParameter> contractMethodParameterList) {
        this.mContractMethodParameterList = contractMethodParameterList;
    }

    public List<ContractMethodParameter> getContractMethodParameterList() {
        return mContractMethodParameterList;
    }

    ContractConfirmPresenterImpl(ContractConfirmView view) {
        this.view = view;
        mContext = getView().getContext();
        interactor = new ContractConfirmInteractorImpl();
    }


    void confirmContract(final String uiid, final int gasLimit, final int gasPrice) {
        getView().setProgressDialog();
        mContractTemplateUiid = uiid;
        ContractBuilder contractBuilder = new ContractBuilder();
        contractBuilder.createAbiConstructParams(mContractMethodParameterList, uiid ,mContext)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        getView().dismissProgressDialog();
                        getView().setAlertDialog(mContext.getString(R.string.error), e.getMessage(),"Ok", BaseFragment.PopUpType.error);
                    }

                    @Override
                    public void onNext(String s) {
                        createTx(s,gasLimit,gasPrice);
                    }
                });
    }


    private void createTx(final String abiParams, final int gasLimit, final int gasPrice) {
        QtumService.newInstance().getUnspentOutputsForSeveralAddresses(KeyStorage.getInstance().getAddresses())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<UnspentOutput>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        getView().setAlertDialog(mContext.getString(R.string.error),e.getMessage(),"Ok", BaseFragment.PopUpType.error);
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
                        ContractBuilder contractBuilder = new ContractBuilder();
                        Script script = contractBuilder.createConstructScript(abiParams);
//TODO
                        String hash = contractBuilder.createTransactionHash(script,unspentOutputs,gasLimit, gasPrice,QtumNetworkState.newInstance().getFeePerKb().getFeePerKb(),"0.5",mContext);
                        sendTx(hash, "Stub!");
                    }
                });
    }

    private void sendTx(final String code, final String senderAddress) {
        QtumService.newInstance().sendRawTransaction(new SendRawTransactionRequest(code, 1))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<SendRawTransactionResponse>() {
                    @Override
                    public void onCompleted() {
                        getView().dismissProgressDialog();
                        getView().setAlertDialog(mContext.getString(R.string.contract_created_successfully), "", "OK", BaseFragment.PopUpType.confirm, new BaseFragment.AlertDialogCallBack() {
                            @Override
                            public void onButtonClick() {
                                FragmentManager fm = getView().getFragment().getFragmentManager();
                                int count = fm.getBackStackEntryCount()-2;
                                for(int i = 0; i < count; ++i) {
                                    fm.popBackStack();
                                }
                            }

                            @Override
                            public void onButton2Click() {

                            }
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        getView().dismissProgressDialog();
                        getView().setAlertDialog(mContext.getString(R.string.error),e.getMessage(),"OK", BaseFragment.PopUpType.error);
                    }

                    @Override
                    public void onNext(SendRawTransactionResponse sendRawTransactionResponse) {
                        TinyDB tinyDB = new TinyDB(mContext);
                        ArrayList<String> unconfirmedTokenTxHashList = tinyDB.getUnconfirmedContractTxHasList();
                        unconfirmedTokenTxHashList.add(sendRawTransactionResponse.getTxid());
                        tinyDB.putUnconfirmedContractTxHashList(unconfirmedTokenTxHashList);
                        String name = getView().getContractName();
                        for(ContractTemplate contractTemplate : tinyDB.getContractTemplateList()){
                            if(contractTemplate.getUuid().equals(mContractTemplateUiid)){
                                if(contractTemplate.getContractType().equals("token")){
                                    Token token = new Token(ContractBuilder.generateContractAddress(sendRawTransactionResponse.getTxid()), mContractTemplateUiid, false, null, senderAddress, name);
                                    List<Token> tokenList = tinyDB.getTokenList();
                                    tokenList.add(token);
                                    tinyDB.putTokenList(tokenList);
                                }else{
                                    Contract contract = new Contract(ContractBuilder.generateContractAddress(sendRawTransactionResponse.getTxid()), mContractTemplateUiid, false, null, senderAddress, name);
                                    List<Contract> contractList = tinyDB.getContractListWithoutToken();
                                    contractList.add(contract);
                                    tinyDB.putContractListWithoutToken(contractList);
                                }
                            }
                        }

                    }
                });
    }

    @Override
    public ContractConfirmView getView() {
        return view;
    }
}
