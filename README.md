# Minecraft Photographer

Minecraft Camera plugin for Paper and Spigot servers. Adds craftable cameras to your Minecraft server and the
ability to take photos. Works with Geyser, though the camera looks like a Steve head.

This is a server side only plugin.

## Features 

* Players can change how photos are rendered through commands, e.g. shadows, shading, dithering (See more `/camera help`).
* Players can add a frame to a photo by using any dye color on it. A frame can be removed again by shearing the photo.
* Lots of admin settings such as: 
  * Camera skin
  * Render distance
  * Default camera settings (shading, antialiasing, fov, etc.)
  * Block colors can be updated manually through mapping-file.
* Photos are compressed before saved to the database and take up very little space (0.5-3 KB).
* Photos that despawned or has been thrown into lava, will be removed from the database.


## Installation

1. [Download the plugin](https://github.com/SnorklingTurtle/Minecraft-Photographer/releases/)
2. Move the jar-file into the server plugin-folder
3. Start the server

If you haven't got a config file already, one will be created when booting the server. The config is located at `plugins/Photographer/config.yml`. Restart the server after editing the config.

Photos are stored in a SQLite database at `plugins/Photographer/photos.db`.


## Commands

* `/camera help` - Lists all commands.
* `/camera antialiasing` - Toggles antialiasing
* `/camera shading` - Toggles shading on blocks.
* `/camera shadows` - Toggles shadows.
* `/camera dithering` - Toggles dithering.
* `/camera fov <value>` - Changes field of view.

## Permissions

To allow players to craft and use the camera you must set permissions for at least `camera.craft` and
`camera.use` and optionally `camera.command`. You can either set the permissions in
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
    description: Allows changing camera settings through /camera commands
    default: true
  camera.admincommand:
    description: Allows changing plugin settings through /camera commands
    default: op
  camera.consumepaper:
    description: Consume paper when taking photos
    default: true
  camera.consumedye:
    description: Consume dye when coloring an item frame
    default: true
```

## Limitations

* If your server uses Multiverse-Core, you will run into issues if you change the default 
world. Map IDs will no longer match those in the database, which might end up in some maps or photos getting overwritten.
* Players and mobs won't show up on photos - *X-Files theme plays*

## Todo

* [Fix] [Skull rendering on Geyser](https://geysermc.org/wiki/geyser/custom-skulls/)
* [Fix] Clean up: use 'getInstance()' everywhere instead of passing instance as parameter
* [Fix] Adding use structures names for photos, only when enabled
* [Optimize] Check color mapping
* [Bug] Better rendering of glass
* [Idea] Toggle sky gradient
* [Idea] Add a `/camera silly` command - to take silly photos
* [Idea] Add option to change Overworld, Nether and End sky colors.
* [Idea] Use getBlockData().getMapColor() and fallback to color-mapping.config only if needed
* [Optimize] Can converting colors from string be improved

## Done

* [Idea] Framing
* [Idea] Store camera properties per camera
* [Idea] Make a tool that easily grabs colors of new blocks. E.g. look at block and do `/camera pick-color`.
* [Idea] Better rendering of water. Trace through, but add blue tint.
* [Bug] Photos are distorted when looking up/down.
* [Idea] Prettier sky
* [Fix] Keep track of map IDs and when players copy/destroy maps

## Issues

Any features requests or issues should be made through here:

https://github.com/SnorklingTurtle/Minecraft-Photographer/issues