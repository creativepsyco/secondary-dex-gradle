#!/bin/bash
#set -x
# Copyright 2014 Mohit Singh Kanwal.
#
# Authors:
#    -Mohit Kanwal <mohit.kanwal@gmail.com>
#
# ================================================================================================
# This script does the job of moving out the DEX classes from the rest of the application classes
# The normal build cycle is separated into debug & release (Each flavor has 2 types).
# For debug builds we simply dex the classes.jar from the relevant classes into a separate version
# Ideally the --multi-dex option in the future versions of the build tool should help
# But Gradle script has no way of accessing these, the Dex task is hard coded with a lot of
# internal dependencies. Anyways here are the crucial steps:
#
# Debug:
#    1. Adjust the variant.dex.libraries to prevent the interested classes.jar from getting into the
#       main dex.
#    2. Use these classes to create the Dex file comprising of the interested classes
#    3. Package the classes.dex into a Jar or Zip, anything else is not allowed
#       (the name should remain the same since API v10 has this hardcoded)
#    4. Copy this inside the Assets Folder to be put inside the app (It might be zipped already but
#       this script takes care of unzipping it, putting the classes.zip inside it & zipping it again)
#    5. Copy this zip from the Assets folder at runtime and load it into the internal Directory
#
# Release
#    1. Make sure to copy the classes after the proguard Task
#    2. Follow step 2. onwards same as Debug
# =================================================================================================
FLAVOR=$1
BUILD_TYPE=$2
# Need the location of the asset Directory to copy the necessary resources
ASSET_DIR=$3
EX_DIR=classes
#BUILD_TOOL_LOCATION=/home/gardev/android/android-studio/sdk/build-tools/19.1.0
BUILD_TOOL_LOCATION=$4

# This is the place where you need to specify the DEX dependencies
# Its poor but readable & configurable & requires you to know about the build process
# And all of the stuff that happens during it
# Hard coded stuff for the Game
PACKAGE=com/github/creativepsyco/secondarydex/bigmodule/lib

# Google Play Services has 2 packages
# You can apply the same principle to deal with different libraries
# This code assumes that you are doing a ProGuard step in Release.
# Otherwise things will break,
# You can very well change that after all, with some bash knowledge :)
# I think I am just a tard bit lazy coz all of my dev environment is *NIX based :P
#PACKAGE3=com/google/android/gms
#PACKAGE4=com/google/ads

OUTPACKAGE=com/github/creativepsyco/secondarydex/bigmodule/lib # The Appliction Pacakge Directory
#GOOGPACKAGE=com/google/android # The Google Play Services Package for the package classes

# The WORKDIR is the build area containing the classes
# Usually build/classes/flavour-name/release[debug]
if [ "$FLAVOR" != "" ]; then
    WORKDIR=build/intermediates/classes/${FLAVOR}/${BUILD_TYPE}
else
    WORKDIR=build/intermediates/classes/${BUILD_TYPE}
fi

if [ "$BUILD_TYPE" == "release" ]; then
    CURRENT_DIR=$(pwd)
    if [ "$FLAVOR" != "" ]; then
        WORKDIR=build/intermediates/classes-proguard/${FLAVOR}/${BUILD_TYPE}
    else
        WORKDIR=build/intermediates/classes-proguard/${BUILD_TYPE}
    fi
    # Cleanup
    echo "Performing Clean up"
    rm -rf ${WORKDIR}/${EX_DIR}
    rm -rf ${WORKDIR}/OUTPACKAGE

    mkdir ${WORKDIR}/${EX_DIR}
    mkdir -p ${WORKDIR}/OUTPACKAGE/${OUTPACKAGE}
#    mkdir -p $WORKDIR/OUTPACKAGE/$GOOGPACKAGE

    unzip -q ${WORKDIR}/classes.jar -d ${WORKDIR}/${EX_DIR}

# We move Interesting things (read class files) into 'OUTPACKAGE' LOL
# All the DEX dependencies go inside this directory
# The Structure is very important otherwise the DEX Compiler Will complain
# that the name of the class does not match its location wrt to the package
    echo "[DexPackage] Moving dependent classes into OUTPACKGE"
    mv ${WORKDIR}/${EX_DIR}/${PACKAGE}/* ${WORKDIR}/OUTPACKAGE/${OUTPACKAGE}/

# We now remove the old classes.jar & create a new one in its place using the jar tool
    rm ${WORKDIR}/classes.jar
    jar cvf  ${WORKDIR}/classes.jar -C  ${WORKDIR}/${EX_DIR}/ .
    # Run the DEX Tool
    echo "[DexPackage] Asset Merge Dir: $ASSET_DIR"
    echo "[DexPackage] Running Dex Tool Now"
    ${BUILD_TOOL_LOCATION}/dx --dex --output=${ASSET_DIR}  ${WORKDIR}/OUTPACKAGE

    # To force regeneration of assets
# Since Proguard takes place after the assets have been packaged, we must regenerate them
# This is a special zip file, where there is no compression otherwise all assets cannot be
# Natively Decoded. Dalvik sort of sucks. But we can cheat!
    echo "[DEXPackage] Unzipping Files in the Asset Dir"
    if [ "$FLAVOR" != "" ]; then
		LIB_NAME="app-${FLAVOR}-${BUILD_TYPE}"
	else
		LIB_NAME="app-${BUILD_TYPE}"
	fi

    rm -rf build/intermediates/libs/${LIB_NAME}
    unzip -q build/intermediates/libs/${LIB_NAME}.ap_ -d build/intermediates/libs/${LIB_NAME}
    cd ${ASSET_DIR}
    rm game.zip
    zip -qrn *:: game.zip *.dex
    rm classes.dex
    cd -
    mkdir -p build/intermediates/libs/${LIB_NAME}/assets/
    \cp -fr ${ASSET_DIR}/game.zip build/intermediates/libs/${LIB_NAME}/assets/
    rm build/intermediates/libs/${LIB_NAME}.ap_
    cd build/intermediates/libs/${LIB_NAME}
    zip -qrn *:: ../${LIB_NAME}.ap_ *
    cd ${CURRENT_DIR}
    rm -rf build/intermediates/libs/${LIB_NAME}
#    And we are done
    rm -rf ${WORKDIR}/OUTPACKAGE
    rm -rf ${WORKDIR}/${EX_DIR}
    echo "[DexPackage] Done generating Dexes & Assets"
else
    # Debug Mode
    # Cleanup
#    Must cd to project dir
    CURRENT_DIR=$(pwd)
    echo "Entering ${CURRENT_DIR}"
    cd ..
    echo "Performing Clean up"
    echo "Running the DEX Tool Now\n"
    # Run the DEX Tool
    echo "Asset Merge Dir: $ASSET_DIR"
    rm "${ASSET_DIR}/game.zip"
    "${BUILD_TOOL_LOCATION}/dx" --dex --output=${ASSET_DIR}  build/intermediates/exploded-aar/secondary-dex-gradle/lib/unspecified/classes.jar

    cd ${ASSET_DIR}
    rm game.zip
    zip -qrn *:: game.zip *.dex
    rm classes.dex
    cd "${CURRENT_DIR}"

# Copy over to the archive
    echo "[Dexing] Flavor is ${FLAVOR}"
	if [ "$FLAVOR" != "" ]; then
		LIB_NAME="app-${FLAVOR}-${BUILD_TYPE}"
	else
		LIB_NAME="app-${BUILD_TYPE}"
	fi
    echo "[Dexing] Lib name is $LIB_NAME"

	rm -rf build/intermediates/libs/"${LIB_NAME}"
    unzip -q build/intermediates/libs/"${LIB_NAME}".ap_ -d build/intermediates/libs/"${LIB_NAME}"
    # Make an asset dir if it does not exist
    mkdir -p "build/intermediates/libs/"${LIB_NAME}"/assets/"
    \cp ${ASSET_DIR}/game.zip build/intermediates/libs/"${LIB_NAME}"/assets/
    rm build/intermediates/libs/"${LIB_NAME}".ap_
    cd build/intermediates/libs/"${LIB_NAME}"
    zip -qrn *:: ../"${LIB_NAME}".ap_ *
    rm -rf build/intermediates/libs/"${LIB_NAME}"
    cd -
fi