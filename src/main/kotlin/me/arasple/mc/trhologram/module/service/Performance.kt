package me.arasple.mc.trhologram.module.service

import io.izzel.taboolib.kotlin.Mirror
import me.arasple.mc.trmenu.module.internal.service.Performance

/**
 * @author Arasple
 * @date 2021/2/6 19:31
 */
object Performance {

    fun collect(opt: Mirror.Options.() -> Unit = {}): Mirror.MirrorCollect {
        return MIRROR.collect(opt)
    }

    @Suppress("DEPRECATION")
    inline fun check(id: String, func: () -> Unit) {
        Performance.MIRROR.check(id, func)
    }

    val MIRROR = Mirror()

}