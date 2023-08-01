package ru.sliva.vanish

import java.util.Collections

object VanishService {

    val vanishList = Collections.unmodifiableList(Vanish.vanishList)
    val onlinePlayers = Vanish.onlinePlayers
}