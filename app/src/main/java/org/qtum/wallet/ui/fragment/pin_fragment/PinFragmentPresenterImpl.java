package org.qtum.wallet.ui.fragment.pin_fragment;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.util.Log;
import android.widget.Toast;
import org.qtum.wallet.R;

import org.qtum.wallet.utils.CryptoUtilsCompat;
import org.qtum.wallet.utils.crypto.AESUtil;

import org.qtum.wallet.ui.fragment.backup_wallet_fragment.BackUpWalletFragment;
import org.qtum.wallet.ui.base.base_fragment.BaseFragment;
import org.qtum.wallet.ui.base.base_fragment.BaseFragmentPresenterImpl;
import org.qtum.wallet.ui.fragment.send_fragment.SendFragment;
import org.qtum.wallet.ui.fragment.touch_id_preference_fragment.TouchIDPreferenceFragment;
import org.qtum.wallet.ui.fragment.wallet_main_fragment.WalletMainFragment;
import org.qtum.wallet.utils.CryptoUtils;
import org.qtum.wallet.utils.FingerprintUtils;

import javax.crypto.Cipher;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

class PinFragmentPresenterImpl extends BaseFragmentPresenterImpl implements PinFragmentPresenter {

    private static final String TAG =  PinFragmentPresenterImpl.class.getSimpleName();
    private PinFragmentView mPinFragmentView;
    private PinFragmentInteractorImpl mPinFragmentInteractor;
    private String pinRepeat;
    private String oldPin;
    private String pinHash;
    private String mAction;
    private Context mContext;

    private FingerprintHelper mFingerprintHelper;
    private FingerPrintHelperCompat23 mFingerPrintHelperCompat23;

    private String[] CREATING_STATE;
    private String[] AUTHENTICATION_STATE;
    private String[] CHANGING_STATE;

    private int currentState = 0;
    private boolean mTouchIdFlag;

    PinFragmentPresenterImpl(PinFragmentView pinFragmentView) {
        mPinFragmentView = pinFragmentView;
        mContext = getView().getContext();
        String ENTER_PIN = getView().getContext().getString(R.string.enter_pin_lower_case);
        String ENTER_NEW_PIN = getView().getContext().getString(R.string.enter_new_pin);
        String REPEAT_PIN = getView().getContext().getString(R.string.repeat_pin);
        String ENTER_OLD_PIN = getView().getContext().getString(R.string.enter_old_pin);
        CREATING_STATE = new String[]{ENTER_NEW_PIN, REPEAT_PIN};
        AUTHENTICATION_STATE = new String[]{ENTER_PIN};
        CHANGING_STATE = new String[]{ENTER_OLD_PIN, ENTER_NEW_PIN, REPEAT_PIN};

        mPinFragmentInteractor = new PinFragmentInteractorImpl(getView().getContext());
    }

    @Override
    public void confirm(final String pin) {
        switch (mAction) {
            case PinFragment.CREATING: {
                switch (currentState) {
                    case 0:
                        pinRepeat = pin;
                        currentState = 1;
                        getView().clearError();
                        updateState();
                        break;
                    case 1:
                        if (pin.equals(pinRepeat) ) {
                            getView().clearError();
                            getView().setProgressDialog();
                            getView().hideKeyBoard();
                            getInteractor().createWallet(getView().getContext(), new PinFragmentInteractorImpl.CreateWalletCallBack() {
                                @Override
                                public void onSuccess() {
                                    final BaseFragment fragment;

                                    byte[] saltPassphrase = AESUtil.encryptToBytes(pinRepeat,PinFragmentInteractorImpl.sPassphrase);
                                    getInteractor().saveSaltPassphrase(saltPassphrase);

                                    if(getView().getMainActivity().checkAvailabilityTouchId()) {
                                        fragment = TouchIDPreferenceFragment.newInstance(getView().getContext(), false, pinRepeat);
                                    } else {
                                        fragment = BackUpWalletFragment.newInstance(getView().getContext(), true, pinRepeat);
                                    }
                                    pinHash = CryptoUtilsCompat.generateSHA256String(pinRepeat);
                                    if(getView().getMainActivity().checkAvailabilityTouchId()){
                                        CryptoUtils.encodeInBackground(pinRepeat)
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe(new Subscriber<String>() {
                                                               @Override
                                                               public void onCompleted() {

                                                               }

                                                               @Override
                                                               public void onError(Throwable e) {
                                                                   Log.d(TAG, "OnError: "+ e.getMessage());
                                                               }

                                                               @Override
                                                               public void onNext(String s) {

                                                                   getInteractor().saveTouchIdPassword(s);
                                                                   getInteractor().savePassword(pinHash);
                                                                   getView().getMainActivity().onLogin();
                                                                   getView().openRootFragment(fragment);
                                                                   getView().dismissProgressDialog();
                                                                   PinFragmentInteractorImpl.isDataLoaded = false;
                                                               }
                                                           });
                                    } else {
                                        getInteractor().savePassword(pinHash);
                                        getView().getMainActivity().onLogin();
                                        getView().dismiss();
                                        getView().openFragmentWithBackStack(fragment, fragment.getClass().getName());
                                        getView().dismissProgressDialog();
                                        PinFragmentInteractorImpl.isDataLoaded = false;
                                    }
                                }

                                @Override
                                public void OnError(String message) {
                                    Log.d(TAG, "OnError: "+ message);
                                }
                            });
                        } else {
                            getView().confirmError(getView().getContext().getString(R.string.incorrect_repeated_pin));
                        }
                        break;
                }
            }
            break;

            case PinFragment.IMPORTING: {
                switch (currentState) {
                    case 0:
                        pinRepeat = pin;
                        currentState = 1;
                        getView().clearError();
                        updateState();
                        break;
                    case 1:
                        if (pin.equals(pinRepeat)) {
                            getView().clearError();
                            getView().setProgressDialog();

                            byte[] saltPassphrase = AESUtil.encryptToBytes(pinRepeat,getView().getPassphrase());
                            getInteractor().saveSaltPassphrase(saltPassphrase);

                            pinHash = CryptoUtilsCompat.generateSHA256String(pinRepeat);
                            final Fragment fragment;
                            if (getView().getMainActivity().checkAvailabilityTouchId()) {
                                fragment = TouchIDPreferenceFragment.newInstance(getView().getContext(), true,pinRepeat);
                            } else {
                                fragment = WalletMainFragment.newInstance(getView().getContext());
                                getView().getMainActivity().setRootFragment(fragment);
                            }
                            if(getView().getMainActivity().checkAvailabilityTouchId()){
                                CryptoUtils.encodeInBackground(pinRepeat)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new Subscriber<String>() {
                                            @Override
                                            public void onCompleted() {

                                            }

                                            @Override
                                            public void onError(Throwable e) {

                                            }

                                            @Override
                                            public void onNext(String s) {
                                                getInteractor().savePassword(pinHash);
                                                getInteractor().saveTouchIdPassword(s);
                                                getInteractor().setKeyGeneratedInstance(true);
                                                getView().dismissProgressDialog();
                                                getView().getMainActivity().onLogin();;
                                                getView().openRootFragment(fragment);
                                            }
                                        });
                            } else {
                                getInteractor().savePassword(pinHash);
                                getInteractor().setKeyGeneratedInstance(true);
                                getView().getMainActivity().onLogin();;
                                getView().dismissProgressDialog();
                                getView().openRootFragment(fragment);
                            }
                        } else {
                            getView().confirmError(getView().getContext().getString(R.string.incorrect_repeated_pin));
                        }
                        break;
                }
            }
            break;

            case PinFragment.AUTHENTICATION: {
                try {
                    String pinHashEntered = CryptoUtilsCompat.generateSHA256String(pin);
                    String pinHashGenuine = getInteractor().getPassword();
                    if (pinHashEntered.equals(pinHashGenuine)) {
                        getView().clearError();
                        final WalletMainFragment walletFragment = WalletMainFragment.newInstance(getView().getContext());
                        getView().setProgressDialog();
                        getView().hideKeyBoard();
                        getInteractor().loadWalletFromFile(new PinFragmentInteractorImpl.LoadWalletFromFileCallBack() {
                            @Override
                            public void onSuccess() {
                                getView().getMainActivity().setRootFragment(walletFragment);
                                getView().getMainActivity().onLogin();
                                ;
                                getView().openRootFragment(walletFragment);
                                getView().dismissProgressDialog();
                                PinFragmentInteractorImpl.isDataLoaded = false;
                            }

                            @Override
                            public void OnError(String message) {
                                Log.d(TAG, "OnError: "+ message);
                            }
                        });
                    } else {
                        getView().confirmError(getView().getContext().getString(R.string.incorrect_pin));
                    }
                }catch (Exception e){
                    Log.d(TAG, e.getMessage());
                }
            }
            break;

            case PinFragment.CHECK_AUTHENTICATION: {
                String pinHashEntered = CryptoUtilsCompat.generateSHA256String(pin);
                String pinHashGenuine = getInteractor().getPassword();
                if (pinHashEntered.equals(pinHashGenuine)) {
                    getView().clearError();
                    getView().hideKeyBoard();
                    getView().getMainActivity().setCheckAuthenticationShowFlag(false);
                    getView().getMainActivity().onBackPressed();
                } else {
                    getView().confirmError(getView().getContext().getString(R.string.incorrect_pin));
                }
            }
            break;

            case PinFragment.AUTHENTICATION_FOR_PASSPHRASE: {
                String pinHashEntered = CryptoUtilsCompat.generateSHA256String(pin);
                String pinHashGenuine = getInteractor().getPassword();
                if (pinHashEntered.equals(pinHashGenuine)) {
                    getView().clearError();
                    getView().hideKeyBoard();
                    getView().getMainActivity().setCheckAuthenticationShowFlag(false);
                    Fragment backUpWalletFragment = BackUpWalletFragment.newInstance(getView().getContext(), false, pin);
                    getView().getMainActivity().onBackPressed();
                    getView().openFragment(backUpWalletFragment);
                } else {
                    getView().confirmError(getView().getContext().getString(R.string.incorrect_pin));
                }
            }
            break;

            case PinFragment.AUTHENTICATION_AND_SEND: {
                String pinHashEntered = CryptoUtilsCompat.generateSHA256String(pin);
                String pinHashGenuine = getInteractor().getPassword();
                if (pinHashEntered.equals(pinHashGenuine)) {
                    getView().clearError();
                    String address = getView().getMainActivity().getAddressForSendAction();
                    String amount = getView().getMainActivity().getAmountForSendAction();
                    String token = getView().getMainActivity().getTokenForSendAction();

                    final BaseFragment sendFragment = SendFragment.newInstance(false, address, amount, token, getView().getContext());

                    getView().setProgressDialog();
                    getView().hideKeyBoard();
                    getInteractor().loadWalletFromFile(new PinFragmentInteractorImpl.LoadWalletFromFileCallBack() {
                        @Override
                        public void onSuccess() {

                            getView().getMainActivity().setRootFragment(sendFragment);
                            getView().getMainActivity().onLogin();
                            getView().openRootFragment(sendFragment);

                            getView().dismissProgressDialog();
                            PinFragmentInteractorImpl.isDataLoaded = false;
                        }

                        @Override
                        public void OnError(String message) {
                            Log.d(TAG, "OnError: "+ message);
                        }
                    });
                } else {
                    getView().confirmError(getView().getContext().getString(R.string.incorrect_pin));
                }
            }
            break;

            case PinFragment.CHANGING: {
                switch (currentState) {
                    case 0:
                        oldPin = pin;
                        String pinHashEntered = CryptoUtilsCompat.generateSHA256String(pin);
                        String pinHashGenuine = getInteractor().getPassword();
                        if (pinHashEntered.equals(pinHashGenuine)) {
                            currentState = 1;
                            getView().clearError();
                            updateState();
                        } else {
                            getView().confirmError(getView().getContext().getString(R.string.incorrect_pin));
                        }
                        break;
                    case 1:
                        pinRepeat = pin;
                        currentState = 2;
                        getView().clearError();
                        updateState();
                        break;
                    case 2:
                        if (pin.equals(pinRepeat)) {
                            getView().clearError();

                            final String pinHash = CryptoUtilsCompat.generateSHA256String(pinRepeat);
                            getInteractor().savePassword(pinHash);
                            byte[] oldSaltPassphrase = getInteractor().getSaltPassphrase();
                            String passphrase = AESUtil.decryptBytes(oldPin, oldSaltPassphrase);
                            byte[] saltPassphrase = AESUtil.encryptToBytes(pinRepeat,passphrase);
                            getInteractor().saveSaltPassphrase(saltPassphrase);

                            if(getView().getMainActivity().checkAvailabilityTouchId()){
                                CryptoUtils.encodeInBackground(pinRepeat)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new Subscriber<String>() {
                                            @Override
                                            public void onCompleted() {

                                            }

                                            @Override
                                            public void onError(Throwable e) {

                                            }

                                            @Override
                                            public void onNext(String s) {
                                                getInteractor().saveTouchIdPassword(s);
                                                getView().getMainActivity().onBackPressed();
                                            }
                                        });
                            } else {
                                getView().getMainActivity().onBackPressed();
                            }
                        } else {
                            getView().confirmError(getView().getContext().getString(R.string.incorrect_repeated_pin));
                        }
                        break;
                }
            }
            break;
        }
    }

    @Override
    public void cancel() {
        switch (mAction) {

            case PinFragment.AUTHENTICATION:
            case PinFragment.AUTHENTICATION_AND_SEND:
            case PinFragment.CHECK_AUTHENTICATION:
            case PinFragment.AUTHENTICATION_FOR_PASSPHRASE:{
                getView().finish();
                break;
            }
            case PinFragment.CREATING:
            case PinFragment.IMPORTING:
            case PinFragment.CHANGING: {
                getView().getMainActivity().onBackPressed();
                break;
            }
        }
    }

    @Override
    public void setAction(String action) {
        mAction = action;
    }

    @Override
    public void initializeViews() {
        super.initializeViews();
        mTouchIdFlag = getView().getMainActivity().checkTouchIdEnable();
        int titleID = 0;
        switch (mAction) {
            case PinFragment.IMPORTING:
            case PinFragment.CREATING:
                titleID = R.string.create_pin;
                break;
            case PinFragment.AUTHENTICATION_AND_SEND:
            case PinFragment.AUTHENTICATION:
            case PinFragment.CHECK_AUTHENTICATION:
            case PinFragment.AUTHENTICATION_FOR_PASSPHRASE:
                if(mTouchIdFlag && FingerprintUtils.isSensorStateAt(FingerprintUtils.mSensorState.READY, mContext)){
                    titleID = R.string.confirm_fingerprint_or_pin;
                } else {
                    titleID = R.string.enter_pin;
                }
                break;
            case PinFragment.CHANGING:
                titleID = R.string.change_pin;
                break;
        }
        getView().setToolBarTitle(titleID);
    }

    @Override
    public void onPause(Context context) {
        super.onPause(context);
        pinRepeat = "0";
        currentState = 0;
    }

    @Override
    public void onResume(Context context) {
        super.onResume(context);
        updateState();
        if (PinFragmentInteractorImpl.isDataLoaded) {
            switch (mAction) {
                case PinFragment.CREATING: {
                    BaseFragment backUpWalletFragment = BackUpWalletFragment.newInstance(getView().getContext(), true, pinRepeat);
                    getInteractor().savePassword(pinHash);
                    getView().openRootFragment(backUpWalletFragment);
                    getView().dismissProgressDialog();
                    break;
                }
                case PinFragment.AUTHENTICATION: {
                    WalletMainFragment walletFragment = WalletMainFragment.newInstance(getView().getContext());
                    getView().getMainActivity().setRootFragment(walletFragment);
                    getView().openRootFragment(walletFragment);
                    getView().dismissProgressDialog();
                    break;
                }
                case PinFragment.AUTHENTICATION_AND_SEND: {
                    String address = getView().getMainActivity().getAddressForSendAction();
                    String amount = getView().getMainActivity().getAmountForSendAction();
                    String token = getView().getMainActivity().getTokenForSendAction();

                    final BaseFragment sendFragment = SendFragment.newInstance(false, address, amount,token, getView().getContext());
                    getView().getMainActivity().setRootFragment(sendFragment);
                    getView().openRootFragment(sendFragment);
                    getView().dismissProgressDialog();
                    break;
                }
            }
            PinFragmentInteractorImpl.isDataLoaded = false;
        }

        if(mTouchIdFlag && (mAction.equals(PinFragment.AUTHENTICATION_AND_SEND) || mAction.equals(PinFragment.AUTHENTICATION) || mAction.equals(PinFragment.CHECK_AUTHENTICATION)) || mAction.equals(PinFragment.AUTHENTICATION_FOR_PASSPHRASE)){
            prepareSensor();
        }

    }

    private void prepareSensor() {
        if (FingerprintUtils.isSensorStateAt(FingerprintUtils.mSensorState.READY, mContext)) {
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
                FingerprintManagerCompat.CryptoObject cryptoObject = CryptoUtils.getCryptoObject();
                if (cryptoObject != null) {
                    mFingerprintHelper = new FingerprintHelper(mContext);
                    mFingerprintHelper.startAuth(cryptoObject);
                } else {
                    //TODO: make
                    Toast.makeText(mContext, "new fingerprint enrolled. enter pin again", Toast.LENGTH_SHORT).show();
                }
            } else {
                FingerprintManager.CryptoObject cryptoObject = CryptoUtils.getCryptoObjectCompat23();
                if (cryptoObject != null) {
                    mFingerPrintHelperCompat23 = new FingerPrintHelperCompat23(mContext);
                    mFingerPrintHelperCompat23.startAuth(cryptoObject);
                } else {
                    //TODO: make
                    Toast.makeText(mContext, "new fingerprint enrolled. enter pin again", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onStop(Context context) {
        super.onStop(context);
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (mFingerprintHelper != null) {
                mFingerprintHelper.cancel();
            }
        } else {
            if (mFingerPrintHelperCompat23 != null) {
                mFingerPrintHelperCompat23.cancel();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public class FingerPrintHelperCompat23 extends FingerprintManager.AuthenticationCallback{

        private Context mContext;
        private android.os.CancellationSignal mCancellationSignal;

        FingerPrintHelperCompat23(Context context) {
            mContext = context;
        }

        void cancel() {
            if (mCancellationSignal != null) {
                mCancellationSignal.cancel();
            }
        }

        void startAuth(FingerprintManager.CryptoObject cryptoObject) {
            mCancellationSignal = new android.os.CancellationSignal();
            FingerprintManager manager = (FingerprintManager) mContext.getSystemService(Context.FINGERPRINT_SERVICE);
            manager.authenticate(cryptoObject, mCancellationSignal, 0,  this, null);
        }

        @Override
        public void onAuthenticationError(int errorCode, CharSequence errString) {
            super.onAuthenticationError(errorCode, errString);
            getView().confirmError(errString.toString());
        }

        @Override
        public void onAuthenticationFailed() {
            super.onAuthenticationFailed();
            getView().confirmError("try again");
        }

        @Override
        public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
            super.onAuthenticationHelp(helpCode, helpString);
            getView().confirmError(helpString.toString());
        }

        @Override
        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);
            Cipher cipher = result.getCryptoObject().getCipher();
            String encoded = getInteractor().getTouchIdPassword();
            String decoded = CryptoUtils.decode(encoded, cipher);
            getView().setPin(decoded);
        }
    }

    public class FingerprintHelper extends FingerprintManagerCompat.AuthenticationCallback {
        private Context mContext;
        private CancellationSignal mCancellationSignal;

        FingerprintHelper(Context context) {
            mContext = context;
        }

        void startAuth(FingerprintManagerCompat.CryptoObject cryptoObject) {
            mCancellationSignal = new CancellationSignal();
            FingerprintManagerCompat manager = FingerprintManagerCompat.from(mContext);
            manager.authenticate(cryptoObject, 0, mCancellationSignal, this, null);
        }

        void cancel() {
            if (mCancellationSignal != null) {
                mCancellationSignal.cancel();
            }
        }

        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString) {
            getView().confirmError(errString.toString());
        }

        @Override
        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
            getView().confirmError(helpString.toString());
        }

        @Override
        public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
            Cipher cipher = result.getCryptoObject().getCipher();
            String encoded = getInteractor().getTouchIdPassword();
            String decoded = CryptoUtils.decode(encoded, cipher);
            getView().setPin(decoded);
        }

        @Override
        public void onAuthenticationFailed() {
            getView().confirmError("try again");
        }
    }

    @Override
    public PinFragmentView getView() {
        return mPinFragmentView;
    }

    private PinFragmentInteractorImpl getInteractor() {
        return mPinFragmentInteractor;
    }

    private void updateState() {
        String state = null;
        switch (mAction) {
            case PinFragment.IMPORTING:
            case PinFragment.CREATING:
                state = CREATING_STATE[currentState];
                break;
            case PinFragment.AUTHENTICATION:
            case PinFragment.AUTHENTICATION_AND_SEND:
            case PinFragment.CHECK_AUTHENTICATION:
            case PinFragment.AUTHENTICATION_FOR_PASSPHRASE:
                state = AUTHENTICATION_STATE[currentState];
                break;
            case PinFragment.CHANGING:
                state = CHANGING_STATE[currentState];
                break;
        }
        getView().updateState(state);
    }

}