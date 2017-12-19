# CheezDroid
Android-based Vision System


## To provision a device for robot use
* Enable device admin
1. Settings App > Security > Device Administrators > Click box 'on' for CheezDroid

* Enable device owner
1. adb shell
2. dpm set-device-owner com.team254.cheezdroid/.ChezyDeviceAdminReceiver

## How to Install ADB on the RoboRIO

Download and run the script from [here] (https://github.com/Team254/FRC-2016-Public/blob/master/installation/install.osx.sh). Note that this script has only been tested on Mac OS X; it hasn't been tested on Windows or Linux.

## Setting up Android Studios (for Windows)
For installing all neccesary libraries to compile the vision app:

1. Install Android Studio
2. Open the vision_app folder as a project in Android Studio
3. From the top bar, go Tools->Android->SDK Manager
4. Go to the SDK Tools tab
5. Make sure CMake, LLDB and NDK are all checked then press apply
6. Restart Android Studio and it should compile

If you run into issues, make sure that NDK is in your path.
If the error is about it not finding CV commands, follow these steps:

1. Download http://sourceforge.net/projects/opencvlibrary/files/opencv-android/3.1.0/OpenCV-3.1.0-android-sdk.zip/download
2. Unzip OpenCV-3.1.0-android-sdk.zip (OpenCV-android-sdk)
3. Create directory app/src/main/jniLibs
4. Copy OpenCV-android-sdk/sdk/native/libs/* to app/src/main/jniLibs/
