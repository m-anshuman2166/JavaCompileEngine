package com.xiaoyv.javaengine

import android.app.Application
import com.xiaoyv.java.compiler.JavaEngine

/**
  * @author Wang Huaiyu
  * @since 2020/5/10
  */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        JavaEngine.init(this)
    }
}
