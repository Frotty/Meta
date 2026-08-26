# Local co-op UI input — design proposal

**Status:** proposal, not implemented. Opened for a decision on shape before any code.

The goal: two players navigating Meta UI independently on **one machine**, including
**one keyboard** — P1 on the arrows, P2 on WASD — without the game reaching around
Meta to do it.

The concrete case that prompted this is a fighting-game character select: two grids,
two cursors, each player confirming their own. Today that is not expressible.

---

## What blocks it today

Meta's UI input is single-cursor by construction, in four separate places. Each has
to be addressed or explicitly excluded; a change to one alone does nothing.

### 1. Bindings are global

`MetaUiInputBindings` holds one keyboard key set and one controller button set per
`MetaUiAction`. There is one instance, registered as a singleton in `MetaModule`.

Binding P2's WASD to `NAVIGATE_*` alongside P1's arrows does not give two players
two cursors — it gives two players **one** cursor that either can move.

### 2. `UiControlHelper` is a singleton, and five things depend on that

`singleton("default") { UiControlHelper() }`, injected by `MetaSelectBox`,
`MetaUIRenderer`, `MetaDialog`, and two editor windows.

Those consumers are *right* to want one: a modal dialog belongs to one cursor, and
so does an open select-box dropdown. So "make it per player everywhere" is the wrong
shape — it would push a player parameter into every widget that has no opinion about
players.

### 3. Canonical-key synthesis crosses player boundaries

This is the constraint that would derail a naive implementation, so it is worth
being explicit about.

`UiControlHelper.synthesizeCanonicalKeyDown` re-emits the *canonical* key whenever a
bound alias is pressed — that is deliberate, and it is what makes a rebound key and
a pad button both reach a listener registered on one keycode (`BackAction` in a
consumer relies on it).

With two profiles, P2 pressing their confirm synthesises `ENTER`, and **P1's helper
sees `ENTER` as its own confirm.** Per-player isolation is broken by the mechanism
that makes rebinding work. Either synthesis becomes player-tagged, or it is
suppressed once more than one profile is registered — and the second option silently
breaks `BackAction`-style listeners.

### 4. Focus is one actor, and `MetaFocusable` cannot say whose

`MetaUIRenderer` keeps one private `focusedActor` and passes it to
`FocusRenderer.draw(stage, focusedActor, deltaTime)`. `MetaFocus.assign` flips one
boolean through `MetaFocusable.setMetaFocused(focused: Boolean)`.

With two cursors an actor can be focused by P1 and not P2 — or by both, mid-hover —
and a boolean cannot express that. Nor can one focus ring distinguish the players,
which is the whole point of showing it.

---

## Proposed shape

### `MetaPlayer`

```kotlin
@JvmInline
value class MetaPlayer(val index: Int) {
    companion object { val ONE = MetaPlayer(0) }
}
```

A local slot, not a network identity and not a profile. `ONE` is what every existing
consumer gets without asking.

### Per-player binding profiles

`MetaUiInputBindings` stays exactly as it is and becomes **player one's** profile.
A registry holds the rest:

```kotlin
class MetaUiInputProfiles {
    operator fun get(player: MetaPlayer): MetaUiInputBindings
    fun forPlayers(count: Int)           // allocate profiles, defaults on player one only
    val playerCount: Int
}
```

Additional profiles start **empty** rather than defaulted — two players cannot both
have the arrows, and silently copying defaults would produce exactly the shared
cursor this is meant to avoid.

### `UiControlHelper` gains a player, and keeps its singleton

```kotlin
class UiControlHelper(val player: MetaPlayer = MetaPlayer.ONE)
```

Its key listener resolves through `profiles[player]` and **ignores keys it does not
own**. The registered singleton stays player one, so `MetaDialog`, `MetaSelectBox`,
`MetaUIRenderer` and the editor are untouched.

A game wanting a second cursor constructs a second helper and scopes it:

```kotlin
val p2 = UiControlHelper(MetaPlayer(1))
p2.focusFirstIn(rightCharacterGrid)
```

`focusFirstIn(root)` already confines the spatial search to a group via
`focusedRoot`, which is the affordance that makes this work at all — two cursors in
two panels never contend for the same actor.

### Synthesis becomes player-scoped

The narrow fix: synthesise the canonical key **only for player one**, and have
non-primary helpers dispatch their action directly without re-emitting a keycode.

Consequence to accept deliberately: a `BackAction`-style listener registered on a
canonical keycode hears player one only. That is the right default — a screen-level
"back" is a system action, not a per-player one — but it must be *documented*, not
discovered.

### Focus becomes a set

```kotlin
interface MetaFocusable {
    fun setMetaFocused(player: MetaPlayer, focused: Boolean)
}
interface FocusRenderer {
    fun draw(stage: Stage, focused: MetaFocusSet, deltaTime: Float)
}
```

`setMetaFocused(Boolean)` stays as a default that forwards `MetaPlayer.ONE`, so no
existing widget changes. `DefaultFocusRenderer` tints the ring per player index.

---

## Suggested slicing

**Slice 1 — foundation, zero behaviour change.** `MetaPlayer`,
`MetaUiInputProfiles`, `UiControlHelper(player)` filtering by its own profile,
player-scoped synthesis. Single-player behaviour must be byte-identical; the test
that matters is that the existing suite passes untouched.

**Slice 2 — visible second cursor.** Per-player focus in `MetaUIRenderer`,
`MetaFocusSet`, the `MetaFocusable` overload, per-player ring tint. This is where
the fighting-game select screen becomes possible.

**Slice 3 — controllers.** Pad-to-player assignment in `MetaControllerListener`,
which today merges every device onto one stream on purpose. The raw-button capture
added for `MetaKeyRebindDialog` needs the same player dimension, so a rebind screen
can rebind *player two's* pad.

Slice 1 is worth doing even if 2 and 3 never happen: it is what turns "Meta assumes
one cursor" from an unwritten assumption into a stated one with a seam.

---

## Open questions for the maintainer

1. **Does a dialog belong to a player?** A modal opened by P2 mid-select — does it
   capture only P2's input, or all of it? The exclusive-processor stack has no player
   dimension, and adding one is a bigger change than any of the above.
2. **Is player one privileged?** The synthesis proposal makes it so. The alternative
   is no synthesis at all for anyone, which is cleaner in theory and breaks the
   rebinding path that makes canonical keys useful.
3. **Should `MetaUiInputProfiles` persist per player?** `MetaUiInputProfilePersistence`
   saves one profile today. Two players implies two saved profiles and a decision
   about what a fresh second player starts with.
