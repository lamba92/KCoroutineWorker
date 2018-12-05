# KCoroutineWorker [![Build Status](https://travis-ci.org/lamba92/KCoroutineWorker.svg?branch=master)](https://travis-ci.org/lamba92/KCoroutineWorker) [![](https://jitpack.io/v/lamba92/KCoroutineWorker.svg)](https://jitpack.io/#lamba92/KCoroutineWorker)

Commodity classes to implement cyclic workers.

Written in Kotlin with ❤️.

## Usage

Extend `AbstractCoroutineWorker` or `FilesQueueWorker` and implements the methods you need.

## Installing [![](https://jitpack.io/v/lamba92/KCoroutineWorker.svg)](https://jitpack.io/#lamba92/KCoroutineWorker)

Add the [JitPack.io](http://jitpack.io) repository to the project `build.grade`:
```
repositories {
    maven { url 'https://jitpack.io' }
}
```

Then import the latest version in the `build.gradle` of the modules you need:

```
dependencies {
    implementation 'com.github.lamba92:KCoroutineWorker:{latest_version}'
}
```

If using Gradle Kotlin DSL:
```
repositories {
    maven(url = "https://jitpack.io")
}
...
dependencies {
    implementation("com.github.lamba92", "KCoroutineWorker", "{latest_version}")
}

