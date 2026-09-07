# XPK format audit

Audit of the XPK game-data archive as implemented in `runtime/src/main/kotlin/de/fatox/meta/assets/`, against four
goals: resistance to casual extraction, fast decompression at acceptable ratio, non-blocking indexed lazy loading, and
resistance to modification given that Meta itself is public.

Measurements in this document were taken on the audit machine (Temurin 25.0.3, Windows 11) against this repository's
own `assets/` folder and against synthetic corpora shaped like game content. They are order-of-magnitude evidence for
the design decisions, not a benchmark suite; re-measure on real game data before acting on the ratio numbers.

## 1. What XPK is today

A `.xpk` file is:

```
[ 7z archive, bytes 0..5 of the signature overwritten with junk ] [ 8-byte little-endian XXH64 of everything above ]
```

The reader is three classes:

| File | Role |
| --- | --- |
| [XPKLoader.kt](runtime/src/main/kotlin/de/fatox/meta/assets/XPKLoader.kt) | Reads the whole file, verifies the trailer, restores the 7z signature, enumerates entries, reads one entry |
| [XPKByteChannel.kt](runtime/src/main/kotlin/de/fatox/meta/assets/XPKByteChannel.kt) | `SeekableByteChannel` over the in-heap archive bytes |
| [XPKFileHandle.kt](runtime/src/main/kotlin/de/fatox/meta/assets/XPKFileHandle.kt) | libGDX `FileHandle` per entry, decompressing lazily on first `readBytes()` |

[MetaAssetProvider.loadPackedAssetsFromFolder](runtime/src/main/kotlin/de/fatox/meta/assets/MetaAssetProvider.kt:80)
indexes every `.xpk` in a folder by calling `getList` and caching one handle per entry under a case-folded path key.

There is no writer. `SevenZOutputFile` appears only in
[XPKLoaderTest.kt:22](runtime/src/test/kotlin/de/fatox/meta/assets/XPKLoaderTest.kt:22), so the format is defined
solely by what the reader happens to accept.

## 2. Findings

### Critical

**C1 — Entry reads are O(N²).**
[XPKLoader.loadEntry](runtime/src/main/kotlin/de/fatox/meta/assets/XPKLoader.kt:84) constructs a *new* `SevenZFile`
per entry, seeks to 0, and calls `nextEntry` until the name matches. Every read therefore re-parses the archive header
and, because 7z compresses solid by default, re-decompresses every preceding entry in the same block. Measured against
synthetic game-shaped corpora, versus a single sequential pass over the same archive:

| Entries | Raw | 7z (LZMA2) | One sequential pass | Current `loadEntry` pattern | Penalty |
| --- | --- | --- | --- | --- | --- |
| 32 | 809 KB | 765 KB | 18.3 ms | 267.2 ms | **14.6×** |
| 128 | 3.0 MB | 2.9 MB | 66.4 ms | 4060.8 ms | **61.1×** |
| 512 | 12.0 MB | 11.4 MB | 261.1 ms | 61 883.7 ms | **237.0×** |

The penalty grows superlinearly with entry count: reading 512 entries out of a 12 MB archive takes **62 seconds**
where one sequential pass takes 261 ms. This is the single largest load-time defect in the format, and it gets worse
the more the archive is used as intended.

**C2 — `SevenZFile` is never closed.**
Neither [`getList`](runtime/src/main/kotlin/de/fatox/meta/assets/XPKLoader.kt:34) nor
[`loadEntry`](runtime/src/main/kotlin/de/fatox/meta/assets/XPKLoader.kt:86) closes the reader. Each instance holds
LZMA2 decoder state and a dictionary buffer (tens of MB at high presets) until GC collects it. Combined with C1 that
is one abandoned decoder per asset read.

**C3 — `XPKFileHandle.array` is published unsafely across threads.**
[XPKFileHandle.kt:18](runtime/src/main/kotlin/de/fatox/meta/assets/XPKFileHandle.kt:18) uses
`LazyThreadSafetyMode.NONE`, so the backing field is neither `volatile` nor guarded. It is read from at least two
threads:

- AssetManager's worker, via
  [MetaTextureLoader.loadAsync](runtime/src/main/kotlin/de/fatox/meta/assets/MetaTextureLoader.kt:38) →
  `TextureData.Factory.loadFromFile` → `readBytes()`, and via
  [MetaTextureAtlasLoader.getDependencies](runtime/src/main/kotlin/de/fatox/meta/assets/MetaTextureAtlasLoader.kt:32),
  which parses the atlas text on the worker by design.
- The GL thread, via [`getResource(..., FileHandle::class)`](runtime/src/main/kotlin/de/fatox/meta/assets/MetaAssetProvider.kt:249)
  handed to e.g. [MetaSoundSource.kt:100](runtime/src/main/kotlin/de/fatox/meta/sound/MetaSoundSource.kt:100).

The visible failures are a stale/null read on the second thread and a duplicated (expensive) decode. The
`synchronized(file)` inside `loadEntry` protects the channel, not the memoised reference.

**C4 — `parent()` and `child()` fabricate handles that alias the wrong entry's bytes.**
[`parent()`](runtime/src/main/kotlin/de/fatox/meta/assets/XPKFileHandle.kt:27) and the
[`child()` miss path](runtime/src/main/kotlin/de/fatox/meta/assets/XPKFileHandle.kt:62) construct a new handle
carrying **this** entry's `entryName` and `entrySize`. So a lookup that should fail instead returns a handle where
`exists()` is `true`, `length()` is `0`, and `readBytes()` returns *a different entry's* payload. Atlas page
resolution runs through exactly this path: `TextureAtlasData(atlasFile, atlasFile.parent(), flip)` resolves each page
with `child(pageName)`. A page-name mismatch feeds atlas text to `Pixmap` instead of reporting a missing file.

### Medium

**M1 — The whole compressed archive is pinned in heap, and startup is proportional to archive size.**
[`readAndVerify`](runtime/src/main/kotlin/de/fatox/meta/assets/XPKLoader.kt:57) reads and hashes the entire file
before a single entry name is available, requires it to fit in an `int` (hard 2 GB cap), and hands the array to
`XPKByteChannel`, which holds it for as long as any handle survives. A 500 MB pack costs a full read plus a full hash
before the first asset byte, even if three files are wanted.

**M2 — Decompressed entries are cached forever with no eviction.** Same line as C3. After loading, both the
compressed archive and every decompressed asset are live. Sounds are resident twice: raw Ogg bytes memoised on the
handle, decoded PCM in OpenAL.

**M3 — `Thread.yield()` is not asynchrony.** Calls at
[XPKLoader.kt:50](runtime/src/main/kotlin/de/fatox/meta/assets/XPKLoader.kt:50),
[:76](runtime/src/main/kotlin/de/fatox/meta/assets/XPKLoader.kt:76),
[:104](runtime/src/main/kotlin/de/fatox/meta/assets/XPKLoader.kt:104) and
[:112](runtime/src/main/kotlin/de/fatox/meta/assets/XPKLoader.kt:112) bound no latency, guarantee nothing about
scheduling, and add a scheduler hint per 64 KB. The work is still fully synchronous on whichever thread called in.

**M4 — The XXH64 trailer is a corruption check with zero tamper resistance.** It is unkeyed, non-cryptographic, and
stored inside the file it protects, so anyone can recompute it. The failure message even prints the expected value
([HashUtils.kt:52](runtime/src/main/kotlin/de/fatox/meta/api/crypto/HashUtils.kt:52)). That is fine as an integrity
check and should not be mistaken for protection — see §3.

**M5 — The obfuscation is six bytes deep.** The 20-byte 7z start header at offsets 12..31 (next-header offset, size
and CRC32) survives untouched, as does the entire end-of-archive header. Writing `37 7A BC AF 27 1C` over the first
six bytes and dropping the last eight opens the file in 7-Zip. A carver that keys on the start-header structure rather
than the magic finds it as-is.

**M6 — There is no index.** Lookup is a case-sensitive linear string comparison
([XPKLoader.kt:91](runtime/src/main/kotlin/de/fatox/meta/assets/XPKLoader.kt:91)) against an enumeration that itself
requires decoder state, while every other asset path in the system is case-folded
([AssetPath.kt:9](runtime/src/main/kotlin/de/fatox/meta/assets/AssetPath.kt:9)).

**M7 — There is no streaming read.** [`read()`](runtime/src/main/kotlin/de/fatox/meta/assets/XPKFileHandle.kt:37)
materialises the whole entry and then wraps it in a stream, and `readBytes()!!` throws NPE on a miss rather than
reporting a missing file.

**M8 — The format is unversioned.** No own magic, no version field, no entry count, no per-entry checksum. XPK v1 and
a future v2 are indistinguishable except by file extension, and integrity can only be checked all-or-nothing.

**M9 — Path separators round-trip pointlessly.** 7z stores `/`;
[XPKLoader.kt:44](runtime/src/main/kotlin/de/fatox/meta/assets/XPKLoader.kt:44) converts to `\`; `assetPathKey`
converts back; and
[MetaTextureAtlasLoader.kt:66](runtime/src/main/kotlin/de/fatox/meta/assets/MetaTextureAtlasLoader.kt:66) carries a
`replace('\\', '/')` to undo it for AssetManager keys.

**M10 — No packer exists in Meta.** See §1. Nothing enforces which 7z coders, solid-block layout, or entry ordering
the reader will actually meet.

### Low

- **L1** — `XPKByteChannel` carries a live `write`/`truncate`/`resize` path
  ([:64](runtime/src/main/kotlin/de/fatox/meta/assets/XPKByteChannel.kt:64),
  [:100](runtime/src/main/kotlin/de/fatox/meta/assets/XPKByteChannel.kt:100),
  [:135](runtime/src/main/kotlin/de/fatox/meta/assets/XPKByteChannel.kt:135)) on a read-only archive — dead mutable
  code inherited from Commons Compress' `SeekableInMemoryByteChannel`, in a class documented `@NotThreadSafe`
  ([:18](runtime/src/main/kotlin/de/fatox/meta/assets/XPKByteChannel.kt:18)) that is shared by every handle.
- **L2** — The default constructor yields `size == -8`
  ([:173](runtime/src/main/kotlin/de/fatox/meta/assets/XPKByteChannel.kt:173)).
- **L3** — `SeekableByteChannel.hash()` allocates a full-size heap buffer, truncates `size()` to `Int`, and honours
  only one `read` call ([HashUtils.kt:26](runtime/src/main/kotlin/de/fatox/meta/api/crypto/HashUtils.kt:26)).
- **L4** — `XXH64(ByteBuffer, …)` mutates the caller's position, limit and byte order, restoring only the limit
  ([XxHash64.kt:13](runtime/src/main/kotlin/de/fatox/meta/api/crypto/XxHash64.kt:13)).
- **L5** — `length` is a `var` never reassigned; `sibling`/`child` return nullable into libGDX's non-null contract;
  `exists()` is unconditionally `true`; `type()`, `isDirectory` and `list()` are not overridden, so the fallback in
  [`read()`](runtime/src/main/kotlin/de/fatox/meta/assets/XPKFileHandle.kt:39) inspects a null `file`.
- **L6** — `archive.isDirectory` is never consulted
  ([XPKLoader.kt:38](runtime/src/main/kotlin/de/fatox/meta/assets/XPKLoader.kt:38)), so directory entries would be
  indexed as zero-length files.
- **L7** — `listEntryNames` re-reads and re-hashes the whole archive instead of reusing an open reader
  ([XPKLoader.kt:14](runtime/src/main/kotlin/de/fatox/meta/assets/XPKLoader.kt:14)).

## 3. Focus 1 — obfuscation

### Assessment

The current scheme raises the cost of extraction from *seconds* to *seconds*. Six damaged bytes defeat a magic-byte
scanner and nothing else. Filenames, directory structure, entry sizes and the whole coder configuration remain in the
7z header, and the header itself is at a predictable place with a predictable shape.

State the ceiling plainly, because it shapes every decision below: **a client that can decrypt its own assets ships
the means to decrypt them.** No amount of client-side work makes extraction impossible. The achievable goal is to
raise the cost from "hex-edit six bytes" to "reverse-engineer a custom container and recover a key from a running
process" — which is a real and worthwhile difference, and is where every shipped game archive format sits.

### Recommendation

1. **Encrypt the whole payload with a seekable keystream.** AES-CTR (`AES/CTR/NoPadding`, hardware-accelerated) or
   ChaCha20 — both verified present on the project's Temurin 25 toolchain. CTR/stream mode is the essential property:
   the keystream at any offset is computable from the block index, so random access survives encryption. Counter =
   `f(archiveSalt, blockIndex)`.
2. **Leave no plaintext structure.** No magic, no readable version field. The file starts with a 16-byte random salt
   (indistinguishable from noise) and everything after it — including the table of contents — is ciphertext. Whole-file
   entropy is then uniform and every carver, `binwalk` signature and "media extract" tool finds nothing. Identify the
   format by successfully authenticating after decryption, not by a header probe.
3. **Store only name *hashes* in the shipped TOC.** This is the highest-value, lowest-cost obfuscation available.
   Replace path strings with `xxh3(normalisedPath, gameSeed)`. Even an attacker who fully breaks the encryption gets
   4096 nameless blobs with no filenames, no extensions and no directory tree. Lookup by 64-bit hash is faster than
   string comparison anyway, so this *helps* §5. Emit a plaintext `name → hash` manifest as a build artifact for
   debugging, and never ship it.
4. **Keep XXH64/XXH3 for integrity, per block, not per file.** Cheap, incremental, parallel, and it tells you which
   block is bad. Drop the single whole-file trailer.
5. **Add an Ed25519 signature over the TOC** (verified available on the toolchain). This is the one part of the scheme
   that is cryptographically real rather than obfuscation: with the private key in CI only, nobody can *author* an
   archive the unmodified game accepts, even knowing the entire format and the symmetric key. See §6.

Explicitly do not: invent a cipher, rely on the source being closed, or gate loading behind a single boolean check
that can be patched out — decryption must be *structurally* dependent on the secret, so a wrong key yields garbage
rather than a failed `if`.

## 4. Focus 2 — compression

### Measured

This repository's own `assets/` folder (38 files, 2 051 302 bytes — png, ttf, vert/frag/glsl, g3db/g3dj, mdx, xml,
properties), one solid stream per codec, best of 15 timed runs after warmup:

| Codec | Packed | Ratio | Decompress | Throughput |
| --- | --- | --- | --- | --- |
| LZMA2 / xz preset 9 *(current class of codec)* | 777 152 | 2.64× | 55.5 ms | **37 MB/s** |
| LZMA2 / xz preset 6 | 777 152 | 2.64× | 51.6 ms | 40 MB/s |
| **Deflate level 9** (`java.util.zip`, native zlib) | 950 253 | 2.16× | **6.8 ms** | **301 MB/s** |
| LZ4 frame, Commons Compress pure Java | 1 094 911 | 1.87× | 48.2 ms | 43 MB/s |

Two results matter:

- **LZMA2 decompresses at ~37 MB/s here.** That is the codec 7z uses by default and it is the wrong end of the
  ratio/speed curve for a format whose stated goal is fast loading. Deflate gives up 18% on size to decode **7.4×
  faster**.
- **Commons Compress' pure-Java LZ4 is not the fast option.** 43 MB/s at a *worse* ratio than Deflate — no better than
  LZMA2 on speed and much worse on size. It also drags in `commons-codec` (for `XXHash32`) and `commons-lang3`, neither
  of which `runtime` currently declares. Rule it out.

`java.util.zip` wins here because it is native zlib in the JDK, with no new dependency and no native to ship.

The same 512-entry synthetic corpus from C1, repacked per entry with Deflate and stored raw where compression did not
pay, against the 7z/LZMA2 archive read through the current access pattern:

| Layout | Packed | Read all 512 | Read one arbitrary entry |
| --- | --- | --- | --- |
| 7z / LZMA2, solid, current `loadEntry` | 11 694 KB | 61 883.7 ms | up to a full solid-block decode |
| Per-entry Deflate + store-raw | **11 666 KB** | **3.7 ms** | **0.006 ms** |

The ratio parity here is an artefact of that corpus being two-thirds pseudo-random bytes — but that is exactly the
shape of a PNG- and OGG-heavy game archive, and it is the argument for the store-raw rule: LZMA2 spent 62 seconds to
end up 28 KB *larger* than not compressing the incompressible parts at all. On genuinely compressible content the real
gap is the `assets/` table above (2.16× vs 2.64×), not this one.

### Recommendation

1. **Compress per entry (or per small frame), never solid across the archive.** Solid blocks are the direct cause of
   C1 and are fundamentally incompatible with random lazy access. If ratio on many tiny files matters, group entries
   into *bundles* that are loaded together (one screen, one level) and make the bundle the solid unit — a block decode
   then serves a whole load phase instead of one file.
2. **Default codec: Deflate (JDK zlib).** Zero new dependencies, 301 MB/s measured, ratio within 18% of LZMA2 on real
   asset data. For archives in the hundreds of MB at most, that trade is clearly right given the stated priority.
3. **Store raw when compression does not pay.** Unconditionally store `.png .jpg .webp .ogg .mp3 .ktx2 .basis .zst`
   uncompressed, and fall back to raw for any entry where compressed size ≥ 97% of input. Costs nothing in size and
   removes the decode entirely for the bulk of a game's bytes.
4. **Consider Zstandard only if archive size is measured to matter.** Zstd would plausibly recover most of the ratio
   gap at Deflate-class-or-better speed, but it needs `zstd-jni` — a JNI native per platform, plus widening the
   `mavenCentral` content filter in [build.gradle:76](build.gradle:76), which currently allowlists specific groups.
   **These numbers were not measured here** (no zstd artifact was available offline); measure before adopting. A
   trained zstd dictionary over the small text-ish entries (shaders, `.atlas`, JSON, `.fnt`) is the part most likely
   to justify the dependency.
5. **The larger load-time win is upstream of the archive.**
   [MetaTextureLoader.loadAsync](runtime/src/main/kotlin/de/fatox/meta/assets/MetaTextureLoader.kt:38) decodes PNG to
   RGBA and builds the mip chain on the CPU at load time. For a texture-heavy game that cost usually dominates archive
   decompression outright. Pre-transcoding textures to KTX2/Basis or to raw premultiplied RGBA with prebuilt mips at
   pack time turns `loadAsync` into a copy and leaves `StagedTextureUploads` only the upload. Raw RGBA compresses well,
   so archive size stays reasonable. Worth sequencing before any codec change.
6. **Block size: 64–256 KB, not MPQ's 4 KB.** Blocking costs ratio only while blocks are small, and the penalty is
   gone well before the sizes that hurt latency. Measured on `assets/`, against a solid Deflate stream as the ceiling
   (2.159×):

   | Block | Packed | Ratio | % of ceiling | Decode all | Decode one block |
   | --- | --- | --- | --- | --- | --- |
   | 4 KB *(MPQ's sector size)* | 1 054 907 | 1.945× | 90.1% | 11.33 ms | 0.017 ms |
   | 16 KB | 995 430 | 2.061× | 95.5% | 7.84 ms | 0.047 ms |
   | **64 KB** | 963 347 | 2.129× | **98.6%** | 5.61 ms | 0.221 ms |
   | **256 KB** | 952 931 | 2.153× | **99.7%** | 5.69 ms | 0.795 ms |
   | 1 MB | 950 219 | 2.159× | 100.0% | 5.34 ms | 3.079 ms |
   | 4 MB | 950 253 | 2.159× | 100.0% | 5.84 ms | 4.642 ms |

   4 KB gives up 10% of the achievable ratio — MPQ's number was sized for 1990s disk and CD-ROM sectors and should not
   be inherited. Past 256 KB the ratio is exhausted while per-block decode latency keeps growing linearly, which is
   the cost paid on every random access. **A shared dictionary does not rescue small blocks**: priming a 32 KB
   dictionary lifted 4 KB blocks only from 1.945× to 1.981× (91.8% of ceiling), so use bigger blocks rather than a
   dictionary for this purpose. Keep the block size a profile parameter; **§9 settles the default at 64 KB**, because
   the 1 MB page padding that keeps Steam deltas from cascading costs ~3% at 64 KB and ~6% at 128 KB.

## 5. Focus 3 — indexing, lazy loading and async

### Recommendation

**Index.** Put an explicit table of contents in the container: `nameHash (u64) → blockIndex, offsetInBlock, rawSize,
storedSize, codec, checksum`, sorted by `nameHash`. On open, read and decrypt just the TOC (kilobytes), then look up
with a binary search over a `long[]` — allocation-free, cache-friendly, ~20 ns, and it replaces both the linear scan
(M6) and the decoder-state-dependent enumeration.

**File access.** Prefer **positional `FileChannel.read(ByteBuffer, position)`** for the payload: it is thread-safe,
does not touch channel position, needs no locking, and lets the OS page cache do its job. `FileChannel.map` into a
`MemorySegment` with an `Arena` (verified working on the toolchain) removes the 2 GB cap and the heap copy, but two
things argue against making it the default:

- With a keystream you must decrypt into a scratch buffer anyway, so the zero-copy benefit of a read-only mapping is
  largely spent.
- On Windows a mapping holds the file, which interferes with patching or updating the archive while the game runs.

Where FFM genuinely earns its place is the **destination** side: allocate decode buffers from an `Arena` as direct
`MemorySegment`s so a native decoder receives them without a JNI copy and they are freed deterministically instead of
waiting on `DirectByteBuffer` cleanup.

**Threading.** Two pools, deliberately separated:

- **I/O** — virtual threads (`Executors.newVirtualThreadPerTaskExecutor()`, verified available). A blocking positional
  read parks the virtual thread without pinning a carrier, which is exactly what the `Thread.yield()` sprinkling
  (M3) was imitating badly. Delete every `Thread.yield()`.
- **Decode** — a small *platform*-thread pool sized to cores. This distinction matters: decompression is CPU-bound,
  and a native (JNI) decode call pins its carrier thread, so running decode on virtual threads starves the I/O pool.

**Delivery.** Results must land on the GL thread through the existing frame-budgeted pump, not by calling into
reactive state from a worker — `AGENTS.md` makes reactive writes GL-thread-only, and
[MetaAssetProvider.update](runtime/src/main/kotlin/de/fatox/meta/assets/MetaAssetProvider.kt:211) with
`pendingFinalization`/`StagedTextureUploads` is already the right shape. Keep it; give the reader a
`CompletableFuture<MemorySegment>`-style API and drain completions in `update`.

**Lifetime.** Decompressed bytes must be a *transient* buffer released once the loader consumes them, not a permanent
memo (M2). Return a pooled/`Arena`-scoped buffer with explicit release, and let a genuinely hot small asset opt into
caching rather than caching everything by default.

**Handle contract.** Fix `XPKFileHandle` to normalise on `/` throughout (M9), return a non-null missing-file handle
whose `exists()` is `false` instead of aliasing another entry (C4), override `type()`, `isDirectory`, `list()`,
`readString` and the `readBytes(byte[], int, int)` overloads, and make `read()` a genuinely lazy decoding stream (M7)
so streamed audio does not have to be fully resident.

## 6. Focus 4 — hardening when Meta itself is public

The question is whether a public Meta undermines the scheme. It does not have to, provided the split is drawn in the
right place: **Meta ships the mechanism; the game ships the parameters.**

### Design

Introduce an `XpkProfile` that carries everything an attacker would need beyond the algorithm:

| Parameter | Why it must be game-side |
| --- | --- |
| Root key (32 B) | Obvious |
| Name-hash seed | Without it, recovered hashes cannot even be dictionary-attacked against guessed asset names |
| Keystream nonce derivation | `nonce = f(archiveSalt, blockIndex)`; a game-supplied `f` breaks generic decryptors |
| Block-placement permutation seed | Blocks are stored in a permuted order, so a recovered key alone does not let an attacker carve blocks sequentially without the TOC |
| Codec id table | Which numeric id means which codec — tiny, and meaningfully annoying to guess |
| Footer layout | Field order, plus an obfuscation of the footer's own bytes derived from file length |
| TOC signing public key | See below |

Then:

1. **Meta registers no default profile.** `XpkReader` has no no-arg constructor,
   [MetaModule](runtime/src/main/kotlin/de/fatox/meta/MetaModule.kt) binds no `XpkProfile`, and
   `loadPackedAssetsFromFolder` returns `false` when none is bound. This fits how the graph is already wired —
   `AssetProvider` itself is bound downstream, not in Meta
   ([MetaEditorModule.kt:25](editor/src/main/kotlin/de/fatox/meta/modules/MetaEditorModule.kt:25)), so the profile
   travels the same route. A game must supply its own. Someone who
   clones Meta and points it at your `.xpk` has the algorithm and no constants — the OSS repo tells them the *shape*
   of the container and nothing about this game's instance of it. Keep an obviously-fake profile in `testFixtures` so
   Meta's own tests still exercise the reader.
2. **Ship the packer the same way.** `XpkWriter(profile)` in Meta; the profile — and therefore the key — lives in the
   game repo, with the signing key in CI secrets only. This also closes M10.
3. **Never store key material as a literal.** A Kotlin `const val` string lands in the constant pool and is
   `strings`-able. Derive it at class-init from several unrelated fragments so no contiguous 32 bytes of key exist in
   the jar.
4. **Bind the key to the game binary.** Derive the root key as `HKDF(xxh3(bytes of the game's own primary jar) ‖
   profileConstant)`. Patching *anything* in the game jar then breaks asset decryption as a structural consequence
   rather than as a check that can be NOP'd out — which is precisely the "less easily modified" property asked for.
   Two caveats to plan for: it also breaks legitimate patching, so re-pack archives per build; and you need a
   development bypass (e.g. a profile variant selected by a system property) or local iteration becomes painful.
5. **Sign the TOC with Ed25519.** This is the only cryptographically strong lever available here. With the private key
   in CI, a third party cannot produce an archive that an unmodified game accepts — knowing the format and the
   symmetric key does not help them. They can still patch the game to skip verification, but that is a different and
   much higher cost class than editing an archive, and it is detectable by (4).

### Honest limits

Everything in (1)–(4) is cost-raising, not security. The key is materialised in the process, so a debugger, a JVMTI
agent or a `javax.crypto` hook recovers it, and Kotlin bytecode decompiles cleanly. The realistic outcome is that
casual extraction and drive-by asset swapping stop working entirely, and a determined reverse engineer spends hours
instead of seconds. Only the Ed25519 signature (5) resists someone who fully understands the format. Budget effort
accordingly and do not build features that depend on the archive being unreadable.

## 7. Proposed XPK v2 container

```
offset 0     : salt[16]                       stable per archive, NOT per build (see §9)
offset 16    : ciphertext                     AES-CTR; per-block nonce derived from the block's content hash
                 payload pages                1 MB, 1 MB-aligned; blocks never straddle a page (§9)
                   payload blocks             64 KB default, content-addressed,
                                              stably ordered, independently decodable
                 table of contents            sorted by nameHash
                   entry: nameHash u64, contentKey u64, blockIndex u32, offsetInBlock u32,
                          rawSize u32, storedSize u32, codec u8, flags u8
                 block table                  blockIndex -> fileOffset u64, storedSize u32, contentHash u64
                 bundle table                 bundleId -> block range, for load-group prefetch
offset len-64: footer                         obfuscated per profile
                 tocOffset u64, tocLength u32, blockCount u32, entryCount u32,
                 formatVersion u16, profileId u16, tocChecksum u64,
                 signature[64]                Ed25519 over the TOC
```

Two changes from the first sketch, both driven by §9:

- **Entries carry a `contentKey`** (hash of the entry's bytes) as well as a `nameHash`. Identical payloads then
  occupy one block — free dedup across level variants and reskins — and the block table becomes the only thing that
  moves when content is repacked.
- **The keystream nonce comes from block content, not from a per-build salt.** An unchanged block therefore encrypts
  to identical ciphertext across builds, which is what keeps delta patches small. This leaks block-level plaintext
  equality, which for game assets is not a concern.

Properties this buys, mapped to the goals: uniform entropy and no magic (§3.1–2); nameless entries (§3.3);
independently decodable, randomly accessible blocks (§4.1, C1); a small contiguous index read at open (§5, M1, M6);
per-block integrity (M4, M8); an explicit version and profile id (M8); a signature a game-side public key alone can
verify (§6.5); and byte-stable output for unchanged content (§9).

## 8. Protection decisions

### The cipher is free; a custom one is negative value

"A known cipher is detectable" conflates the file with the client. **Ciphertext carries no tell** — AES-CTR output is
indistinguishable from random, with no magic, header or statistical signature. What is detectable is the *client*:
`Cipher.getInstance("AES/CTR/NoPadding")` is a string in the constant pool. So the axis is not standard-versus-novel
cipher; it is **standard primitive with a custom protocol**.

Speed does not argue for a custom cipher either. 32 MiB, best of 10 after warmup:

| Construction | Time | Throughput | Note |
| --- | --- | --- | --- |
| `System.arraycopy` (floor) | 4.5 ms | 7 510 MB/s | no work at all |
| **AES-256/CTR, 64 KB blocks via `update()`** | **5.6 ms** | **5 986 MB/s** | AES-NI intrinsic engages |
| xoshiro256\*\* keystream XOR, hand-rolled | 6.3 ms | 5 331 MB/s | no security analysis |
| ChaCha20, single `doFinal` | 9.8 ms | 3 412 MB/s | software, constant-time |
| AES-256/CTR, single 32 MiB `doFinal` | 85.1 ms | 394 MB/s | misses the intrinsic |
| Deflate decompress | — | 301 MB/s | the actual bottleneck |
| LZMA2 decompress | — | 37 MB/s | current codec |

AES-CTR fed 64 KB blocks is **faster than the hand-rolled keystream** and ~20× faster than the decompressor behind
it. A novel cipher would be slower, weaker and more work. Note the trap: one giant `doFinal` runs 15× slower than the
same cipher fed 64 KB chunks — and block-granular is the call shape this design uses anyway. **Decision: AES-256/CTR,
one reused `Cipher`, driven per block.**

### Name hashes: keyed and fast, not slow

Unkeyed-and-fast is empirically insufficient, and MPQ is the evidence: Blizzard stored only hashes and withheld
listfiles, and communities recovered large fractions of the filenames from wordlists, because asset names are English
words with a handful of known extensions.

| Option | Speed | Offline brute force | Verdict |
| --- | --- | --- | --- |
| xxh3 / MPQ-style, unkeyed | ~GB/s | Feasible — small, guessable namespace | No |
| Slow KDF (Argon2, scrypt) | ~ms per name | Infeasible | Unnecessary |
| **SipHash-2-4 or truncated HMAC, secret-keyed** | ~GB/s | Infeasible without the key | **Yes** |

Keyed wins because the key ships in the client regardless: against a jar-reader it is no worse than anything else,
and against someone holding only the `.xpk` it is a wall. The slow-KDF option is moot once asset keys are generated
as **build-time constants**, so the runtime never hashes a string at all. Use 64-bit keys (collision odds at 10 000
entries ≈ 3 × 10⁻¹²) and fail the pack step on any collision rather than trusting the math. SipHash is not in the JDK
but is ~40 lines of a published specification, which is not the same thing as inventing a primitive.

### What to take from MPQ and CASC, and what not to

| Mechanism | Source | Take it? |
| --- | --- | --- |
| Sectored per-file compression, independently decodable | MPQ | **Yes** — the direct fix for C1 |
| 4 KB sector size | MPQ | **No** — costs 10% of ratio; sized for 1990s media. See §4.6 |
| Names never stored, hash table only | MPQ | **Yes**, but keyed — their listfile history is the argument |
| Per-file key derived from the filename | MPQ | **Yes** — you need the name to decrypt, and the name is not in the file |
| Content addressing (files keyed by hash of content) | CASC | **Yes** — free dedup, and it makes §9 work |
| Index separate from payload blobs | CASC | **Yes** — a patch rewrites indices, not the whole archive |
| Fixed-size archive blobs + local indices | CASC | **Partly** — worth it once archives exceed ~1 GB |

### Ideas compared

| Idea | Stops | Broken by | Build cost | Runtime cost | Verdict |
| --- | --- | --- | --- | --- | --- |
| Current: 6 patched bytes | magic-byte scanners | hex editor, 30 s | — | none | **Insufficient** |
| AES-CTR payload, no plaintext structure | every carver, all casual extraction | reading the jar | Low | ~free | **Do it** |
| Keyed name hashes only | name recovery, triage of dumped blobs | key recovery from jar | Low | negative (faster) | **Do it** |
| Custom protocol over AES (stable permutation, game nonce derivation) | a generic extractor built from this public repo | targeted RE of one game | Low | none | **Do it** |
| Ed25519 signature over the TOC | third parties *authoring* valid archives | patching the client to skip the check | Low | µs, once | **Do it** — only strong lever |
| Key derived from game-jar hash | archive reuse after any binary patch | debugger at the derivation site | Medium | one hash at boot | **Yes, with a dev bypass** |
| Key derivation in native code via FFM | bytecode-level key extraction | native RE, or a `Cipher` hook | Medium | none | **Yes** — best value per unit of pain |
| Novel cipher instead of AES | nothing extra | reading the jar | Medium–High | *slower* than AES | **No** |
| Encrypted class loading from XPK | `unzip` + decompiler on game logic | a `ClassFileTransformer` dumping at `defineClass` | High | startup, and the AOT cache | **No** |
| R8/ProGuard on the game jar | casual decompilation, name recovery; also shrinks the jar | determined RE with time | Low | none | **Yes** |

### Why not encrypted class loading

A `ClassLoader` reading encrypted `.class` bytes out of the XPK is perhaps 100 lines, and the DI graph would not
fight it — `MetaInject` is 101 lines of explicit registration with no classpath scanning or reflection. It is still
the wrong trade:

- **The bootstrap is always plaintext.** The loader and its key derivation must be readable to run, so the problem
  moves rather than disappears.
- **`defineClass` receives plaintext bytecode.** A `-javaagent` with a `ClassFileTransformer`, or a JVMTI agent, dumps
  every class as it loads. Published tooling exists; this is minutes of work for anyone who has done it once.
- **JDK 25 raises the price.** AOT class loading and linking (JEP 483) and AOT method profiling (JEP 515) cannot cache
  classes arriving through a custom loader, so startup time is traded for a speed bump.
- Plus broken stack traces, awkward IDE and Gradle builds, and a permanent debugging tax.

**Better lever for the same goal:** put only the key-derivation function in a small native library reached through
FFM. Tiny surface, far more annoying to reverse than bytecode, and no interference with class loading, the AOT cache,
DI or debugging — and LWJGL natives already ship, with `--enable-native-access=ALL-UNNAMED` already set in
[build.gradle:10](build.gradle:10), so packaging is solved. Pair it with R8 on the game jar, which also serves the
goal of a smaller jar by shrinking unused library code.

Getting *game logic* out of the jar entirely requires either the classloader route above or native code. The coherent
position: native for the genuinely sensitive parts (key derivation, tamper checks), R8-obfuscated jar for everything
else.

## 9. Patching and delta updates

This constrains the format more sharply than the obfuscation does, and the two pull against each other if the
encryption is designed carelessly.

**Alignment is the lever, and 1 MB is the number.** Confirmed against Valve's own documentation, which contradicts an
earlier guess in this audit that chunking was content-defined:

- *"SteamPipe initially splits each file into roughly one megabyte (MB) chunks."*
- *"When SteamPipe is processing an update for an existing game, it searches to find any such chunks that match the
  previous build of the game."*
- *"To update a 25 GB pack file, SteamPipe will always build a new, 25 GB file."*

Valve's stated guidance for pack files is then explicit, and it matches the recommendations below: *"limit pack file
size. Probably one or two gigabytes (GB) is sufficiently large"*, *"group assets by level / realm / feature into their
own pack files"*, *"avoid shuffling asset ordering with a pack file"*, and keep *"asset changes … localized within the
pack file as much as possible"*.

**So the answer to "is one big archive bad on Steam" is: archives are expected, giant archives are not.** Three
separate reasons to cap pack size at 1–2 GB, only the first of which is about download size:

1. A shift anywhere cascades into every following 1 MB chunk, so a small change can re-download most of the archive.
2. Steam rebuilds the whole target file on the player's disk — a 25 GB pack needs 25 GB of free space to patch,
   whatever the download size.
3. Chunk matching has more to work with when unrelated content is not interleaved.

**Two block sizes, not one.** This is the synthesis of §4.6 and the 1 MB chunk unit, and it resolves what looked like
a conflict:

- **Compression blocks: 64 KB.** Keeps 98.6% of the achievable ratio and 0.221 ms per random access.
- **Pages: 1 MB, 1 MB-aligned.** Compression blocks are packed into 1 MB pages and each page is padded to its
  boundary. A block whose compressed size changes therefore never shifts anything outside its own page, so a change
  dirties exactly one Steam chunk and cannot cascade.

Tail padding costs on average half a compression block per page — about **3% of archive size at 64 KB blocks** — which
buys cascade-proof patching. That is why the compression block is 64 KB here rather than the 128 KB that pure ratio
would pick: at 128 KB the same padding costs ~6%.

**The real risks are all determinism, not geometry:**

| Hazard | Effect on patch size | Fix |
| --- | --- | --- |
| Per-build random salt in the keystream | **Every byte changes; full re-download** | Derive the per-block nonce from block content (§7) |
| Block placement permuted by build-time randomness | Whole file reshuffles | Key the permutation off content/name hashes, never a build seed |
| Non-deterministic pack order (map iteration, timestamps, thread races) | Spurious whole-archive churn | Sort deterministically; zero all timestamps; single-threaded final assembly, or order-stable merge |
| Solid compression across block boundaries | One changed byte invalidates the tail | Per-block compression — already required by C1 |
| TOC at the front of the file | Adding one asset shifts every payload offset | Footer at the end (already in §7) |
| Volatile and stable assets in one archive | A script tweak dirties the texture pack | **Split archives by change rate** |
| Block sizes shifting page contents | Cascades into every later 1 MB chunk | Pad pages to 1 MB; never let a block straddle a page |
| Pack files over ~2 GB | Full-file rebuild on the player's disk | Cap at 1–2 GB, split by level/feature |

The split rows are the cheapest large wins and need no format support: put balance data, scripts and localisation in
one archive and textures, audio and models in another, and cap each at 1–2 GB. A gameplay patch then never touches the
art archive at all — which is exactly what Valve's "group assets by level / realm / feature" advice is for.

**Make determinism a test.** Pack the same inputs twice in separate clean checkouts and assert the outputs are
byte-identical. Without that gate, every other measure here is guesswork.

Sources: [Steamworks — Uploading to Steam](https://partner.steamgames.com/doc/sdk/uploading),
[Aligning Unreal Pak Chunks with SteamPipe Chunks](https://unrealution.com/packaging/aligning-unreal-pak-chunks-with-steampipe-chunks-to-optimize-delta-updates/).

## 10. JVM 25 features to adopt

Adopted decisions. All of these were probed on the project toolchain (Temurin 25.0.3) and work.

| Feature | Status | Use | Decision |
| --- | --- | --- | --- |
| `javax.crypto.KDF` — HKDF (JEP 510) | Final in 25 | Root-key derivation; replaces any hand-rolled HKDF | **Adopt** |
| FFM: `MemorySegment`, `Arena`, `Linker` | Final since 22 | `Arena`-scoped decode buffers (no JNI copy, deterministic free); the native key-derivation function of §8 | **Adopt** |
| Virtual threads | Final since 21 | Archive I/O; replaces every `Thread.yield()` | **Adopt** |
| `ScopedValue` (JEP 506) | Final in 25 | Carry the `XpkProfile` and decrypt context down an async load without threading it through every signature | **Adopt** |
| Compact object headers (JEP 519) | Final in 25 | Thousands of small entry handles | Already enabled in `gradle.properties` |
| AOT class loading (JEP 483 / 515) | 24 / 25 | Startup — and the reason to reject a custom classloader (§8) | **Preserve** |
| `java.lang.classfile` (JEP 484) | Final since 24 | Build-time bytecode work without ASM, if obfuscation is done in-house | Optional |
| `StructuredTaskScope` (JEP 505) | **Preview** in 25 | Load-group fan-out with proper cancellation | **Wait for final** |
| ML-DSA signatures (JEP 497) | Final since 24 | Post-quantum alternative for TOC signing | Not needed; Ed25519 is sufficient |
| Vector API (`jdk.incubator.vector`) | **Incubator** in 25 | A SIMD keystream | **Skip** — AES-NI already beats it, and it needs `--add-modules` |
| `sun.misc.Unsafe` memory access | Being removed | — | Do not build on it; use FFM |

## 11. Suggested order of work

The correctness fixes are worth landing on the current format first — they are small, independently testable, and do
not wait on a format decision.

1. **C1–C4 on v1.** Open one `SevenZFile` per archive, close it, hold a name→entry map, guard the memoised array
   properly (or drop the memo), and stop fabricating aliased handles. C1 alone is the large load-time win.
2. **M3, M9, L1–L7.** Delete the `Thread.yield()` calls and the dead write path, normalise on `/`, tighten the
   `FileHandle` contract. Mechanical.
3. **§4.5 texture pre-transcoding.** Likely the biggest real-world load-time improvement, and independent of the
   container.
4. **Split archives by change rate** (§9). No format work, no code — just repack, and gameplay patches stop touching
   the art archive.
5. **v2 container**, behind an `XpkProfile` (§6) with a v1 reader retained for one release. Land the writer in Meta at
   the same time so the format has an authoritative definition (M10). Order within this step: deterministic packing
   and the byte-identical-output test first, then content addressing, then the 128 KB block layout, then AES-CTR with
   content-derived nonces, then keyed name hashes, then the Ed25519 signature.
6. **Native key derivation via FFM plus R8 on the game jar** (§8). Independent of the container, and the point at
   which the jar-size goal is actually served.
7. **Zstd**, only if §4.4 measurements on real game data justify the native dependency.

Add tests alongside: **byte-identical output from two clean packs of the same inputs** (the gate everything in §9
rests on), a v1↔v2 round-trip, a wrong-profile rejection, a corrupted-block rejection, a tampered-signature
rejection, a pack-time name-hash collision failure, a concurrent-read test that would have caught C3, and a
missing-`child()` test that would have caught C4.
