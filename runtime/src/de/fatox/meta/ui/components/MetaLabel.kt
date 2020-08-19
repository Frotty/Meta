package de.fatox.meta.ui.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.StringBuilder;
import de.fatox.meta.Meta;
import de.fatox.meta.api.graphics.FontProvider;
import de.fatox.meta.injection.Inject;

/**
 * A text label, with optional word wrapping.
 * <p>
 * The preferred size of the label is determined by the actual text bounds, unless {@link #setWrap(boolean) word wrap} is enabled.
 *
 * @author Nathan Sweet
 */
public class MetaLabel extends Widget {
    static private final Color tempColor = new Color();
    static private final GlyphLayout prefSizeLayout = new GlyphLayout();

    private final GlyphLayout layout = new GlyphLayout();
    private final Vector2 prefSize = new Vector2();
    private final StringBuilder text = new StringBuilder();
    private BitmapFont font;
    private float size;
    private BitmapFontCache cache;
    private int labelAlign = Align.left;
    private int lineAlign = Align.left;
    private boolean wrap;
    private float lastPrefHeight;
    private boolean prefSizeInvalid = true;
    private float fontScaleX = 1, fontScaleY = 1;
    private String ellipsis;
    @Inject
    private FontProvider metaFontProvider;
    private final Color fontColor;
    private final boolean mono;

	public MetaLabel(CharSequence text, int size, Color color) {
		this(text, size, color, false);
	}

    public MetaLabel(CharSequence text, int size, Color color, boolean monospace) {
        Meta.inject(this);
        this.mono = monospace;
        this.size = size;
        font = metaFontProvider.getFont(size, monospace);
        setAlignment(Align.center);
        fontColor = color;
        setText(text);
        cache = font.newFontCache();
        layout();
    }

    public MetaLabel(CharSequence text, int size) {
        this(text, size, Color.WHITE);
    }

    /** @param newText May be null, "" will be used. */
    public void setText(CharSequence newText) {
        if (newText == null) newText = "";
        if (newText instanceof StringBuilder) {
            if (text.equals(newText)) return;
            text.setLength(0);
            text.append((StringBuilder) newText);
        } else {
            if (textEquals(newText)) return;
            text.setLength(0);
            text.append(newText);
        }
        invalidateHierarchy();
    }

    public boolean textEquals(CharSequence other) {
        int length = text.length;
        char[] chars = text.chars;
        if (length != other.length()) return false;
        for (int i = 0; i < length; i++)
            if (chars[i] != other.charAt(i)) return false;
        return true;
    }

    public StringBuilder getText() {
        return text;
    }

    public void invalidate() {
        super.invalidate();
        prefSizeInvalid = true;
    }

    private void scaleAndComputePrefSize() {
        BitmapFont font = cache.getFont();
        float oldScaleX = font.getScaleX();
        float oldScaleY = font.getScaleY();
        if (fontScaleX != oldScaleX || fontScaleY != oldScaleY) font.getData().setScale(fontScaleX, fontScaleY);

        computePrefSize();

        if (fontScaleX != oldScaleX || fontScaleY != oldScaleY) font.getData().setScale(oldScaleX, oldScaleY);
    }

    private void computePrefSize() {
        prefSizeInvalid = false;
        GlyphLayout prefSizeLayout = MetaLabel.prefSizeLayout;
        if (wrap && ellipsis == null) {
            float width = getWidth();
            prefSizeLayout.setText(cache.getFont(), text, Color.WHITE, width, Align.left, true);
        } else
            prefSizeLayout.setText(cache.getFont(), text);
        prefSize.set(prefSizeLayout.width, prefSizeLayout.height);
    }

    public void layout() {
        BitmapFont font = cache.getFont();
        float oldScaleX = font.getScaleX();
        float oldScaleY = font.getScaleY();
        if (fontScaleX != oldScaleX || fontScaleY != oldScaleY) font.getData().setScale(fontScaleX, fontScaleY);

        boolean wrap = this.wrap && ellipsis == null;
        if (wrap) {
            float prefHeight = getPrefHeight();
            if (prefHeight != lastPrefHeight) {
                lastPrefHeight = prefHeight;
                invalidateHierarchy();
            }
        }

        float width = getWidth(), height = getHeight();
        float x = 0, y = 0;

        GlyphLayout layout = this.layout;
        float textWidth, textHeight;
        if (wrap || text.indexOf("\n") != -1) {
            // If the text can span multiple lines, determine the text's actual size so it can be aligned within the label.
            layout.setText(font, text, 0, text.length, Color.WHITE, width, lineAlign, wrap, ellipsis);
            textWidth = layout.width;
            textHeight = layout.height;

            if ((labelAlign & Align.left) == 0) {
                if ((labelAlign & Align.right) != 0)
                    x += width - textWidth;
                else
                    x += (width - textWidth) / 2;
            }
        } else {
            textWidth = width;
            textHeight = font.getData().capHeight;
        }

        if ((labelAlign & Align.top) != 0) {
            y += cache.getFont().isFlipped() ? 0 : height - textHeight;
            y += font.getDescent();
        } else if ((labelAlign & Align.bottom) != 0) {
            y += cache.getFont().isFlipped() ? height - textHeight : 0;
            y -= font.getDescent();
        } else {
            y += (height - textHeight) / 2;
        }
        if (!cache.getFont().isFlipped()) y += textHeight;

        layout.setText(font, text, 0, text.length, Color.WHITE, textWidth, lineAlign, wrap, ellipsis);
        cache.setText(layout, x, y);

        if (fontScaleX != oldScaleX || fontScaleY != oldScaleY) font.getData().setScale(oldScaleX, oldScaleY);
    }

    public void draw(Batch batch, float parentAlpha) {
        validate();
        Color color = tempColor.set(getColor());
        color.a *= parentAlpha;
        if (fontColor != null) color.mul(fontColor);
        cache.tint(color);
        cache.setPosition(getX(), getY());
        cache.draw(batch);
    }

    public float getPrefWidth() {
        if (wrap) return 0;
        if (prefSizeInvalid) scaleAndComputePrefSize();
        float width = prefSize.x;
        return width;
    }

    public float getPrefHeight() {
        if (prefSizeInvalid) scaleAndComputePrefSize();
        float height = prefSize.y - font.getDescent() * fontScaleY * 2;
        return height;
    }

    public GlyphLayout getGlyphLayout() {
        return layout;
    }

    /**
     * If false, the text will only wrap where it contains newlines (\n). The preferred size of the label will be the text bounds.
     * If true, the text will word wrap using the width of the label. The preferred width of the label will be 0, it is expected
     * that something external will set the width of the label. Wrapping will not occur when ellipsis is enabled. Default is false.
     * <p>
     * When wrap is enabled, the label's preferred height depends on the width of the label. In some cases the parent of the label
     * will need to layout twice: once to set the width of the label and a second time to adjust to the label's new preferred
     * height.
     */
    public void setWrap(boolean wrap) {
        this.wrap = wrap;
        invalidateHierarchy();
    }

    public int getLabelAlign() {
        return labelAlign;
    }

    public int getLineAlign() {
        return lineAlign;
    }

    /**
     * @param alignment Aligns all the text within the label (default left center) and each line of text horizontally (default
     *                  left).
     * @see Align
     */
    public void setAlignment(int alignment) {
        setAlignment(alignment, alignment);
    }

    /**
     * @param labelAlign Aligns all the text within the label (default left center).
     * @param lineAlign  Aligns each line of text horizontally (default left).
     * @see Align
     */
    public void setAlignment(int labelAlign, int lineAlign) {
        this.labelAlign = labelAlign;

        if ((lineAlign & Align.left) != 0)
            this.lineAlign = Align.left;
        else if ((lineAlign & Align.right) != 0)
            this.lineAlign = Align.right;
        else
            this.lineAlign = Align.center;

        invalidate();
    }

    public void setFontScale(float fontScale) {
        this.fontScaleX = fontScale;
        this.fontScaleY = fontScale;
        invalidateHierarchy();
    }

    public void setFontScale(float fontScaleX, float fontScaleY) {
        this.fontScaleX = fontScaleX;
        this.fontScaleY = fontScaleY;
        invalidateHierarchy();
    }

    public float getFontScaleX() {
        return fontScaleX;
    }

    public void setFontScaleX(float fontScaleX) {
        this.fontScaleX = fontScaleX;
        invalidateHierarchy();
    }

    public float getFontScaleY() {
        return fontScaleY;
    }

    public void setFontScaleY(float fontScaleY) {
        this.fontScaleY = fontScaleY;
        invalidateHierarchy();
    }

    /**
     * When non-null the text will be truncated "..." if it does not fit within the width of the label. Wrapping will not occur
     * when ellipsis is enabled. Default is false.
     */
    public void setEllipsis(String ellipsis) {
        this.ellipsis = ellipsis;
    }

    /**
     * When true the text will be truncated "..." if it does not fit within the width of the label. Wrapping will not occur when
     * ellipsis is true. Default is false.
     */
    public void setEllipsis(boolean ellipsis) {
        if (ellipsis)
            this.ellipsis = "...";
        else
            this.ellipsis = null;
    }

    /** Allows subclasses to access the cache in {@link #draw(Batch, float)}. */
    protected BitmapFontCache getBitmapFontCache() {
        return cache;
    }

    public String toString() {
        return super.toString() + ": " + text;
    }

    public void setMaxWidth(int maxWidth) {
        while (getGlyphLayout().width > maxWidth) {
            size *= 0.95;
			updateFont();
		}
    }

	private void updateFont() {
		font = metaFontProvider.getFont(Math.round(size), mono);
		setText(text);
		cache = font.newFontCache();
		layout();
	}

	public void setFontSize(int size) {
		this.size = size;
		updateFont();
	}

    /**
     * The style for a label, see {@link MetaLabel}.
     *
     * @author Nathan Sweet
     */
    static public class LabelStyle {
        public BitmapFont font;
        /** Optional. */
        public Color fontColor;
        /** Optional. */
        public Drawable background;

        public LabelStyle() {
        }

        public LabelStyle(BitmapFont font, Color fontColor) {
            this.font = font;
            this.fontColor = fontColor;
        }

        public LabelStyle(LabelStyle style) {
            this.font = style.font;
            if (style.fontColor != null) fontColor = new Color(style.fontColor);
            background = style.background;
        }
    }
}
