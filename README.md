# Minecraft Photographer

Minecraft Camera plugin for Paper and Spigot servers. Adds craftable cameras to your Minecraft server and the
ability to take photos. Works with Geyser, though the camera looks like a Steve head.

This is a server side only plugin.

## Features 

* Players can change how photos are rendered through commands, e.g. shadows, shading, dithering (See more `/camera help`).
* In Overworld the sky color changes depending on the time of day.
* Sun and moon are rendered on the sky.
* Field of view can be adjusted through the config.
* Camera skin can be changed through the config.
* Render distance can be adjusted through the config.
* Block colors can be updated manually through mapping-file.
* Photos that despawned or has been thrown into lava, will be removed from the database.
* Photos are compressed before saved to the database and take up very little space (0.5-3 KB).


## Installation

1. [Download the plugin](https://github.com/SnorklingTurtle/Minecraft-Photographer/releases/)
2. Move the jar-file into the server plugin-folder
3. Start the server

If you don't already have a config file, one will be created on first boot of the server. The config is located at `plugins/Minecraft-Photographer/config.yml`. Restart the server after editing the config.

Photos are stored in a SQLite database at `plugins/Minecraft-Photographer/photos.db`.


## Commands

* `/camera help` - List of helpful commands.
* `/camera aa` - Toggles anti-aliasing
* `/camera shading` - Toggles shading on blocks.
* `/camera shadows` - Toggles shadows.
* `/camera dithering` - Toggles dithering.

## Permissions

To allow players to craft and use the camera you must set permissions for at least `cameras.craft` and
`cameras.use` and optionally `cameras.command`. You can either set the permissions in
permissions.yml or through LuckPerms. Other permissions plugins might work as well, but haven't been tested.

Here's an example for permissions.yml:

```yml
  camera.craft:
    description: Allows crafting the camera.
    default: true
  camera.use:
    description: Allows using the camera item.
    default: true
  camera.command:
    description: Allows camera commands /camera
    default: op
  camera.usepaper:
    description: Require paper when taking photos
    default: true
```

## Limitations

* If your server uses Multiverse-Core, you will run into issues if you change the default 
world. Map IDs will no longer match those in the database, which might end up in some maps or photos getting overwritten.
* Players and mobs won't show up on photos - *X-Files theme plays*

## Todo

* [Idea] Make a tool that easily grabs colors of new blocks. E.g. look at block and do `/camera pick-color`.
* [Fix] Keep track of map IDs and when players copy/destroy maps 
* [Optimize] Check color mapping
* [Idea] Better rendering of water. Trace through, but add blue tint.
* [Idea] Framing
* [Idea] Toggle sky gradient
* [Fix] [Skull rendering on Geyser](https://geysermc.org/wiki/geyser/custom-skulls/)
* [Idea] Add a `/camera silly` command - to take silly photos
* [Idea] Add option to change Overworld, Nether and End sky colors.
* [Idea] Use getBlockData().getMapColor() and fallback to color-mapping.config only if needed
* [Idea] Store camera properties per camera
* [Optimize] Can converting colors from string be improved


## Done

* Photos are distorted when looking up/down.
* [Idea] Prettier sky


## Issues

Any features requests or issues should be made through here:

https://github.com/SnorklingTurtle/Minecraft-Photographer/issues