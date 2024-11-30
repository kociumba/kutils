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
 - custom damage tint (opacity 0 turns off tint)
    > [!NOTE]
   >  this is implemented using core shaders instead of a mixins
   >  couse I'm stupid and could figure it out myself, so you need to reload resources to
   >  apply changes. I'm fully intending to migrate this to mixins when I figure out how.
 - time changer (freeze time on a specific tick amount)

## FAQ

#### Q: why 1.21+ instead of 1.8.9?
A: because 1.21+ has much better performance than 1.8.9, and while hypixel
will most likely never remove support for joining on 1.8.9, there are already
some gamemodes only accessible on 1.21+, and the same was said to be the case for 
skyblock's upcoming foraging update.

#### Q: why is there no X feature?
A: first of all suggest it in [issues](https://github.com/kociumba/kmod/issues),
but aside from that the only reason I'm at all attempting to write a minecraft mod
is kotlin compatibility, as I despise java itself and if it was the only choice,
this mod would never exist (yes mixins are still in java but mixins almost feel like a dsl).