/*
 * Copyright (c) 2015 CoinAge-DAO
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.coinprism.model;

import com.coinprism.utils.SecurePreferences;
import com.coinprism.wallet.R;
import com.coinprism.wallet.fragment.BalanceTab;
import com.coinprism.wallet.fragment.SendTab;
import com.coinprism.wallet.fragment.TransactionsTab;

import org.solarij.core.NetworkParameters;
import org.solarij.crypto.MnemonicCode;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Contains the global state of the application.
 */
public class WalletState
{
    public final static String seedKey = "wallet.seed";
    private static WalletState state;

    private final WalletConfiguration configuration;
    private final APIClient api;
    private Boolean firstLaunch;
    private AddressBalance walletData;
    private BalanceTab balanceTab;
    private SendTab sendTab;
    private TransactionsTab transactionsTab;

    public WalletState(WalletConfiguration configuration, APIClient api, Boolean firstLaunch)
    {
        this.configuration = configuration;
        this.api = api;
        this.firstLaunch = firstLaunch;

        Timer updateTimer = new Timer();
        TimerTask task = new TimerTask()
        {
            @Override
            public void run()
            {
                WalletState.this.triggerUpdate();
            }
        };

        updateTimer.schedule(task, 0, 60000);
    }

    /**
     * Gets the singleton instance.
     *
     * @return the singleton instance
     */
    public static WalletState getState()
    {
        if (state == null)
            state = initialize();

        return state;
    }

    private static WalletState initialize()
    {
        SecurePreferences preferences = new SecurePreferences(CoinprismWalletApplication.getContext());

        String seed = preferences.getString(seedKey, null);

        Boolean firstLaunch = false;
        WalletConfiguration wallet;
        if (seed == null)
        {
            seed = WalletConfiguration.createWallet();

            SecurePreferences.Editor editor = preferences.edit();
            editor.putString(seedKey, seed);
            editor.commit();

            firstLaunch = true;
        }

        wallet = new WalletConfiguration(
            seed, NetworkParameters.fromID(CoinprismWalletApplication.getContext().getString(R.string.network)));

        try
        {
            MnemonicCode.INSTANCE = new MnemonicCode(CoinprismWalletApplication.getContext().getAssets()
                .open("bip39-wordlist.txt"), null);
        }
        catch (IOException exception)
        { }

        return new WalletState(
            wallet,
            new APIClient(
                CoinprismWalletApplication.getContext().getString(R.string.api_base_url),
                wallet.getNetworkParameters()),
            firstLaunch);
    }

    public void triggerUpdate()
    {
        BalanceLoader loader = new BalanceLoader(this);
        loader.execute(configuration.getAddress());
    }

    public void updateData(AddressBalance data)
    {
        if (data != null)
            this.walletData = data;

        this.balanceTab.updateWallet();
        this.sendTab.updateWallet();
    }

    public AddressBalance getBalance()
    {
        return this.walletData;
    }

    public WalletConfiguration getConfiguration()
    {
        return this.configuration;
    }

    /**
     * Gets the API client object.
     * @return the API client object
     */
    public APIClient getAPIClient()
    {
        return this.api;
    }

    public BalanceTab getBalanceTab()
    {
        return balanceTab;
    }

    public void setBalanceTab(BalanceTab balanceTab)
    {
        this.balanceTab = balanceTab;
    }

    public SendTab getSendTab()
    {
        return sendTab;
    }

    public void setSendTab(SendTab sendTab)
    {
        this.sendTab = sendTab;
    }

    public TransactionsTab getTransactionsTab()
    {
        return transactionsTab;
    }

    public void setTransactionsTab(TransactionsTab transactionsTab)
    {
        this.transactionsTab = transactionsTab;
    }

    /**
     * Gets whether this is the first time that the application is launched.
     *
     * @return a boolean indicating whether this is the first time that the application is launched
     */
    public Boolean getFirstLaunch()
    {
        return firstLaunch;
    }

    public void unsetFirstLaunch()
    {
        this.firstLaunch = false;
    }
}
