# No More Max/MSP

Goal is to replace our use of the MSP Patch, and get our routing saved into version control.


## OSC Sources

- ShowKontrol (1 output, pointed at lighting-1)
- VJLab (Running on lighting-1, supports multiple outputs - will route directly to all receivers)
- TE Chromatik (Running on lighting-1, supports multiple outputs (with filters))


## VJLab Destinations

- [ ] TODO: update VJLab config file

## Chromatik OSC Destinations (No Remapping)

For any OSC paths already in Chromatik's engine, we can just add filters to output them to their destinations. 

### mship-led:

```
/lx/director/foh
/lx/palette/swatch/color/
/lx/tempo
```

### ipad-1,ipad-2:


```
/lx/focus/channel/pattern/focused/parameter/
/lx/focus/channelAux/pattern/focused/parameter/
```

- [ ] do we need to send anything else, or is this enough?

### mship-laser: (?)

```
/lx/director/lasers (?)
```

### TODO (?)

- [ ] what else? check MSP patch



## Chromatik OSC Destinations (With Address Remapping)

Sometimes the receiver expects a value from Chromatik under a different OSC address than Chromatik uses:

```
/lx/tempo/bpm -> /b/Master/BPM
```

In such cases, we'll need to do that as a Chromatik plugin which publishes those OSC messages into the engine. Once they're in the engine, we can configure them to get sent to their destination using the normal Chromatik LSC Output system.

### Lasers/Beyond

This is being handled by a Chromatik plugin `Beyond` from @jkbelcher.

Paths:

```
/b/Master/BPM
/b/MasterLC/Brightness
/b/Channels/15/Value
/b/Channels/16/Value
```

- [ ] TODO: 32-beat "resync"? ask @jeffvyduna

### (Maybe) Linkups

Forwarding tempo/color/etc to other art cars...

Also possible we'll receive them with weird input names and need to translate them to, for example, correctly update our colors, if they're sending us something like `/grandMA/colors/1/hue` and we need to route that to `/lx/palette/swatch/color/1/hue`.

### (Maybe) Resolume

Ask Sina / Misha. Likely:
- tempo
- color
- (?) Director fader?

## Possible Approaches for Remapping

Simplest might be to mirror approach of Beyond Plugin - shouldn't be too hard to code one on-playa if needed.

Another option - what if we could externalize this into a config file in version control? Then we have a Chromatik plugin to load up that file, and execute the remappings.

```jsonc
{
  "remappings": {
    "/grandMA/colors/1/hue": "/lx/palette/swatch/color/1/hue",
    "/lx/tempo/bpm": "/b/Master/BPM"
  }
}
```

Given our experience with lasers, it's possible to imagine this would only work if we could pass over the value as-is - but if it needs to be normalized/scaled (e.g. hue ranging from `0-360` needs to be sent as `0-1`), it's possible we could define some kind of transformations.

```jsonc
{
  "remappings": {
    "/grandMA/colors/1/hue": {
      "dest": "/lx/palette/swatch/color/1/hue",
      "transforms": "clamp($value / 360.0, 0.0, 1.0)"
    },
    "/lx/tempo/trigger": {
      "dest": "/b/Master/BPM",
      // I'm making this up, but imagine someone wanted an inverse of how beat is triggered
      "transforms": "abs(1.0 - $value)"
    }
  }
}
```

One idea is to re-use Chromatik fixture file expression language - that might be easy to embed, but it's not designed for performance (so doing this for a hihg-traffic OSC param might be prohibitive). It'd probably be okay though for params that aren't changing that often - main palette hue, Director controller faders (depending on how they're being used by the director).

Given that this only needs one variable to be defined (value), it also might be possible to just have a list of operations to apply sequentially:

```jsonc
{
  "remappings": {
    "/grandMA/colors/1/hue": {
      "dest": "/lx/palette/swatch/color/1/hue",
      "transforms": [
        ["div", 360.0],
        ["clamp", 0.0, 1.0]
      ]
    },
    "/lx/tempo/trigger": {
      "dest": "/b/Master/BPM",
      "transforms": [
        ["sub", 1.0],
        ["abs"]
      ]
    }
  }
}
```


