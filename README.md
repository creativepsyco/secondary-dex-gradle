secondary-dex-gradle
====================
> To checkout a different version of loading the DEX files see branch [`method2`][2]

This project aims to acheive custom class loading in Dalvik as was outlined in the [Android Developers Blogpost][1]

How to run
----------
I have a gradle wrapper checked in so all you need to do is define a file `local.properties` with a variable called `sdk.dir` like this:

```
sdk.dir=Absolute Location of your Android SDK
```
If you import the project in Android Studio, it would automatically create one for you.

Compile & run. If everything is correct, then you should see a log message in Logcat with the tag: `MyLoader`

Structure
---------

The structure of this project is like this:

```bash
-Root Project
|-- app 
|-- lib (The secondary Dex Library)
```

We modify the `build.gradle` files to be able to churn out the Dex file for the secondary library

Take a look at [app's build.gradle](app/build.gradle) and [lib's build.gradle](lib/build.gradle)

We then use this and copy it into the assets folder which gets packaged into the final APK.

Once this is done, loading the dex at runtime is easy, and is explained in the blogpost.

A requirement is that both the library and the app will need to maintain a shared interface, otherwise the loading function will need to invoke methods at runtime via reflection.

[1]: http://android-developers.blogspot.sg/2011/07/custom-class-loading-in-dalvik.html
[2]: https://github.com/creativepsyco/secondary-dex-gradle/tree/method2
