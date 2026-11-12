# Candidate extensions

Things found in a consuming game that look like they belong here instead: either
because Meta already has most of the machinery and the game is filling a gap, or
because the game solved something generic well enough that a second consumer
would rewrite it.

Sourced from an audit of **BabSky** (`../babs-sky`, a libGDX + Box2D v3 game) in
August 2026, while moving its menus onto `UiControlHelper`. Each entry records
what exists there, why it is generic, and what would have to be decided before it
moves. Nothing here is committed to: it is a list of leads, and a couple of them
are arguments *against* moving.

---

## 1. A menu-shaped focusable row

**In the game:** `MenuList` + `MenuRow`: a vertical list of choices with an
accent marker on the focused row. `MenuRow` implements `MetaFocusable` purely to
become navigable and to suppress `DefaultFocusRenderer`'s ring, and carries one
`ClickListener` so that confirm (which `UiControlHelper` synthesises as a click),
a real click, and a hover all land in one place.

**Why it is generic:** every game with a title screen writes this. The pieces it
needs from Meta already exist; what is missing is the *shape*.

**Why `MetaActionList` was not used:** its rows are tool-style: fixed
COMPACT/COMFORTABLE densities of 32 and 44 logical pixels, `MetaType.BODY` text,
button chrome. Right for an editor panel, wrong for a menu read from a couch,
where a row is 44px of *type* on a 4K display. That is a real gap rather than a
missed reuse, so the extension is a second list widget, not a change to that one.

**Open question:** whether the marker belongs to the widget or to the consumer.
BabSky's is a `SolidRect` in the game's accent colour, tinted from three states
(focused-and-enabled, focused-and-disabled, neither). A Meta version probably
wants that as a skin role.

## 2. Custom focus rendering in headless tests

**What happened:** `MetaFocus.assign` calls `setMetaFocused` only when
`handlesMetaFocus` is true, and `DefaultFocusRenderer` skips a focusable that
handles its own focus. Both correct. But the *callback* only arrives if a
`UIRenderer` relays it, and `MetaHeadlessUi`'s `LayoutOnlyRenderer` implements
`setFocusedActor` as `Unit`.

So a widget that reacts in `setMetaFocused` works at runtime and is silently
inert under test. BabSky hit this and moved its selection onto
`UiControlHelper.focusedActor`: the reactive value, which `setFocusedActor` sets
unconditionally. That is the documented path and it is the right one; the trap is
that the interface makes the other path look equally available.

**Not the suggestion:** having `UiControlHelper` call `MetaFocus.assign` itself
instead of going through the renderer. `MetaUIRenderer` keeps its own private
`focusedActor` field, updated *only* in `setFocusedActor` and read in `draw` to
feed `DefaultFocusRenderer`, so bypassing it would leave ordinary buttons and
select boxes navigable but with no visible focus ring, which is a worse bug than
the one being fixed.

**Done** (the documentation half): `setMetaFocused`'s KDoc now says it is
presentation-only and points at `UiControlHelper.focusedActor` for state. If the
callback should *also* reach a headless consumer, that still needs an API change
keeping the renderer's tracking intact rather than routing around it.

## 3. Input-capable test harness (implemented)

`MetaHeadlessUi.install(input = { DispatchingInput() })` exercises navigation through the shipped test fixture
without claiming `Gdx.input.inputProcessor` or the static `Controllers` registry. The default `LayoutOnlyInput`
accepts registrations and dispatches nothing.

## 3a. Toast test fixture (implemented)

Pass `toastManager = { MetaToastManager(toastStage()) }` to `MetaHeadlessUi.install` to exercise the notification
path. The default renderer throws when a test reaches an unconfigured toast manager, so a missing fixture fails
where the notification would have been added.

## 3b. Per-player UI navigation (implemented)

Per-player helpers and binding profiles support independent keyboard and assigned-controller navigation.
See the [README example](../README.md#runtime-conventions) for usage and the
[design record](local-coop-input.md) for the canonical-key synthesis constraint and the remaining focus-ring work.

## 4. A screen fade / transition primitive

**Done:** `MetaScreenTransition<T>` carries a typed pending destination through fade-out and publishes it through a
reactive `finished` value only once the screen is fully covered. It also owns entry fades, immediate covered holds,
and cancellation without storing a callback or screen reference.

**In the game:** two of them. `Fade` (Kotlin, reactive: phases IDLE/IN/OUT, a
pending intent carried through the fade so the navigation decision is a value
rather than a callback) and `ScreenFade` (Java, imperative). The reactive one is
the good design: `MainMenuScreen` reads `fade.finished()` in one effect and that
is the single place deciding where a completed fade leads.

## 5. A material-routed collision sound system: mostly already here

**In the game:** `CollisionAudioSystem` (~430 lines) hand-rolls voice limiting
(`MAX_ACTIVE = 6`), a global cooldown and a per-pair cooldown, distance
attenuation between `MIN_DISTANCE` and `MAX_DISTANCE`, and round-robin banks
keyed by a material category.

**Already in Meta:** `MetaSoundPlayer` has per-definition `maxInstances`,
`MetaSoundPlaybackPolicy.cooldownMs`, positional volume and pan through
`MetaSoundFalloff`, `randomPitchRange`, and looping. Much of this is the same
class twice.

**But not the voice budget, which is the part that matters most here.**
`MetaSoundPlayer` keeps a handle list per `MetaSoundDefinition` and caps that
list, so N definitions playing at once admit up to `N * maxInstances` voices.
`CollisionAudioSystem.MAX_ACTIVE` is a single global budget of six across every
material. A migration that treats `maxInstances` as the equivalent would let a
pile-up of mixed-material impacts spam the backend. A player-wide cap is a second
prerequisite alongside the playback abstraction below.

**The blocker, and it is real:** BabSky goes through **miniaudio**
(`games.rednblack.miniaudio`) for `MAAttenuationModel`/`MAPositioning`, while
Meta's sound layer sits on libGDX `Sound` behind a pluggable `SoundHandler`
(`NoSoundHandler`, `DesktopSoundHandler`). Moving the game onto
`MetaSoundPlayer` as it stands would trade miniaudio's spatial model for
Meta's falloff maths.

**The interesting option, and it is bigger than it first looked:**
`SoundHandler` is not the seam. It declares exactly one method, `duration(sound)`:
`MetaSoundPlayer` creates its sounds with `Gdx.audio.newSound` and calls
`Sound.play`/`loop` directly, so implementing `SoundHandler` against miniaudio
would leave every byte of playback on libGDX and buy no spatial audio at all.

What the move actually needs is a **sound creation and playback abstraction**
that does not exist yet: something `MetaSoundPlayer` goes through instead of
touching `Gdx.audio` and `Sound`, with the libGDX path as the default
implementation and miniaudio as an optional sibling module. That is the version
where the game deletes 400 lines and Meta gains spatial audio, and it is a
larger piece of work than "write a `SoundHandler`", which is worth knowing
before anyone starts.

**What is genuinely game-specific and should stay:** the material → category
routing (`stone`/`obsidian`/`iron` → `soft`/`metallic`/`glass`) and the
impact-speed thresholds. Those are content decisions.

## 6. A rolling / continuous contact sound

Not a dedup: a gap both projects have. `MetaSoundPlayer` plays one-shots and
loops, but there is no notion of a loop whose gain and filter track a continuous
physical quantity (contact speed, in a rolling game). Any physics game wants it
and it is fiddly enough: fade in and out around the contact starting and
stopping, avoid retriggering per contact event: that it should not be written
twice.

## 7. Preferences: several games' worth of `Gdx.app.getPreferences`

`MetaData` is registered and used for Meta's own state, but BabSky still reaches
for raw `Preferences` in four places (`BabsMain` volume and bindings,
`GpuHintOverlay`, `LevelProgress`). Not a Meta gap: a game not using what is
there. Noted here only because the pattern is likely repeated in other
consumers, and a short "persisting your own settings" note in Meta's docs would
probably fix it everywhere at once.

## 7a. `MetaType` has no responsive counterpart

`MetaType` is fixed pixels: `CAPTION 12`, `BODY 16`, `LABEL 18`, `SUBTITLE 21`,
`TITLE 24`, `HEADING 32`, `DISPLAY 48`, and `MetaResponsive` knows nothing about
type. Nothing joins them.

That is fine for a tool UI read at desk distance and unusable for one read from a
couch. BabSky's display face runs 96px at 1280 and 200px at QHD, four times
`MetaType.DISPLAY` at the top end, so it cannot use the scale at all: `UiScale`
rolls a full ramp (display/heading/item/body/caption/hint) on top of
`MetaResponsiveState`, and every `MetaLabel` in the game is sized from it rather
than from a semantic token.

An audit against Meta's own conventions flags that as a custom-rolled typography
scale bypassing `MetaType`, and it is, but the correction is not "use
`MetaType`", because `MetaType` cannot express it. The gap is Meta's.

**Suggestion:** a responsive `MetaType`: the same semantic roles, resolved per
breakpoint through `MetaResponsiveValue`, with today's constants as the base
tier so nothing changes for existing consumers. A game then overrides the tiers
it cares about and keeps using the role names, instead of inventing a parallel
vocabulary.

## 8. Deliberately **not** candidates

Recorded so the same ground is not re-covered:

- **`UiScale`**: looks like a duplicate of `MetaResponsive` and is not. It is
  already built on `MetaResponsiveState`, `responsive()` and `MetaBreakpoints`.
  The numbers in its ramp are content. See section 7a for the reusable part.
- **`PromptIcons`**: Meta now bundles Kenney glyphs through `MetaInputGlyphs`. Game action-to-glyph mappings belong
  in the consumer.
- **`InputBindings`**: overlaps `MetaUiInputBindings` for six UI actions, and
  the fix was to project one onto the other (`MenuInputBindings` in the game),
  not to move it. The rest of it is move/speed/grip/pause/restart, which are the
  game's verbs.
