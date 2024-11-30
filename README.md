# Kmod

A simple (probably buggy) collection of qol features
and tweaks that I am used to from 1.8.9 but for 1.21+

This mod is fully client side and will never feature server side features.
This is because it is intended for use on servers like hypixel just like 
the old 1.8.9 mods.

## Overview

Kmod uses [Vigilance](https://github.com/EssentialGG/Vigilance) by the Essential team for its config.
While I know some people do not like Essential, this library is separate from the ecosystem,
and does not require Essential to be installed.

## Features

Implemented features:
 - custom damage tint
 - disable damage tint (by setting the opacity to 0)
> stopped being stupid and wrote a proper mixin for this 😎
> no more core shaders and resource reloading
 - time changer (freeze time client side on a specific tick amount)
 - always sprint toggle (does not reset on death)

## FAQ

#### Q: why 1.21+ instead of 1.8.9?
A: because 1.21+ has much better performance than 1.8.9, and while hypixel
will most likely never remove support for joining on 1.8.9, there are already
some gamemodes only accessible on 1.21+, and the same was said to be the case for 
skyblock's upcoming foraging update.

#### Q: why is there no X feature?
A: first of all suggest it in [issues](https://github.com/kociumba/kmod/issues),
but aside from that I'm just not really a java or kotlin dev, so maby I don't know how to do it,
or I just didn't think of it.