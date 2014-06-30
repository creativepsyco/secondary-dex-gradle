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

package com.github.creativepsyco.secondarydex.bigmodule.lib;

import android.util.Log;

/**
 * This must be an Implementation of an Interface which is shared
 * This is to prevent Runtime penalty on Reflection to access the methods
 *
 * @author: msk
 */
public class MyLoader implements Runnable {

    @Override
    public void run() {
        /**
         * This code is packaged in a separate DEX module and is loaded at run time
         */
        Log.d("MyLoader", "This is running from the Library");
    }
}
