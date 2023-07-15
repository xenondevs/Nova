[![Banner](https://i.imgur.com/HiDAPmf.png)](https://hangar.papermc.io/xenondevs/Nova)

<p align="center">
  <a href="https://github.com/xenondevs/Nova/stargazers">
    <img src="https://img.shields.io/github/stars/xenondevs/Nova">
  </a>
  <a href="https://github.com/xenondevs/Nova/actions/workflows/ci.yml">
    <img src="https://img.shields.io/github/actions/workflow/status/xenondevs/Nova/ci.yml">
  </a>
  <a href="">
    <img alt="Modrinth Game Versions" src="https://img.shields.io/modrinth/game-versions/nova-framework">
  </a>
</p>
<p align="center">
  <a href="https://hangar.papermc.io/xenondevs/Nova">
    <img src="https://img.shields.io/modrinth/v/nova-framework?color=0552eb&label=Hangar">
  </a>
  <a href="https://modrinth.com/plugin/nova-framework">
    <img src="https://img.shields.io/modrinth/v/nova-framework?label=Modrinth">
  </a>
</p>


# Nova

Nova is a framework for developers to easily create custom items, blocks, GUIs, gameplay mechanics and more, without any client-side modifications.  
With Nova, developers don't have to deal with resource pack tricks, data serialization or world formats and can instead
just focus in adding content to the game.  
As a server administrator, you can choose from a set of Nova addons, which will add content to the game.

## Features

* Custom items
  * Custom wearables
    * Custom armor, armor toughness, knockback resistance
    * Custom armor textures
  * Custom food
  * Custom tools
    * Custom tool levels and categories
    * Custom break speed, attack speed, attack damage etc.
* Custom GUIs
* Custom blocks
  * Customizable break time, particles and sounds
  * Block break effect even on barrier blocks
  * Solid blocks via note- and mushroom blocks
* World Generation
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

To build Nova, run the `loaderJarSpigot` task for a spigot-mapped jar or `loaderJarMojang` for a mojang-mapped jar.  
You can specify an output directory using the `outDir` property: `-PoutDir="<path>"`.

# Plugin- and Addon API

If you're planning to make your own plugin compatible with Nova, you might be interested in our [Plugin API documentation](https://xenondevs.xyz/docs/nova/api).   
If you're interested in creating a Nova addon, check out our [Addon API documentation](https://xenondevs.xyz/docs/nova/addon/) and the [Nova-Addon-Template](https://github.com/xenondevs/Nova-Addon-Template).
