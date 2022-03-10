# Fabric GameProvider for Mindustry

This library enables [fabric](https://fabricmc.net/) mod development for the
game [Mindustry](https://mindustrygame.github.io/) by Anuke. As for now only the desktop client and server is supported.

## Usage

---

### Loading Mods

1. Go to the [releases](https://github.com/Qendolin/mindustry-fabric-loader/releases) and download the latest
   installer.  
   `mindustry-fabric-installer-x.x.x-fat.jar`
2. Run the installer and pick the Mindustry game directory.  
   The installer will look for the following jars:
    - `jre/desktop.jar` *itch.io release*
    - `Mindustry.jar` *current GitHub relase*
    - `desktop-release.jar` *old GitHub release*
    - `desktop.jar`
3. Once the installer is done all dependencies will be at `%LOCALAPPDATA%/Mindustry_Fabric` or your os's equivalent.  
   Also launch script named `mindustry_fabric` will be created in the selected directory.
4. Run the `mindustry_fabric` script. On the title screen, below the Mindustry logo there should now be a text that
   says `Modded` and the fabric version.
5. After the first launch a `mods` folder will be created inside the game directory where you can place your mods.

### Developing Mods

TODO
