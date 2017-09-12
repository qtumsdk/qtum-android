package org.qtum.wallet.ui.fragment.currency_fragment;


import org.qtum.wallet.model.Currency;
import org.qtum.wallet.ui.base.base_fragment.BaseFragmentView;

import java.util.List;

interface CurrencyFragmentView extends BaseFragmentView {
    void setCurrencyList(List<Currency> currencyList);
}
