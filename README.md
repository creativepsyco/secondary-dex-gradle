secondary-dex-gradle
====================
[ ![Codeship Status for creativepsyco/secondary-dex-gradle](https://www.codeship.io/projects/6a9c2100-f318-0131-3c3a-7e98aa4e2896/status)](https://www.codeship.io/projects/27799)

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

Overview of Method 2
---------------------

Shortcomings of the previous method:

* Since you need a dynamic interface, you must make sure all third-party libraries must be invoked this way, which is not practical (Google Play services adds about 11K methods, support libaries almost the same amount, so if you know what that means, you know what I am talking about)
* You cannot do Proguard since the process of churning out DEX files happens at different stages.
* You cannot load Activities, and you must dynamically cast all interfaces at runtime to achieve what you want to go.
* This makes your app highly dependent on the secondary DEX generation method which might be a bit risky.

For these reasons, I thought of using simple Bash scripting to achieve what i want to do. Simply put:

* Be able to plugin the secondary dex mechanism when I want and be able to turn it off. This makes development simple devoid of any potential errors
* Be able to do Proguard on the entire app, including secondary DEXes.
* Be able to filter out third-party jars (Often the culprit behind the high method count)
* Avoid reflection & interface casts wherever possible.

Its a very **hackish** solution, but I think it works fine, and does not incur any high penalty, I was able to reduce the Main DEX file size of one of the apps I work on to `4.5 MB` from `7.1 MB` and it worked on Gingerbread devices with a breeze (At least guaranteed to run on 99% of the gingerbread devices). Some of the devices have a weird way of managing filesystems, and sometimes loading DEX files fails and I am yet to find out why. But anyways, this is a pluggable and battle-tested solution.

I am no gradle expert so I churned out a bash script to do this. Would love to get a pull-request with a gradle based solution :)

Overview of Multiple Dexing methods
-----------------------------------

The Multiple DEXing has 2 phases:

* Generation of Multiple DEX files
* Loading of Classes & interfaces from DEX files at runtime

The Generation of Multiple DEX files have the following methods

* Use Build Toolchain to generate multiple DEX files. This will defer on Ant, Gradle. Each have tasks to customize this
* Use scripts to generate the DEX files. (Pros: works on *NIX Cons: works only on *NIX)

The Loading has multiple methods too:

* Use a shared interface between 2 modules. Cast the the dynamically loaded module (from the DEX file) into an interface & cache it for usage.
* Use pure reflection to do code execution. Cache the methods. (Cons: Slow & waste memory)
* Hack the classpath (via reflection) to allow loading of classes from multiple location.

Implementation
--------------

* Fix [`build.gradle`](app/build.gradle)  to inject a task via shell script to generate the DEXes.
* In the [`package.sh`](package.sh) script do a preprocessing of sort to generate Dex files, insert & merge resources etc.
* You might need to refer to the Android gradle plugin's source code to find out which stages are done when, and how you can take advantage of it.
* In a gist, we generate a DEX file, package it inside an assets folder & load it up in runtimes as well as hack the path to allow the system to treat 2 location of dex files. Check out [`FrameworkHack.java`](app/src/main/java/com/github/creativepsyco/secondarydex/plugin/FrameworkHack.java) on how this is done. Thanks to: [Ruboto](https://github.com/ruboto/ruboto-core/blob/master/src/org/ruboto/FrameworkHack.java). This allows activities etc. to be loaded from the secondary Dex file.


Structure
---------
The structure of this project is like this:

```bash
-Root Project
|-- app 
|-- lib (The secondary Dex Library)
```


[1]: http://android-developers.blogspot.sg/2011/07/custom-class-loading-in-dalvik.html
[2]: https://github.com/creativepsyco/secondary-dex-gradle/tree/method2
