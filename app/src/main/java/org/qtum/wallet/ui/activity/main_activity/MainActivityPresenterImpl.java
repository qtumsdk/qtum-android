package org.qtum.wallet.ui.activity.main_activity;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.nfc.NfcAdapter;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.MenuItem;

import org.qtum.wallet.R;
import org.qtum.wallet.dataprovider.receivers.network_state_receiver.NetworkStateReceiver;
import org.qtum.wallet.dataprovider.services.update_service.UpdateService;
import org.qtum.wallet.datastorage.listeners.LanguageChangeListener;
import org.qtum.wallet.datastorage.QtumSharedPreference;
import org.qtum.wallet.ui.base.base_activity.BasePresenterImpl;
import org.qtum.wallet.ui.base.base_fragment.BaseFragment;
import org.qtum.wallet.ui.fragment.news_fragment.NewsFragment;
import org.qtum.wallet.ui.fragment.pin_fragment.PinFragment;
import org.qtum.wallet.ui.fragment.profile_fragment.ProfileFragment;
import org.qtum.wallet.ui.fragment.send_fragment.SendFragment;
import org.qtum.wallet.ui.fragment.start_page_fragment.StartPageFragment;
import org.qtum.wallet.ui.fragment.wallet_main_fragment.WalletMainFragment;
import org.qtum.wallet.utils.QtumIntent;


import java.util.ArrayList;
import java.util.List;


class MainActivityPresenterImpl extends BasePresenterImpl implements MainActivityPresenter {

    private MainActivityView mMainActivityView;
    private MainActivityInteractorImpl mMainActivityInteractor;
    protected Fragment mRootFragment;
    private Context mContext;
    public boolean mAuthenticationFlag = false;
    private boolean mCheckAuthenticationFlag = false;
    private boolean mCheckAuthenticationShowFlag = false;
    private boolean mSendFromIntent = false;

    private Intent mIntent;
    private UpdateService mUpdateService = null;

    private NetworkStateReceiver mNetworkReceiver;

    private String mAddressForSendAction;
    private String mAmountForSendAction;
    private String mTokenAddressForSendAction;

    private LanguageChangeListener mLanguageChangeListener;
    private List<MainActivity.OnServiceConnectionChangeListener> mServiceConnectionChangeListeners = new ArrayList<>();

    MainActivityPresenterImpl(MainActivityView mainActivityView) {
        mMainActivityView = mainActivityView;
        mContext = getView().getContext();
        mMainActivityInteractor = new MainActivityInteractorImpl(mContext);
    }

    @Override
    public void onCreate(Context context) {
        super.onCreate(context);

        mNetworkReceiver = new NetworkStateReceiver(getView().getNetworkConnectedFlag());
        mContext.registerReceiver(mNetworkReceiver,
                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        mLanguageChangeListener = new LanguageChangeListener() {
            @Override
            public void onLanguageChange() {
                getView().resetMenuText();
            }
        };

        QtumSharedPreference.getInstance().addLanguageListener(mLanguageChangeListener);

    }

    @Override
    public void onStop(Context context) {
        super.onStop(context);
        if(mAuthenticationFlag && !mCheckAuthenticationShowFlag){
            mCheckAuthenticationFlag = true;
        }
    }

    public void resetAuthFlags(){
        mCheckAuthenticationFlag = true;
        mCheckAuthenticationShowFlag = false;
    }

    @Override
    public void onResume(Context context) {
        super.onResume(context);

    }

    @Override
    public void onPostResume(Context context) {
        super.onPostResume(context);
        if(mAuthenticationFlag && mCheckAuthenticationFlag && !mCheckAuthenticationShowFlag){
            BaseFragment pinFragment = PinFragment.newInstance(PinFragment.CHECK_AUTHENTICATION, getView().getContext());
            getView().openFragment(pinFragment);
            mCheckAuthenticationFlag = false;
            mCheckAuthenticationShowFlag = true;
        }
    }

    public boolean isCheckAuthenticationShowFlag() {
        return mCheckAuthenticationShowFlag;
    }

    public void setCheckAuthenticationShowFlag(boolean checkAuthenticationShowFlag) {
        mCheckAuthenticationShowFlag = checkAuthenticationShowFlag;
    }

    @Override
    public MainActivityView getView() {
        return mMainActivityView;
    }

    private MainActivityInteractorImpl getInteractor() {
        return mMainActivityInteractor;
    }

    @Override
    public void onLogin() {
        mAuthenticationFlag = true;
        mIntent = new Intent(mContext, UpdateService.class);
        if (!isMyServiceRunning(UpdateService.class)) {
            mContext.startService(mIntent);
            if(mUpdateService!=null){
                mUpdateService.startMonitoring();
            } else {
                mContext.bindService(mIntent, mServiceConnection, 0);
            }
        } else {
            if(mUpdateService!=null){
                mUpdateService.startMonitoring();
            }else {
                mContext.bindService(mIntent, mServiceConnection, 0);
            }
        }
    }

    public void stopUpdateService(){
        if(mUpdateService != null) {
            mUpdateService.stopMonitoring();
            getView().getActivity().stopService(mIntent);
        }
    }

    @Override
    public void subscribeOnServiceConnectionChangeEvent(MainActivity.OnServiceConnectionChangeListener listener) {
        mServiceConnectionChangeListeners.add(listener);
        listener.onServiceConnectionChange(mUpdateService!=null);
    }

    @Override
    public void onLogout() {
        mAuthenticationFlag = false;
        if(mUpdateService!=null){
            mUpdateService.stopMonitoring();
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mUpdateService = ((UpdateService.UpdateBinder) iBinder).getService();
            mUpdateService.clearNotification();
            mUpdateService.startMonitoring();
            for(MainActivity.OnServiceConnectionChangeListener listener : mServiceConnectionChangeListeners) {
                listener.onServiceConnectionChange(true);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    public UpdateService getUpdateService(){
        return mUpdateService;
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onPostCreate(Context context) {
        super.onPostCreate(context);
        openStartFragment();
    }

    private void openStartFragment() {
        Fragment fragment;
        if (getInteractor().getKeyGeneratedInstance(mContext)) {
            if(mSendFromIntent){
                fragment = PinFragment.newInstance(PinFragment.AUTHENTICATION_AND_SEND, getView().getContext());
                getView().openRootFragment(fragment);
            } else if(!mAuthenticationFlag){
                fragment = StartPageFragment.newInstance(true,getView().getContext());
                getView().openRootFragment(fragment);
            }
        } else {
            fragment = StartPageFragment.newInstance(false,getView().getContext());
            getView().openRootFragment(fragment);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_wallet:
                if (mRootFragment != null && mRootFragment.getClass().getSimpleName().contains(WalletMainFragment.class.getSimpleName())) {
                    getView().popBackStack();
                    return true;
                }
                mRootFragment = WalletMainFragment.newInstance(getView().getContext());
                break;
            case R.id.item_profile:
                if (mRootFragment != null && mRootFragment.getClass().getSimpleName().contains(ProfileFragment.class.getSimpleName())) {
                    getView().popBackStack();
                    return true;
                }
                mRootFragment = ProfileFragment.newInstance(getView().getContext());
                break;
            case R.id.item_news:
                if (mRootFragment != null && mRootFragment.getClass().getSimpleName().contains(NewsFragment.class.getSimpleName())) {
                    getView().popBackStack();
                    return true;
                }
                mRootFragment = NewsFragment.newInstance(getView().getContext());
                break;
            case R.id.item_send:
                if (mRootFragment != null && mRootFragment.getClass().getSimpleName().contains(SendFragment.class.getSimpleName())) {
                    getView().popBackStack();
                    return true;
                }

                mRootFragment = SendFragment.newInstance(false, null, null, null, getView().getContext());

                break;
            default:
                return false;
        }
        getView().openRootFragment(mRootFragment);
        return true;
    }

    @Override
    public void setRootFragment(Fragment fragment) {
        mRootFragment = fragment;
    }

    @Override
    public void processIntent(Intent intent) {
        switch (intent.getAction()){
            case QtumIntent.SEND_FROM_SDK:
                mSendFromIntent = true;
                mAddressForSendAction = intent.getStringExtra(QtumIntent.SEND_ADDRESS);
                mAmountForSendAction = intent.getStringExtra(QtumIntent.SEND_AMOUNT);
                mTokenAddressForSendAction = intent.getStringExtra(QtumIntent.SEND_TOKEN);
                break;
            case NfcAdapter.ACTION_NDEF_DISCOVERED:
                mSendFromIntent = true;
                mAddressForSendAction = "QbShaLBf1nAX3kznmGU7vM85HFRYJVG6ut";
                mAmountForSendAction = "1.431";
                break;
            default:
                break;
        }
    }

    @Override
    public void processNewIntent(Intent intent) {
        switch (intent.getAction()) {

            case QtumIntent.OPEN_FROM_NOTIFICATION:
                mRootFragment = WalletMainFragment.newInstance(getView().getContext());
                getView().openRootFragment(mRootFragment);
                getView().setIconChecked(0);
                break;
            case QtumIntent.SEND_FROM_SDK:
                mAddressForSendAction = intent.getStringExtra(QtumIntent.SEND_ADDRESS);
                mAmountForSendAction = intent.getStringExtra(QtumIntent.SEND_AMOUNT);
                mTokenAddressForSendAction = intent.getStringExtra(QtumIntent.SEND_TOKEN);
                if(mAuthenticationFlag){
                    mRootFragment = SendFragment.newInstance(false,mAddressForSendAction,mAmountForSendAction, mTokenAddressForSendAction, getView().getContext());
                    getView().openRootFragment(mRootFragment);
                    getView().setIconChecked(3);
                } else {
                    Fragment fragment = PinFragment.newInstance(PinFragment.AUTHENTICATION_AND_SEND, getView().getContext());
                    getView().openRootFragment(fragment);
                }
                break;

            case NfcAdapter.ACTION_NDEF_DISCOVERED:
                mAddressForSendAction = "QbShaLBf1nAX3kznmGU7vM85HFRYJVG6ut";
                mAmountForSendAction = "0.253";
                if(mAuthenticationFlag) {
                    mRootFragment = SendFragment.newInstance(false,mAddressForSendAction,mAmountForSendAction, mTokenAddressForSendAction, getView().getContext());
                    getView().setIconChecked(3);
                } else{
                    mRootFragment = PinFragment.newInstance(PinFragment.AUTHENTICATION_AND_SEND, getView().getContext());

                }
                getView().openRootFragment(mRootFragment);
                break;
            default:
                break;
        }

    }

    @Override
    public NetworkStateReceiver getNetworkReceiver() {
        return mNetworkReceiver;
    }

    @Override
    public void onDestroy(Context context) {
        super.onDestroy(context);
        getInteractor().clearStatic();
        clearservice();
    }

    public void clearservice(){
        if(mUpdateService != null) {
            mContext.unbindService(mServiceConnection);
            mContext.unregisterReceiver(mNetworkReceiver);
        }
    }

    public String getAddressForSendAction() {
        return mAddressForSendAction;
    }

    public String getAmountForSendAction() {
        return mAmountForSendAction;
    }

    public String getTokenForSendAction() {
        return mTokenAddressForSendAction;
    }
}