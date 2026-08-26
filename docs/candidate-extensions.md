# Candidate extensions

Things found in a consuming game that look like they belong here instead — either
because Meta already has most of the machinery and the game is filling a gap, or
because the game solved something generic well enough that a second consumer
would rewrite it.

Sourced from an audit of **BabSky** (`../babs-sky`, a libGDX + Box2D v3 game) in
August 2026, while moving its menus onto `UiControlHelper`. Each entry records
what exists there, why it is generic, and what would have to be decided before it
moves. Nothing here is committed to — it is a list of leads, and a couple of them
are arguments *against* moving.

---

## 1. A menu-shaped focusable row

**In the game:** `MenuList` + `MenuRow` — a vertical list of choices with an
accent marker on the focused row. `MenuRow` implements `MetaFocusable` purely to
become navigable and to suppress `DefaultFocusRenderer`'s ring, and carries one
`ClickListener` so that confirm (which `UiControlHelper` synthesises as a click),
a real click, and a hover all land in one place.

**Why it is generic:** every game with a title screen writes this. The pieces it
needs from Meta already exist; what is missing is the *shape*.

**Why `MetaActionList` was not used:** its rows are tool-style — fixed
COMPACT/COMFORTABLE densities of 32 and 44 logical pixels, `MetaType.BODY` text,
button chrome. Right for an editor panel, wrong for a menu read from a couch,
where a row is 44px of *type* on a 4K display. That is a real gap rather than a
missed reuse, so the extension is a second list widget, not a change to that one.

**Open question:** whether the marker belongs to the widget or to the consumer.
BabSky's is a `SolidRect` in the game's accent colour, tinted from three states
(focused-and-enabled, focused-and-disabled, neither). A Meta version probably
wants that as a skin role.

## 2. `MetaFocusable` needs a "no ring, I draw my own" story that does not lie

**What happened:** `MetaFocus.assign` calls `setMetaFocused` only when
`handlesMetaFocus` is true, and `DefaultFocusRenderer` skips a focusable that
handles its own focus. Both correct. But the *callback* only arrives if a
`UIRenderer` relays it, and `MetaHeadlessUi`'s `LayoutOnlyRenderer` implements
`setFocusedActor` as `Unit`.

So a widget that reacts in `setMetaFocused` works at runtime and is silently
inert under test. BabSky hit this and moved its selection onto
`UiControlHelper.focusedActor` — the reactive value, which `setFocusedActor` sets
unconditionally. That is the documented path and it is the right one; the trap is
that the interface makes the other path look equally available.

**Suggestion:** either have `UiControlHelper` drive `MetaFocus.assign` itself
rather than going through the renderer, or say plainly in `MetaFocusable`'s KDoc
that `setMetaFocused` is presentation-only and state belongs on `focusedActor`.
The first removes the trap; the second documents it.

## 3. `LayoutOnlyInput` cannot drive a registered processor

`keyDown`/`keyUp` return `false` without dispatching to the processors added
through `addGlobalInputProcessor`. `UiControlHelper` registers its navigation
listener exactly that way, so no headless test can exercise the spatial
arrow-key step — `navigate`, `getNextX`, `getNextY` — which is the most
interesting logic in the class and the easiest to break with a layout change.

Everything else about that stub is unusually careful (the exclusive-processor
stack is a LIFO on purpose, with a comment explaining what a clobbering stub
would let through). This looks like an oversight rather than a decision.

**Suggestion:** dispatch to the stored `globalProcessors`/`screenProcessors` the
way `MetaInput` does, honouring the exclusive stack. It would let Meta test its
own navigation, not just consumers test theirs.

## 3a. `getToastManager()` throws where a no-op would do

`UIRenderer.getToastManager()` is not optional in the interface but is optional
in practice: `MetaUIRenderer` has one and `MetaHeadlessUi`'s `LayoutOnlyRenderer`
throws `UnsupportedOperationException` with a helpful message. The message is
good. Throwing is the problem.

A toast is a notification *about* an action, so it tends to be the last line of a
handler — "Fullscreen on" after toggling fullscreen. In BabSky that meant one
absent UI layer aborted the settings action halfway through, after the host had
been asked to toggle and before the page could say so. The consumer now wraps
every `toast()` in a try/catch, which is the wrong place for that knowledge.

**Suggestion:** return a no-op toast manager from renderers that have no toast
layer, or make the accessor nullable. A consumer should not have to defend
against a notification.

## 4. A screen fade / transition primitive

**In the game:** two of them. `Fade` (Kotlin, reactive — phases IDLE/IN/OUT, a
pending intent carried through the fade so the navigation decision is a value
rather than a callback) and `ScreenFade` (Java, imperative). The reactive one is
the good design: `MainMenuScreen` reads `fade.finished()` in one effect and that
is the single place deciding where a completed fade leads.

**Why it is generic:** Meta has `UIRenderer.armStartupTransition` for the
startup cover and nothing for screen-to-screen. Any game with more than one
screen needs this.

**Open question:** the intent payload is typed `String?` in BabSky. A Meta
version wants a generic parameter.

## 5. A material-routed collision sound system — mostly already here

**In the game:** `CollisionAudioSystem` (~430 lines) hand-rolls voice limiting
(`MAX_ACTIVE = 6`), a global cooldown and a per-pair cooldown, distance
attenuation between `MIN_DISTANCE` and `MAX_DISTANCE`, and round-robin banks
keyed by a material category.

**Already in Meta:** `MetaSoundPlayer` has per-definition `maxInstances`,
`MetaSoundPlaybackPolicy.cooldownMs`, positional volume and pan through
`MetaSoundFalloff`, `randomPitchRange`, and looping. This is very close to the
same class twice.

**The blocker, and it is real:** BabSky goes through **miniaudio**
(`games.rednblack.miniaudio`) for `MAAttenuationModel`/`MAPositioning`, while
Meta's sound layer sits on libGDX `Sound` behind a pluggable `SoundHandler`
(`NoSoundHandler`, `DesktopSoundHandler`). Moving the game onto
`MetaSoundPlayer` as it stands would trade miniaudio's spatial model for
Meta's falloff maths.

**The interesting option:** a miniaudio-backed `SoundHandler` in Meta (or a
sibling module, to keep the dependency optional). That is the version where the
game deletes 400 lines and Meta gains spatial audio, rather than the game
losing something.

**What is genuinely game-specific and should stay:** the material → category
routing (`stone`/`obsidian`/`iron` → `soft`/`metallic`/`glass`) and the
impact-speed thresholds. Those are content decisions.

## 6. A rolling / continuous contact sound

Not a dedup — a gap both projects have. `MetaSoundPlayer` plays one-shots and
loops, but there is no notion of a loop whose gain and filter track a continuous
physical quantity (contact speed, in a rolling game). Any physics game wants it
and it is fiddly enough — fade in and out around the contact starting and
stopping, avoid retriggering per contact event — that it should not be written
twice.

## 7. Preferences: several games' worth of `Gdx.app.getPreferences`

`MetaData` is registered and used for Meta's own state, but BabSky still reaches
for raw `Preferences` in four places (`BabsMain` volume and bindings,
`GpuHintOverlay`, `LevelProgress`). Not a Meta gap — a game not using what is
there. Noted here only because the pattern is likely repeated in other
consumers, and a short "persisting your own settings" note in Meta's docs would
probably fix it everywhere at once.

## 8. Deliberately **not** candidates

Recorded so the same ground is not re-covered:

- **`UiScale`** — looks like a duplicate of `MetaResponsive` and is not. It is
  already built on `MetaResponsiveState`, `responsive()` and `MetaBreakpoints`;
  what it adds is one game's type ramp (display/heading/item/body/caption/hint,
  and a glyph size for Kenney's input faces). That is content.
- **`PromptIcons`** — Kenney input-prompt faces chosen per `ControllerFamily`.
  Font *files* the game ships, and a mapping from actions to glyphs in them.
  A Meta version would be a licence and asset decision, not a code one.
- **`InputBindings`** — overlaps `MetaUiInputBindings` for six UI actions, and
  the fix was to project one onto the other (`MenuInputBindings` in the game),
  not to move it. The rest of it is move/speed/grip/pause/restart, which are the
  game's verbs.
