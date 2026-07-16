# Meta Icons

Meta bundles Remix Icon 4.9.1 as a font-backed icon set.

Use icons by Remix class name:

```kotlin
MetaIcon("ri-information-line")
MetaImageButton("ri-add-line")
MetaIconTextButton("Open", "ri-folder-open-line")
```

Search `remixicon.tsv` for supported names. The columns are:

- `name`: Remix class name, accepted by `MetaIcon` and `MetaIcons.glyph`.
- `codepoint_hex`: font codepoint used internally.
- `category`: upstream Remix folder/category.

Runtime code can query the same data through `MetaIcons.exists`, `MetaIcons.info`, `MetaIcons.names`,
`MetaIcons.entries`, and `MetaIcons.search`. This keeps the icon catalog discoverable without generating a 3k-entry
enum.

The authoring copies live under `assets/`; runtime consumers load mirrored copies from
`runtime/src/main/resources/`. When updating the font, catalog, or license, update both locations together and keep
the corresponding files byte-for-byte identical.

Use this path for ordinary UI glyphs instead of adding standalone PNG toolbar icons. Bitmap assets still make sense for
game/editor art, logos, previews, screenshots, atlases, or other non-glyph visuals.

The bundled Remix Icon license permits use inside apps, games, templates, design systems, and UI kits. Meta ships the
icons as a minor UI component, not as a standalone icon pack or paid icon library; keep that distinction intact when
redistributing.
