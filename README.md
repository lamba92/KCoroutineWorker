# KCoroutineWorker [![Build Status](https://travis-ci.org/lamba92/KCoroutineWorker.svg?branch=master)](https://travis-ci.org/lamba92/KCoroutineWorker) [![](https://jitpack.io/v/lamba92/KCoroutineWorker.svg)](https://jitpack.io/#lamba92/KCoroutineWorker)

Commodity classes to implement cyclic workers.

Written in Kotlin with ❤️.

## Usage

Extend `AbstractCoroutineWorker` and implements the methods you need.

A CoroutineWorker basically is a worker that executes always the method `execute()` and when needed `executeMaintenance()` like so: 
```
while (itShouldRun) {
    execute()
    if (shouldDoMaintenance)
        executeMaintenance()
    delay(customTime)
}
``` 

There are few overridable methods that allows to execute some code in between the worker lifecycle:
 - `onStart()`: Executed before entering the main cycle.
 - `onCancel()`: Executed as soon as the `stop()` is called on the worker. In this scope the worker has not yet received the cancellation signal.
 - `onPostCancel()`: Last method executed by the worker when stopped.
 - `onReset()`: It is executed when `reset()` is called on the Worker just after `onPostCancel()`.
 
The worker is controllable using:
 - `start(wait: Boolean = false)`: starts the worker, eventually awaiting it to finish it's work.
 - `stop(wait: Boolean = false)`: stops the worker, eventually awaiting it to finish stopping.
 - `restart(wait: Boolean = false)`: restart the worker, eventually waiting for it to stop.
 - `reset(wait: Boolean = false)`: restart the worker executing `onReset()` in between, eventually waiting for it to stop.

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

