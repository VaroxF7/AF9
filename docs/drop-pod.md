# Drop Pod

A new player does not start on the ground: they are taken to the top of the sky above the spawn, strapped into a pod, and
come down. The idea is Supersymmetry's arrival; the implementation is AF9's own (af9-core `droppod`: `DropPodEntity`,
`AF9DropPod`, and the model and renderer in `droppod/client`; the texture is drawn by `tools/textures/drop_pod.py`).

## 1. The arrival

* **Who**: a player joining a world for the first time. The mark is kept in Forge's persisted player data (`af9_drop_pod_arrived`)
  and a play time of zero is checked as well, so players who have already played in a world the pack is added to are never
  dropped. Creative and spectator players skip it, and so do other dimensions than the Overworld.
* **Where from**: `dropPod.height` blocks above the spawn (250), never above the build limit. A title says "AF9" and the pod's line.
* **The fall**: terminal speed 0.5 blocks a tick (10 blocks a second, so about 24 s from 250), flames and smoke from the four
  thrusters, a rumble every 6 ticks. The rider cannot get out: a dismount is undone while the pod falls, and the rider takes no
  fall damage.
* **Soft blocks**: the pod flattens whatever is under it in a 3 x 3 with a hardness under 0.3 (leaves, plants, snow, carpets)
  and drops nothing; anything harder it lands on.
* **Landing**: on the ground, or on water or lava; a block-break sound of the block under it, a burst of its particles, a
  thump. The door slides up and the restraint swings away. The rider is let go of 1.5 s after landing.
* **Lift-off**: 7 s after landing the pod flies up, faster and faster, through soft blocks; under anything hard it just
  disappears. It never explodes and breaks nothing but the soft blocks. It is gone above the build limit.
* A player who logs out in the fall is put back in the pod when they come back (it is saved with its chunk).

## 2. Settings (`config/af9-common.toml`, `[dropPod]`)

| Key | Default | |
| --- | --- | --- |
| `onFirstJoin` | true | the arrival at all |
| `height` | 250 | blocks above the spawn, 40 to 1000 |

## 3. Not copied

Supersymmetry's mod is GPL-3.0 and AF9's is not: its code, model, texture and sounds are not used. The pod is a different
design (an open cage with a sliding door and a restraint bar, built from boxes), and the sounds are vanilla's.

## 4. Multiplayer

Every new player has their own pod, made when *they* first join (the mark is per player, in their own data). Players who join
at the same moment are not stacked: a pod never starts within 5 blocks of another one, the column moves out from the spawn
until it is clear. The pod flattens soft blocks only where its rider may interact (spawn protection and claims hold), and
nobody else can get into a pod: it cannot be clicked, and it takes one rider, the player it was made for.
