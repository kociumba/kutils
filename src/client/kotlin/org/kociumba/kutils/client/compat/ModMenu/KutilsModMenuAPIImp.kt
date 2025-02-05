package org.kociumba.kutils.client.compat.ModMenu

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import org.kociumba.kutils.client.c

class KutilsModMenuAPIImpl : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*>? {
        return ConfigScreenFactory { c.gui() }
    }
}
