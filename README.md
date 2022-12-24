[![Banner](https://i.imgur.com/HiDAPmf.png)](https://www.spigotmc.org/resources/93648/)

<p align="center">
  <a href="https://www.spigotmc.org/resources/93648/reviews">
    <img src="https://img.shields.io/spiget/rating/93648"> 
  </a>
  <a href="https://www.spigotmc.org/resources/93648/">
    <img src="https://img.shields.io/spiget/downloads/93648"> 
  </a>
  <a href="https://www.spigotmc.org/resources/93648/">
    <img src="https://img.shields.io/spiget/tested-versions/93648"> 
  </a>
</p>

# Nova

Nova is a framework for developers to add custom items, blocks, guis and more to Spigot servers.  
With Nova, developers don't have to deal with resource pack tricks, data serialization or world formats and can instead
just focus in adding content to the game.

## Features

* Custom items
  * Custom wearables
    * Custom armor, armor toughness, knockback resistance
  * Custom food
  * Custom tools
    * Custom tool levels and categories
    * Custom break speed, attack speed, attack damage etc.
* Custom GUIs
* Custom blocks
  * Customizable break time, particles and sounds
  * Block break effect even on barrier blocks
  * Solid blocks via note- and mushroom blocks
* TileEntity system
* Attachment system
* Ability system
* Recipe system
  * Custom recipe types possible
* Cable network system
  * Custom cables and network types possible
* Built-in items GUI
  * Recipe Explorer
* Built-in WAILA (What Am I Looking At)
* Built-in compatibility with the most popular custom item and protection plugins

# Translating

If you would like to help translate Nova, you can do so [here](https://translate.xenondevs.xyz/).

# Building

To build Nova, run the `loaderJar` gradle task.  
You can specify an output directory with the VM option `"-DoutDir<path>"` and build a mojang-mapped jar with `-Dmojang-mapped`.

# Plugin- and Addon API

If you're planning to make your own plugin compatible with Nova, you might be interested in our [Plugin API documentation](https://xenondevs.xyz/docs/nova/api).   
If you're interested in creating a Nova addon, check out our [Addon API documentation](https://xenondevs.xyz/docs/nova/addon/) and the [Nova-Addon-Template](https://github.com/xenondevs/Nova-Addon-Template).
