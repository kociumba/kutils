# kutils

A simple (probably buggy) collection of qol features
and tweaks that I am used to from 1.8.9 but for 1.21+
with some unique features for hypixel skyblock features.

This mod is fully client sided and tries to stay within the bounds of hypixel mod legality.

## Features

kutils implements the well known [ImGui](https://github.com/ocornut/imgui) library for its ui,
features with custom ui are using imgui, and the config screen is using [Vigilance](https://github.com/EssentialGG/Vigilance).

Huge thanks to [imgui-mc](https://github.com/AlignedCookie88/imgui-mc) for updating the imgui minecraft
bindings to 1.21+

Implemented features:
- custom damage tint
- disable damage tint (by setting the opacity to 0)
> stopped being stupid and wrote a proper mixin for this 😎
> no more core shaders and resource reloading
- time changer (freeze time client side on a specific tick amount)
- always sprint toggle (does not reset on death)

WIP features:
- in game calculator with simple controls (in progress of migrating to an imgui based ui)
- in game implementation of [Skydriver](https://www.github.com/kociumba/Skydriver) (my cli bazaar data app)

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