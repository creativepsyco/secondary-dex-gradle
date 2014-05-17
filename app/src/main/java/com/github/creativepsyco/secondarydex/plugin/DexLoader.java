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
import android.util.Log;

import java.io.*;

import dalvik.system.DexClassLoader;

/**
 * DexLoader
 *
 * @author: msk
 */
public class DexLoader {
    private static final String SECONDARY_DEX_NAME = "classes.jar";
    private static final String LOG_TAG = "DexLoader";
    static final int BUF_SIZE = 8 * 1024;
    private final File dexInternalStoragePath;
    private Context appContext;

    public DexLoader(Context context) {
        appContext = context.getApplicationContext();

        // Start of the Copy
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
        final File optimizedDexOutputPath = appContext.getDir("outdex", Context.MODE_PRIVATE);

        DexClassLoader cl = new DexClassLoader(dexInternalStoragePath.getAbsolutePath(),
                optimizedDexOutputPath.getAbsolutePath(),
                null,
                appContext.getClassLoader());
        Class libProviderClazz = null;
        try {
            // Load the library.
            libProviderClazz =
                    cl.loadClass("com.github.creativepsyco.secondarydex.bigmodule.lib.MyLoader");
            // Cast the return object to the library interface so that the
            // caller can directly invoke methods in the interface.
            // Alternatively, the caller can invoke methods through reflection,
            // which is more verbose.
            Runnable runnable = (Runnable) libProviderClazz.newInstance();
            runnable.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
        long endTime = System.currentTimeMillis();
        Log.d(LOG_TAG, "End of the Loading " + endTime);
        Log.d(LOG_TAG, "Time For loading " + (endTime - time));
    }

}
