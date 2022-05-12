package com.xiaoyv.javaengine

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Html
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.PathUtils
import com.google.googlejavaformat.java.Formatter
import com.google.googlejavaformat.java.JavaFormatterOptions
import com.xiaoyv.java.compiler.JavaEngine
import com.xiaoyv.javaengine.databinding.ActivitySingleSampleBinding
import kotlinx.coroutines.*
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.widget.Toast

/**
 * CompileActivity
 *
 * @author why
 * @since 2022/3/9
 */
class CompileActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    private lateinit var binding: ActivitySingleSampleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySingleSampleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initEvent()

        setOutputSample()
    }

    private fun initEvent() {
        binding.toolbar.menu.add("input example")
            .setOnMenuItemClickListener {
                setInputSample()
                true
            }.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        binding.toolbar.menu.add("output example")
            .setOnMenuItemClickListener {
                setOutputSample()
                true
            }.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        binding.toolbar.menu.add("Run")
            .setOnMenuItemClickListener {
                formatCode()
                runProgram()
                true
            }.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)


    }

    private fun formatCode() {
        val codeText = binding.codeText.text.toString()
        try{
        val formatSource = Formatter(JavaFormatterOptions.builder()
            .style(JavaFormatterOptions.Style.AOSP)
            .build())
            .formatSource(codeText)
        binding.codeText.setText(formatSource)
        }catch(e: Exception){
            Toast.makeText(CompileActivity.this, e.message,Toast.LENGTH_SHORT).show() 
        }
    }

    /**
      * [JavaEngine.CompileExceptionHandler] Default exception catch implementation for internal compilation related coroutine scope.
      *
      * - You can customize the exception information thrown by the whole process
      */
    private fun runProgram() = launch(JavaEngine.CompileExceptionHandler) {
        binding.printView.text = null

        // build folder
        val buildDir = PathUtils.getExternalAppFilesPath() + "/SingleExample/build"

        // java folder
        val javaDir = PathUtils.getExternalAppFilesPath() + "/SingleExample/java"

        // Main.java to be compiled
        val javaFilePath = withContext(Dispatchers.IO) {
            // Main.java file in the java folder, write the code content
            val javaFilePath = "$javaDir/Main.java"

            FileIOUtils.writeFileFromString(javaFilePath, binding.codeText.text.toString())

            // return the source file path
            javaFilePath
        }

       // Compile the class, libFolder is the storage directory for the third-party jar, it can be left empty
       // After the compilation is completed, return the target classes.jar, which is internally processed by the coroutine in the IO thread
        val compileJar: File = JavaEngine.classCompiler.compile(
            sourceFileOrDir = javaFilePath,
            buildDir = buildDir,
            libFolder = null
        ) { taskName, progress ->

            // here is the progress, the callback is on the main thread...
            binding.printView.append(String.format("%3d%% Compiling: %s\n", progress, taskName))
        }
        binding.printView.append("Compiling class success!\n\n")

        binding.printView.append("Compiling dex start...\n")

        // Compile classes.dex, the information related to this step is output through System.xxx.print
        val dexFile = JavaEngine.dexCompiler.compile(compileJar.absolutePath, buildDir)

        binding.printView.append("Compiling dex success!\n\n")

        binding.printView.append("Run dex start...\n\n")

        // JavaEngine.
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
                        continuation.resumeWithException(Exception("Cancel the operation"))
                    }.create()

                dialog.show()
                dialog.setCanceledOnTouchOutside(false)
            },
            printOut = {
                binding.printView.append(it)
            },
            printErr = {
                binding.printView.append(Html.fromHtml("<font color=\"#FF0000\">$it</font>"))
            })

        binding.btSend.setOnClickListener {
            val input = binding.inputEdit.text.toString()
            programConsole.inputStdin(input)
            binding.inputEdit.text = null
        }
    }


    @SuppressLint("SetTextI18n")
    private fun setOutputSample() {
        binding.codeText.setText(
            "/**\n" +
                    " * @author Admin\n" +
                    " */\n" +
                    "public class Main {\n" +
                    "\n" +
                    "    public static void main(String[] args) {\n" +
                    "        System.out.println(\"Start Thread!\");\n" +
                    "        new Thread(()-> System.err.println(\"Hello World!\")).start();\n" +
                    "    }\n" +
                    "}"
        )
    }

    @SuppressLint("SetTextI18n")
    private fun setInputSample() {
        binding.codeText.setText(
            "import java.util.Scanner;\n\n" +
                    "public class Main {\n" +
                    "    public static void main(String[] args) {\n" +
                    "        System.out.println(\"Hello System.in\");\n" +
                    "        System.out.println(\"Please enter something\");\n" +
                    "        Scanner scanner = new Scanner(System.in);\n" +
                    "        String line = scanner.nextLine();\n" +
                    "        System.out.println(\"The following is your input:\");\n" +
                    "        System.out.println(line);\n" +
                    "    }\n" +
                    "}"
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}
