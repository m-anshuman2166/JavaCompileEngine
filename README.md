# JavaCompileEngine (supports JDK8 syntax)

[![Mavnen Central](https://img.shields.io/maven-central/v/io.github.xiaoyvyv/compiler-d8?label=Maven%20Central)](https://search.maven.org /search?q=io.github.xiaoyvyv%20compiler-d8)

This is an engine that can compile and run `Java` code on the `Android` platform, and supports `JDK8` syntax to compile and run.

#### How it works:

> 1. Compile `*.java` files into `*.class` class files.
> 2. Use the `R8 | D8` tool of `Google` to compile the `*.class` file into a `*.dex` file that can run on the `Android` platform. This step can be de-sugared, and the higher version of `JDK` syntactic sugar can be run on the Android platform, and its internal implementation is basically the conversion of syntactic sugar into low-version compatible code.
> 3. Load the `*.dex` file into the application through `DexClassLoader`, and look for the `main` method reflection call.
> 4. Proxy system input `System.in` and output `System.out`.


## Sample download
[JavaEngineDemo](app_image/demo.apk?raw=true)

## Screenshot preview
| | | |
|:--|:--|:--|
| ![Screenshot preview](app_image/1.jpg?raw=true) |![Screenshot preview](app_image/2.jpg?raw=true) | ![Screenshot preview](app_image/3.jpg?raw=true ) |

## 1. Import dependencies
Step 1: Add the `jitpack.io` repository in the `build.gradle` of `Project`
````groovy
    allprojects {
        repositories {
            //...

            mavenCentral()
        }
    }
````
Step 2: Add dependencies
````groovy
    dependencies {
        //...

        // This dependency is relatively large (about 9M), because the internal (assets) contains a simplified Jre, you can choose to remove or simplify.
        implementation 'io.github.xiaoyvyv:compiler-d8:<maven-version>'
    }
````

## 2. Initialization
Initialize the library in `onCreate` of your `Application`.

```kotlin
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        
        //init
        JavaEngine.init(this)
    }
}
````

## 3. Compile Java files
`JavaClassCompiler` provides related methods, please refer to [JavaClassCompiler.kt](https://github.com/xiaoyvyv/JavaCompileEngine/blob/master-d8/compiler-d8/src/main/java/com/xiaoyv/ java/compiler/tools/java/JavaClassCompiler.kt).

> Note that this method needs to be used with coroutines and is called within the scope of coroutines

```kotlin
// Compile the class, libFolder is the storage directory for the third-party jar, it can be left empty
// After the compilation is completed, return the target classes.jar, which is internally processed by the coroutine in the IO thread
val compileJar: File = JavaEngine.classCompiler.compile(
    sourceFileOrDir = javaFilePath,
    buildDir = buildDir,
    libFolder = null
) { taskName, progress ->

   // here is the progress, the callback is on the main thread...
  }
````
## 4. Convert class file to dex file
`DexCompiler` provides related methods, please refer to [JavaDexCompiler.kt](https://github.com/xiaoyvyv/JavaCompileEngine/blob/master-d8/compiler-d8/src/main/java/com/xiaoyv/ java/compiler/tools/dex/JavaDexCompiler.kt)

> Note that this method needs to be used with coroutines and is called within the scope of coroutines

```kotlin
// Compile classes.dex, the information related to this step is output through System.xxx.print
val dexFile = JavaEngine.dexCompiler.compile(compileJar.absolutePath, buildDir)
````
## 5. Run the dex file
`JavaProgram.kt` provides related methods, please refer to [JavaProgram.kt](https://github.com/xiaoyvyv/JavaCompileEngine/blob/master-d8/compiler-d8/src/main/java/com/ xiaoyv/java/compiler/tools/exec/JavaProgram.kt)

Note that the default implementation of `chooseMainClassToRun` is to select the first `main` method that matches to run. You can choose a specific class to be executed in the method callback.
- chooseMainClassToRun The first callback parameter: the matched class containing the `main` method
- chooseMainClassToRun The second callback parameter: the callback related to the coroutine, you need to call back the selected class

<font color="#ff0000">Note: The chooseMainClassToRun callback will make the internal coroutine suspend all the time. You should let the internal know the processing result through continuation.resume() or resume.resumeWithException() in time. It is forbidden to ignore the continuation callback. Otherwise, resources will always be occupied in the background</font>

The `chooseMainClassToRun`, `printOut`, and `printErr` callbacks are all on the main thread and can be used for UI operations.

The completion of the `run` method (does not mean that the program execution is complete, for example your code starts another thread) will return a `programConsole` handle, which can be used to close the input and output streams.

> Note that this method needs to be used with coroutines and is called within the scope of coroutines

```kotlin
//JavaEngine.
val programConsole = JavaEngine.javaProgram.run(dexFile, arrayOf("args"),
    chooseMainClassToRun = { classes, continuation ->
        val dialog = AlertDialog.Builder(this@CompileActivity)
            .setTitle("Please select a main function to run")
            .setItems(classes.toTypedArray()) { p0, p1 ->
                p0.dismiss()
                continuation.resume(classes[p1])
            }
            .setCancelable(false)
            .setNegativeButton("Cancel") { d, v ->
                d.dismiss()
                continuation.resumeWithException(Exception("Cancel operation"))
            }.create()

        dialog.show()
        dialog.setCanceledOnTouchOutside(false)
    },
    printOut = {
        binding.printView.append(it)
    },
    printErr = {
        binding.printView.append(it)
    })
````
## 6. Processing of input data such as Scanner, etc.
Data can be entered by directly calling the `inputStdin(String stdin)` method of `programConsole.`.
```kotlin
    programConsole.inputStdin(str)
````
## 7. Compile related settings
`JavaEngine.compilerSetting` provides related configuration. [JavaEngineSetting.kt](https://github.com/xiaoyvyv/JavaCompileEngine/blob/master-d8/compiler-d8/src/main/java/com/xiaoyv/java/compiler/JavaEngineSetting.kt)

## 8. Questions
For more information, please refer to [Demo](https://github.com/xiaoyvyv/JavaCompileEngine/tree/master/app) and source code [compiler-d8](https://github.com/xiaoyvyv/JavaCompileEngine/tree/master /compiler-d8/)

## 9. Feedback
QQEmail: 1223414335@qq.com
