package com.github.creativepsyco.secondarydex.plugin;

/*
 * Copyright 2013 ThinkFree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.util.Log;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.ZipFile;

import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;

/**
 * This framework hack allows you to inject class paths loaded at runtime.
 */
public class FrameworkHack {
    private static String LOG_TAG = "FrameworkHack";

    private FrameworkHack() {
        // do not create an instance
    }

    /**
     * dalvik do not have security manager
     */
    private static void forceSet(Object obj, Field f, Object val) throws IllegalAccessException {
        f.setAccessible(true);
        f.set(obj, val);
    }

    private static Object forceGetFirst(Object obj, Field fArray) throws IllegalAccessException {
        fArray.setAccessible(true);
        Object[] vArray = (Object[]) fArray.get(obj);
        return vArray[0];
    }

    private static String joinPaths(String[] paths) {
        if (paths == null) {
            return "";
        }
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < paths.length; i++) {
            buf.append(paths[i]);
            buf.append(':');
        }
        return buf.toString();
    }

    // https://android.googlesource.com/platform/dalvik/+/android-1.6_r1/libcore/dalvik/src/main/java/dalvik/system/PathClassLoader.java
    public static void appendDexListImplUnderICS(String[] zipPathsToAppend, PathClassLoader pcl, File optDir)
        throws Exception {
        int oldSize = 1; // gonna assume the original path had single entry for simplicity
        Class pclClass = pcl.getClass();
        Field fPath = pclClass.getDeclaredField("path");
        fPath.setAccessible(true);
        String orgPath = fPath.get(pcl).toString();
        String pathToAdd = joinPaths(zipPathsToAppend);
        String path = orgPath + ':' + pathToAdd;
        forceSet(pcl, fPath, path);

        boolean wantDex = System.getProperty("android.vm.dexfile", "").equals("true");
        File[] files = new File[oldSize + zipPathsToAppend.length];
        ZipFile[] zips = new ZipFile[oldSize + zipPathsToAppend.length];
        DexFile[] dexs = new DexFile[oldSize + zipPathsToAppend.length];

        Field fmPaths = pclClass.getDeclaredField("mPaths");
        String[] newMPaths = new String[oldSize + zipPathsToAppend.length];
        // set originals
        newMPaths[0] = (String) forceGetFirst(pcl, fmPaths);
        forceSet(pcl, fmPaths, newMPaths);
        Field fmFiles = pclClass.getDeclaredField("mFiles");
        files[0] = (File) forceGetFirst(pcl, fmFiles);
        Field fmZips = pclClass.getDeclaredField("mZips");
        zips[0] = (ZipFile) forceGetFirst(pcl, fmZips);
        Field fmDexs = pclClass.getDeclaredField("mDexs");
        dexs[0] = (DexFile) forceGetFirst(pcl, fmDexs);

        for (int i = 0; i < zipPathsToAppend.length; i++) {
            newMPaths[oldSize + i] = zipPathsToAppend[i];
            File pathFile = new File(zipPathsToAppend[i]);
            files[oldSize + i] = pathFile;
            zips[oldSize + i] = new ZipFile(pathFile);
            if (wantDex) {
                String outDexName = pathFile.getName() + ".dex";
                File outFile = new File(optDir, outDexName);
                dexs[oldSize + i] = DexFile.loadDex(pathFile.getAbsolutePath(), outFile.getAbsolutePath(), 0);
            }
        }
        forceSet(pcl, fmFiles, files);
        forceSet(pcl, fmZips, zips);
        forceSet(pcl, fmDexs, dexs);
    }

    // https://android.googlesource.com/platform/libcore/+/master/libdvm/src/main/java/dalvik/system/BaseDexClassLoader.java
    // https://android.googlesource.com/platform/libcore/+/master/dalvik/src/main/java/dalvik/system/BaseDexClassLoader.java
    public static void appendDexListImplICS(ArrayList<File> jarFiles, PathClassLoader pcl, File optDir,
                                            boolean kitkatPlus) throws Exception {
        Log.d(LOG_TAG, "appendDexListImplICS(" + jarFiles);
        // to save original values
        Class bdclClass = Class.forName("dalvik.system.BaseDexClassLoader");
        // ICS+ - pathList
        Field fPathList = bdclClass.getDeclaredField("pathList");
        fPathList.setAccessible(true);
        Object dplObj = fPathList.get(pcl);
        // to call DexPathList.makeDexElements() for additional jar(apk)s
        Class dplClass = dplObj.getClass();
        Field fDexElements = dplClass.getDeclaredField("dexElements");
        fDexElements.setAccessible(true);
        Object objOrgDexElements = fDexElements.get(dplObj);
        int orgDexCount = Array.getLength(objOrgDexElements);
        Log.d(LOG_TAG, "orgDexCount : " + orgDexCount);
        debugDexElements(objOrgDexElements);
        Class clazzElement = Class.forName("dalvik.system.DexPathList$Element");
        // create new merged array
        int jarCount = jarFiles.size();
        Object newDexElemArray = Array.newInstance(clazzElement, orgDexCount + jarCount);
        System.arraycopy(objOrgDexElements, 0, newDexElemArray, 0, orgDexCount);
        Method mMakeDexElements = null;
        if (kitkatPlus) {
            mMakeDexElements =
                dplClass.getDeclaredMethod("makeDexElements", ArrayList.class, File.class, ArrayList.class);
        } else {
            mMakeDexElements = dplClass.getDeclaredMethod("makeDexElements", ArrayList.class, File.class);
        }
        mMakeDexElements.setAccessible(true);
        Object elemsToAdd;
        if (kitkatPlus) {
            elemsToAdd = mMakeDexElements.invoke(null, jarFiles, optDir, new ArrayList());
        } else {
            elemsToAdd = mMakeDexElements.invoke(null, jarFiles, optDir);
        }
        for (int i = 0; i < jarCount; i++) {
            int pos = orgDexCount + i;
            Object elemToAdd = Array.get(elemsToAdd, i);
            Array.set(newDexElemArray, pos, elemToAdd);
        }
        Log.d(LOG_TAG, "appendDexListImplICS() " + Arrays.deepToString((Object[]) newDexElemArray));
        forceSet(dplObj, fDexElements, newDexElemArray);
    }

    private static void debugDexElements(Object dexElements) throws Exception {
        Object[] objArray = (Object[]) dexElements;
        Class clazzElement = Class.forName("dalvik.system.DexPathList$Element");
        Field fFile = clazzElement.getDeclaredField("file");
        fFile.setAccessible(true);
        for (int i = 0; i < objArray.length; i++) {
            File f = (File) fFile.get(objArray[i]);
            Log.d(LOG_TAG, "[" + i + "] " + f);
        }
    }
}