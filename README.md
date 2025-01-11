# kutils

A collection of qol features
and tweaks that I am used to from 1.8.9 but for 1.21+
with some unique features for hypixel skyblock.

This mod is fully client sided and tries to stay within the bounds of hypixel mod legality.

## Usage

Get the mod from:
- [github releases](https://github.com/kociumba/kutils/releases)
- [curseforge](https://www.curseforge.com/minecraft/mc-mods/kutils)
- [modrinth](https://modrinth.com/mod/kutils)

Install it on a fabric 1.21.1 instance.

In game use `/kutils` or press `right shift` to open the in game config.

Bazaar UI is by default bound to `insert`, and the calculator is by default bound to `b`.

> you can of course change these binding in the settings

## Features

kutils implements the well known [ImGui](https://github.com/ocornut/imgui) library for its ui,
features with custom ui are using imgui, and the config screen is using [Vigilance](https://github.com/EssentialGG/Vigilance).

Huge thanks to [imgui-mc](https://github.com/AlignedCookie88/imgui-mc) for updating the imgui minecraft
bindings to 1.21+

> [!IMPORTANT]
> The non-cross-platform issue should now be resolved, so if you want to help test the mod on platforms other than windows,
> please do so and report any issues.

Using imgui in minecraft is quite unconventional, so the experience of kutils resembles
using a separate companion app hosted in minecraft more than a classic mod.
In fact, you can even drag the custom window outside the minecraft window that hosts them, 
which could be usefull if you don't play in fullscreen or have more than 1 monitor.

But this is only true for the bazaar ui and performance overlay, every other qol feature
seamlessly integrates with minecraft through an in game config screen and keybinds.

> [!WARNING]
> If any other mod provides the same features as kutils, they may be incompatible.
> But most of the time, features should not conflict or can be turned off.

Implemented features:
- custom damage tint
- disable damage tint (by setting the opacity to 0)
- time changer (freeze time client side on a specific tick amount)
- always sprint toggle (does not reset on death)
- cpu and ram usage statistics HUD
- custom minecraft window title with in game config
- remove block breaking particles (may boost performance, because it fully disables them instead of hiding them)
- fullbright using high gamma
- remove F5 "selfie" camera
- historical item price graphs like in [NotEnoughUpdates](https://github.com/NotEnoughUpdates/NotEnoughUpdates),
    to open the graph, click on any item in the bazaar ui.
- image previews in chat, when hovering over an image
- submit signs by pressing `enter`

> [!NOTE]
> Since v0.0.3 any imgui windows from kutils, will save their size, position etc. and restore
> them on next launch.

Basically done features 🤷:
- in game calculator with simple controls (just some ui adjustments left)
- note-taking in game with pinnable custom text

Features with weird issues 💀:
- in game implementation of [Skydriver](https://www.github.com/kociumba/Skydriver) (my cli bazaar data app)
> For anyone more technically interested in reading this, there are issues with the predictions math
> which is essentially ported 1 to 1 from [Skydriver](https://www.github.com/kociumba/Skydriver).
> I honestly think there are some wierd edge cases with the kotlinx serialization.

WIP features:
- health/armor/damage hud
- display other useful stats on the hud
- [chattriggers](https://www.chattriggers.com/) like scripting with lua / to see the docs go to [kutils docs](https://kociumba.gitbook.io/kutils/)

Planned features:
- any qol or data display features
- might think of trying something with custom skyblock content

## More on Bazaar UI

> [!IMPORTANT]
> There is still a weird issue with the predictions here,
> which I can't reliably pinpoint. So for the time being just consider
> the predictions inaccurate. Any other stats here are 100% straight from 
> the hypixel api.

This is the biggest part of the mod, and has a lot of extra features that have
never made it into [Skydriver](https://www.github.com/kociumba/Skydriver).
For example there is inflated item protection - you can set a custom percentage,
and if an items instant sell or buy price is higher than the average price
from the last 7 days + the custom percentage, it will be marked as inflated.

You can also customize the font size of any feature using imgui, which can lead to 
some pretty funny looking uis.

Most windows can also be docked to each other and collapsed.

> [!NOTE]
> The fonts do get blurry when changing font scales, this is unavoidable,
> until I add font loading to the original imgui package.

### Bazaar UI Showcase

<div style="display: flex; justify-content: space-between;">
    <img src="https://raw.githubusercontent.com/kociumba/kutils/refs/heads/main/assets/img.png" width="49%" alt="common usage showcase">
    <img src="https://raw.githubusercontent.com/kociumba/kutils/refs/heads/main/assets/1KAktb7(1).png" width="49%" alt="windows outside of minecraft">
</div>
<div style="display: flex; justify-content: space-between;">
    <img src="https://raw.githubusercontent.com/kociumba/kutils/refs/heads/main/assets/ORy3jX0(1).png" width="49%" alt="funny text scaling">
    <img src="https://raw.githubusercontent.com/kociumba/kutils/refs/heads/main/assets/QrVSBgF.png" width="49%" alt="docking showcase">
</div>
<div style="display: flex; justify-content: space-between;">
    <img src="https://raw.githubusercontent.com/kociumba/kutils/refs/heads/main/assets/img_1.png" width="49%" alt="price graphs">
    <img src="https://raw.githubusercontent.com/kociumba/kutils/refs/heads/main/assets/bazaarUI opacity.png" width="49%" alt="ui opacity">
</div>
<div style="display: flex; justify-content: space-between;">
    <img src="https://raw.githubusercontent.com/kociumba/kutils/refs/heads/main/assets/img_2.png" width="49%" alt="price graphs">
    <img src="https://raw.githubusercontent.com/kociumba/kutils/refs/heads/main/assets/img_3.png" width="49%" alt="ui opacity">
</div>

## Known issues

- chat image preview doesn't work with chatpatches restored chat history
- if an imgui hud window is opened while draggability is off, and outside the minecraft window, it will be stuck there

## FAQ

#### Q: why 1.21+ instead of 1.8.9?
A: because 1.21+ has much better performance than 1.8.9, and while hypixel
will most likely never remove support for joining on 1.8.9, there are already
some gamemodes only accessible on 1.21+, and the same was said to be the case for 
skyblock's upcoming foraging update.

#### Q: why is there no X feature?
A: first of all suggest it in [issues](https://github.com/kociumba/kutils/issues),
but aside from that I'm just not really a java or kotlin dev, so maby I don't know how to do it,
or I just didn't think of it.