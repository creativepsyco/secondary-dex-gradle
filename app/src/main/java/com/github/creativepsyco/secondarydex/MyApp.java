package com.github.creativepsyco.secondarydex;

import android.app.Application;

import com.github.creativepsyco.secondarydex.plugin.SecondaryDex;

/**
 * Created by msk on 15/06/2014.
 */
public class MyApp extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        SecondaryDex.loadSecondaryDex(this);
    }
}
