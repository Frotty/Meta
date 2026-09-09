# Assets and persistence

Orientation for `de.fatox.meta.assets` and the persistence it backs. Root `AGENTS.md` still applies, including the
trust model — most of what looks like a hole in this code is out of scope on purpose.

## Two XPK formats, both live

Neither replaced the other, and the file names do not make that obvious:

| | v1 — `assets/Xpk*.kt` | v2 — `assets/xpk/` |
| --- | --- | --- |
| Container | 7z with the signature partly overwritten | own format: 64 KB blocks in 1 MB pages |
| Codec | Commons Compress | `java.util.zip.Deflater`, CRC32C per block |
| Reads | whole-archive, solid | positional, one block at a time |
| Entry point | `XPKLoader.open` | `XpkV2Archive.openOrNull` |

v1 is solid-compressed, so reading entry *k* decompresses everything before it. That is why `XPKLoader.open` hands
back one archive to reuse rather than a per-entry accessor — reopening per entry made loading quadratic.

**v2 does nothing without a game-supplied `XpkProfile`.** Meta binds none, on purpose: the keys that open a
particular game's archives are not Meta's to hold, so `MetaAssetProvider` resolves it with `injectOrNull` and simply
has no v2 support when it is absent. A test or a game that forgets to bind one sees packed assets silently missing
rather than an error. `XpkV2Archive.openOrNull` likewise returns `null` for "not mine" rather than throwing, because
a wrong profile and a foreign file are indistinguishable from the outside.

`docs/xpk-format-audit.md` has the format, the measurements behind the block and page sizes, and what was
deliberately left out.

## Persistence (`MetaData.kt`)

**libGDX `Json` writes prototype-relative.** A field equal to the value a default-constructed instance would have is
omitted, so saving an all-defaults object produces `{}`. That is not a bug and not worth "fixing" — but it has a
consequence that is easy to miss: **changing a default silently changes what existing saves mean**, because the old
value was never written down. A player who deliberately chose what is now the old default will find their setting
moved. `PersistedFieldNamesTest` pins the names; nothing can pin the defaults, so changing one is a decision about
existing saves.

**A read can fail without the value being gone.** `MetaData.read` answers `Present`, `Absent`, or `Unreadable`, and
the distinction is the difference between "writing here is free" and "writing here destroys data that is still
intact". `Absent` covers a damaged file only once its bytes are safely quarantined elsewhere.

You do not have to get that right at the call site: `save` refuses to write over a key whose last read failed, and
lifts the refusal once a read succeeds. Three callers had grown their own version of that rule before it moved into
the store — if you find yourself adding a fourth, the guard is already doing it.

`writeAtomically` carries its own table of what a rename-published file has to re-establish. Read it before changing
anything there; each row is a bug that was found separately.
