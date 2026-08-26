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
that makes rebinding work.

And it is worse than "make synthesis player-aware", which is what this document
first proposed. `MetaInput.keyDown` broadcasts to **every** global processor, so a
redispatch from one cursor reaches all of them: restricting *who* may synthesize
still leaves player one confirming with SPACE redispatching `ENTER`, and player two's
helper seeing a key its own profile legitimately owns. Both act on one press, with no
conflict in the physical bindings at all.

The synthetic event has to be invisible to every *cursor* while staying visible to
`KeyListener`s, which is who synthesis is for. That makes the "am I synthesizing"
guard shared across helpers rather than per-instance.

### 4. Focus is one actor — but this is *not* a prerequisite

Recorded here because the first version of this document treated it as one, and
that was wrong. Checking it changes the cost of the whole feature.

`MetaUIRenderer` keeps one private `focusedActor` and passes it to
`FocusRenderer.draw(stage, focusedActor, deltaTime)`. `MetaFocus.assign` flips one
boolean through `MetaFocusable.setMetaFocused(focused: Boolean)`.

With two cursors an actor can be focused by P1 and not P2 — or by both, mid-hover —
and a boolean cannot express that. Nor can one focus ring distinguish the players.

**But a self-drawing focusable never reaches that code.**
`DefaultFocusRenderer` returns immediately for any actor where
`MetaFocus.isHandledByActor` holds, so a cell that shows its own focus — read
reactively from *its own* helper's `focusedActor`, which is a per-instance signal —
is unaffected by the renderer having one slot. Two helpers do still trample
`MetaUIRenderer`'s single `focusedActor`, and with self-drawing cells that is
invisible.

That is the pattern already in use: `MenuRow` in the consumer is a
`MetaFocusable` with `handlesMetaFocus = true` that tints an accent marker from
`focusedActor`. It would work as a second player's cell today.

Two consequences worth stating:

- `MetaFocus.assign` will call `setMetaFocused(false)` on P1's cell when P2 moves.
  Harmless **because** `setMetaFocused` is presentation-only and widgets are now
  documented not to keep state there. That contract is what makes this safe.
- The trampled `focusedActor` is a shared mutable with two writers. Invisible, not
  correct. A non-primary helper skipping the renderer call would fix it in one line.

So per-player focus *rendering* is a nice-to-have for games whose cells want
Meta's ring, not a gate on local co-op.

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

Empty defaults only prevent a collision *at allocation*. `get` hands back the live
mutable bindings, because that is what makes a rebinding screen possible, so a
later `setKeyboardKeys` can assign a key another profile already holds and both
cursors then move on one press. The registry cannot arbitrate that — which player
keeps a contested key is a game's decision, and silently dropping one player's
binding is the worst available behaviour — so it reports instead:
`conflictingKeys(a, b)`, for a game to call when a profile is committed.

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

`focusFirstIn(root)` confines the spatial search to a group via `focusedRoot`,
which is the affordance that makes this work at all.

**But the confinement is soft, and the first version of this document over-claimed
it.** `possibleTargets` applies `focusedRoot` only while the selected actor is still
inside it — `focusedRoot?.takeIf { selectedActor.isDescendantOf(it) }` — and falls
back to the full parent lineage otherwise. That fallback is deliberate and has a
test named after it (`manual navigation keeps legacy parent lineage search outside
scoped roots`).

Anything that moves a cursor out of its root therefore un-scopes it, and the pointer
is exactly such a thing: `MetaUIRenderer.touchDown` routes every click to the
*singleton* helper's `focusFromPointer`, which takes the nearest navigable ancestor
with no root check. Click a control in player two's panel and player one's cursor is
now on it, able to navigate and confirm player two's controls.

So: **two keyboard cursors in two panels do not contend, and a mouse breaks that.**
For the case this proposal is about — two players at one keyboard — that is
acceptable, and should be stated rather than glossed. For a game mixing a pointer
with per-player cursors it is not, and needs one of:

- `focusFromPointer` rejecting targets outside a set `focusedRoot`. Small, but a
  behaviour change to a shared API: a dialog would also stop losing focus to a click
  behind it, which is arguably a fix and is still a change.
- Explicit ownership routing, so a click goes to the cursor owning the panel it
  landed in rather than always to the singleton.

Both are maintainer decisions, so neither is in slice one.

### Synthesis becomes player-scoped *and* cursor-invisible

Two parts, and the first alone is not enough:

1. Only player one synthesizes, so a second cursor never emits a canonical key.
2. The guard is **shared across helpers**, so the key player one does emit is not
   read as input by any other cursor. A per-instance guard leaves the emitting helper
   skipping it and every other helper processing it.

Consequence to accept deliberately: a `BackAction`-style listener registered on a
canonical keycode hears player one only. That is the right default — a screen-level
"back" is a system action, not a per-player one — but it must be *documented*, not
discovered.

### Focus becomes a set — only if slice 2 is wanted

The first version of this section proposed **replacing** both signatures. That is
wrong twice over, and the corrections are the interesting part.

**`FocusRenderer` must keep its existing entry point.** A downstream consumer can
supply a custom `FocusRenderer`; changing `draw(Stage, Actor?, Float)` breaks it at
source, and an already-compiled one at runtime. Additive instead — the new method
carries a default that delegates to the old one, so an existing renderer keeps
working and sees player one:

```kotlin
interface FocusRenderer : Disposable {
    fun draw(stage: Stage, focusedActor: Actor?, deltaTime: Float)

    /** Default keeps a pre-existing renderer working: it is shown player one and nothing else. */
    fun draw(stage: Stage, focused: MetaFocusSet, deltaTime: Float) =
        draw(stage, focused[MetaPlayer.ONE], deltaTime)
}
```

**A `MetaFocusable` bridge cannot be a simple default in either direction.** This
was the subtler mistake:

- Have `setMetaFocused(Boolean)` forward *to* the player overload, and every
  existing widget — `MetaTextButton`, `MetaCheckBox`, `MetaSlider` and the rest —
  implements only the boolean and leaves the overload unimplemented. Nothing
  restyles.
- Reverse it, so the overload defaults to calling the boolean, and it compiles and
  keeps existing widgets working — but it **collapses both players into one
  boolean.** One player leaving an actor clears the other's focus, and the widget
  cannot tint per player, which was the point.

So the bridge has to carry state, not just delegate. The shape that works:

```kotlin
interface MetaFocusable {
    fun setMetaFocused(focused: Boolean)

    /**
     * Default keeps existing widgets compiling *and* correct: the boolean is driven by whether *any* player holds
     * focus, so a second player arriving does not re-notify and a first player leaving does not clear it while the
     * second is still there. A widget wanting a per-player tint overrides this instead.
     */
    fun setMetaFocused(player: MetaPlayer, focused: Boolean, held: MetaFocusSet) =
        setMetaFocused(held.isNotEmpty())
}
```

That is more surface than "add a parameter", and it is the honest cost of slice 2.
It is also why slice 1 does not attempt it, and why a second cursor is recommended
to be shown by cells reading `focusedActor` themselves.

---

## Suggested slicing

**Slice 1 — foundation, zero behaviour change.** `MetaPlayer`,
`MetaUiInputProfiles`, `UiControlHelper(player, bindings)` filtering by its own
profile, player-scoped synthesis, and a `dispose()` on the helper — the moment a
cursor is a per-screen instance rather than the app-lifetime singleton, its global
input processor has to be removable, or a recreated screen stacks one per
instance and that player's keys drive every one of them.

Single-player behaviour must be byte-identical; the test that matters is that the
existing suite passes untouched. **Implemented in #33.**

**Slice 2 — optional, for games that want Meta's ring per player.** Per-player
focus in `MetaUIRenderer`, `MetaFocusSet`, the `MetaFocusable` overload, per-player
ring tint. **Not** required for the fighting-game select screen: cells that draw
their own focus already bypass the renderer entirely (see blocker 4).

**Slice 3 — controllers.** Pad-to-player assignment in `MetaControllerListener`,
which today merges every device onto one stream on purpose. The raw-button capture
added for `MetaKeyRebindDialog` needs the same player dimension, so a rebind screen
can rebind *player two's* pad.

Slice 1 is the whole feature for the keyboard case, and it is small: a value class,
a profile registry, a player field on `UiControlHelper`, and not synthesising for
non-primary players. Slices 2 and 3 are additions, not completions.

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
