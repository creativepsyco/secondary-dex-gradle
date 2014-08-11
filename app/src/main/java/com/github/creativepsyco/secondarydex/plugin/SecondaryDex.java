/*
 * Copyright (c) 2014 Mohit Kanwal.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 */
package com.github.creativepsyco.secondarydex.plugin;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import dalvik.system.PathClassLoader;

/**
 * DexLoader
 *
 * @author: msk
 */
// Copyright 2013 Garena.

/**
 * SecondaryDex
 *
 * @author: msk
 */
public class SecondaryDex {
    private static final String SECONDARY_DEX_NAME = "game.zip";
    private static final String LOG_TAG = "SecondaryDex";
    private static final String GAME_API_FQ_NAME = "com.beetalk.gameapi.GameAPI";
    private static final int BUF_SIZE = 1024;
    private static final int SDK_INT_ICS = 11;
    private static final int SDK_INT_KITKAT = 19;
    private static Context appContext;
    private static File dexInternalStoragePath;
    private static File optimizedDexOutputPath;
    private static ArrayList<String> theAppended = new ArrayList<String>();
    private boolean mIsLoaded;

    public static void loadSecondaryDex(Context context) {
        if (appContext == null) {
            appContext = context.getApplicationContext();
        }
        long time = System.currentTimeMillis();
        Log.d(LOG_TAG, "Start of the Loading " + time);
        // Copy to internal storage path
        dexInternalStoragePath = new File(appContext.getDir("dex", Context.MODE_PRIVATE), SECONDARY_DEX_NAME);
        assert dexInternalStoragePath != null;

        Log.d(LOG_TAG, "Internal Storage Path for Dex file: " + dexInternalStoragePath.getAbsolutePath());
        BufferedInputStream bis = null;
        OutputStream dexWriter = null;

        try {
            bis = new BufferedInputStream(appContext.getAssets().open(SECONDARY_DEX_NAME));
            assert bis != null;

            dexWriter = new BufferedOutputStream(
                new FileOutputStream(dexInternalStoragePath));
            byte[] buf = new byte[BUF_SIZE];
            int len;
            while ((len = bis.read(buf, 0, BUF_SIZE)) > 0) {
                dexWriter.write(buf, 0, len);
            }
            dexWriter.close();
            bis.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Internal storage where the DexClassLoader writes the optimized dex file to
        optimizedDexOutputPath = appContext.getDir("outdex", Context.MODE_PRIVATE);
//        PathClassLoader pathClassLoader = (PathClassLoader) Thread.currentThread().getContextClassLoader();
//        assert pathClassLoader != null;

//        DexClassLoader cl = new DexClassLoader(dexInternalStoragePath.getAbsolutePath(),
//                optimizedDexOutputPath.getAbsolutePath(),
//                null,
//                localClassLoader);
        String[] names = new String[]{"game.zip"};
        appendOdexesToClassPath(appContext, appContext.getDir("dex", Context.MODE_PRIVATE), names);
    }

    /**
     * Loads Dexes
     *
     * @param cxt    Application or Activity Context
     * @param dexDir The Dex Directory: Example: {@code /data/data/com.beetalk/app_dex/}
     * @param names
     * @return
     */
    private static boolean appendOdexesToClassPath(Context cxt, File dexDir, String[] names) {
        // non-existing ZIP in classpath causes an exception on ICS
        // so filter out the non-existent
        String strDexDir = dexDir.getAbsolutePath();
        ArrayList<String> zipPaths = new ArrayList<String>();
        for (int i = 0; i < names.length; i++) {
            String zipPath = strDexDir + '/' + names[i];
            File f = new File(zipPath);
            if (f.isFile()) {
                zipPaths.add(zipPath);
            }
        }

        String[] zipsOfDex = new String[zipPaths.size()];
        zipPaths.toArray(zipsOfDex);

        PathClassLoader pcl = (PathClassLoader) cxt.getClassLoader();
        // do something dangerous
        try {
            if (Build.VERSION.SDK_INT < SDK_INT_ICS) {
                FrameworkHack.appendDexListImplUnderICS(zipsOfDex, pcl, dexDir);
            } else { // ICS+
                boolean kitkatPlus = Build.VERSION.SDK_INT >= SDK_INT_KITKAT;
                ArrayList<File> jarFiles = strings2Files(zipsOfDex);
                FrameworkHack.appendDexListImplICS(jarFiles, pcl, dexDir, kitkatPlus);
            }
            // update theAppended if succeeded to prevent duplicated classpath entry
            for (String jarName : names) {
                theAppended.add(jarName);
            }
            Log.d(LOG_TAG, "appendOdexesToClassPath completed : " + pcl);
            Log.d(LOG_TAG, "theAppended : " + theAppended);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return true;
    }

    private static ArrayList<File> strings2Files(String[] paths) {
        ArrayList<File> result = new ArrayList<File>(paths.length);
        int size = paths.length;
        for (int i = 0; i < size; i++) {
            result.add(new File(paths[i]));
        }
        return result;
    }
}