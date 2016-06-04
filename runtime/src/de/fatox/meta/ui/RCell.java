package de.fatox.meta.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.ui.Value.Fixed;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Pool.Poolable;

/**
 * A RCell for a {@link RTable}.
 * @author Nathan Sweet
 */
public class RCell<T extends Actor> implements Poolable {
    static private final Float zerof = 0f, onef = 1f;
    static private final Integer zeroi = 0, onei = 1;
    static private final Integer centeri = onei, topi = Align.top, bottomi = Align.bottom, lefti = Align.left, righti = Align.right;

    Value minWidth, minHeight;
    Value prefWidth, prefHeight;
    Value maxWidth, maxHeight;
    Value spaceTop, spaceLeft, spaceBottom, spaceRight;
    Value padTop, padLeft, padBottom, padRight;
    Float fillX, fillY;
    Integer align;
    Integer expandX, expandY;
    Integer colspan;
    Boolean uniformX, uniformY;

    Actor actor;
    float actorX, actorY;
    float actorWidth, actorHeight;

    private RTable rtable;
    boolean endRow;
    int column, row;
    int RCellAboveIndex = -1;
    float computedPadTop, computedPadLeft, computedPadBottom, computedPadRight;

    public void setLayout(RTable RTable) {
        this.rtable = RTable;
    }

    /** Sets the actor in this RCell and adds the actor to the RCell's RTable. If null, removes any current actor. */
    public <A extends Actor> RCell<A> setActor(A newActor) {
        if (actor != newActor) {
            if (actor != null)
                actor.remove();
            actor = newActor;
            if (newActor != null)
                rtable.addActor(newActor);
        }
        return (RCell<A>) this;
    }

    /** Removes the current actor for the RCell, if any. */
    public RCell<T> clearActor() {
        setActor(null);
        return this;
    }

    /** Returns the actor for this RCell, or null. */
    public T getActor() {
        return (T) actor;
    }

    /** Returns true if the RCell's actor is not null. */
    public boolean hasActor() {
        return actor != null;
    }

    /** Sets the minWidth, prefWidth, maxWidth, minHeight, prefHeight, and maxHeight to the specified value. */
    public RCell<T> size(Value size) {
        if (size == null)
            throw new IllegalArgumentException("size cannot be null.");
        minWidth = size;
        minHeight = size;
        prefWidth = size;
        prefHeight = size;
        maxWidth = size;
        maxHeight = size;
        return this;
    }

    /** Sets the minWidth, prefWidth, maxWidth, minHeight, prefHeight, and maxHeight to the specified values. */
    public RCell<T> size(Value width, Value height) {
        if (width == null)
            throw new IllegalArgumentException("width cannot be null.");
        if (height == null)
            throw new IllegalArgumentException("height cannot be null.");
        minWidth = width;
        minHeight = height;
        prefWidth = width;
        prefHeight = height;
        maxWidth = width;
        maxHeight = height;
        return this;
    }

    /** Sets the minWidth, prefWidth, maxWidth, minHeight, prefHeight, and maxHeight to the specified value. */
    public RCell<T> size(float size) {
        size(new Fixed(size * RTable.virtualHeight));
        return this;
    }

    /** Sets the minWidth, prefWidth, maxWidth, minHeight, prefHeight, and maxHeight to the specified values. */
    public RCell<T> size(float width, float height) {
        size(new Fixed(width), new Fixed(height));
        return this;
    }

    /** Sets the minWidth, prefWidth, and maxWidth to the specified value. */
    public RCell<T> width(Value width) {
        if (width == null)
            throw new IllegalArgumentException("width cannot be null.");
        minWidth = width;
        prefWidth = width;
        maxWidth = width;
        return this;
    }

    /** Sets the minWidth, prefWidth, and maxWidth to the specified value. */
    public RCell<T> width(int width) {
        width(new Fixed(width));
        return this;
    }

    /** Sets the minWidth, prefWidth, and maxWidth to the specified value. */
    public RCell<T> width(float width) {
        width(new Fixed(width * RTable.virtualWidth));
        return this;
    }

    /** Sets the minHeight, prefHeight, and maxHeight to the specified value. */
    public RCell<T> height(Value height) {
        if (height == null)
            throw new IllegalArgumentException("height cannot be null.");
        minHeight = height;
        prefHeight = height;
        maxHeight = height;
        return this;
    }

    /** Sets the minHeight, prefHeight, and maxHeight to the specified value. */
    public RCell<T> height(float height) {
        height(new Fixed(height * RTable.virtualHeight));
        return this;
    }

    /** Sets the minWidth and minHeight to the specified value. */
    public RCell<T> minSize(Value size) {
        if (size == null)
            throw new IllegalArgumentException("size cannot be null.");
        minWidth = size;
        minHeight = size;
        return this;
    }

    /** Sets the minWidth and minHeight to the specified values. */
    public RCell<T> minSize(Value width, Value height) {
        if (width == null)
            throw new IllegalArgumentException("width cannot be null.");
        if (height == null)
            throw new IllegalArgumentException("height cannot be null.");
        minWidth = width;
        minHeight = height;
        return this;
    }

    public RCell<T> minWidth(Value minWidth) {
        if (minWidth == null)
            throw new IllegalArgumentException("minWidth cannot be null.");
        this.minWidth = minWidth;
        return this;
    }

    public RCell<T> minHeight(Value minHeight) {
        if (minHeight == null)
            throw new IllegalArgumentException("minHeight cannot be null.");
        this.minHeight = minHeight;
        return this;
    }

    /** Sets the minWidth and minHeight to the specified value. */
    public RCell<T> minSize(float size) {
        minSize(new Fixed(size));
        return this;
    }

    /** Sets the minWidth and minHeight to the specified values. */
    public RCell<T> minSize(float width, float height) {
        minSize(new Fixed(width), new Fixed(height));
        return this;
    }

    public RCell<T> minWidth(float minWidth) {
        this.minWidth = new Fixed(minWidth);
        return this;
    }

    public RCell<T> minHeight(float minHeight) {
        this.minHeight = new Fixed(minHeight);
        return this;
    }

    /** Sets the prefWidth and prefHeight to the specified value. */
    public RCell<T> prefSize(Value size) {
        if (size == null)
            throw new IllegalArgumentException("size cannot be null.");
        prefWidth = size;
        prefHeight = size;
        return this;
    }

    /** Sets the prefWidth and prefHeight to the specified values. */
    public RCell<T> prefSize(Value width, Value height) {
        if (width == null)
            throw new IllegalArgumentException("width cannot be null.");
        if (height == null)
            throw new IllegalArgumentException("height cannot be null.");
        prefWidth = width;
        prefHeight = height;
        return this;
    }

    public RCell<T> prefWidth(Value prefWidth) {
        if (prefWidth == null)
            throw new IllegalArgumentException("prefWidth cannot be null.");
        this.prefWidth = prefWidth;
        return this;
    }

    public RCell<T> prefHeight(Value prefHeight) {
        if (prefHeight == null)
            throw new IllegalArgumentException("prefHeight cannot be null.");
        this.prefHeight = prefHeight;
        return this;
    }

    /** Sets the prefWidth and prefHeight to the specified value. */
    public RCell<T> prefSize(float width, float height) {
        prefSize(new Fixed(width), new Fixed(height));
        return this;
    }

    /** Sets the prefWidth and prefHeight to the specified values. */
    public RCell<T> prefSize(float size) {
        prefSize(new Fixed(size));
        return this;
    }

    public RCell<T> prefWidth(float prefWidth) {
        this.prefWidth = new Fixed(prefWidth);
        return this;
    }

    public RCell<T> prefHeight(float prefHeight) {
        this.prefHeight = new Fixed(prefHeight);
        return this;
    }

    /** Sets the maxWidth and maxHeight to the specified value. */
    public RCell<T> maxSize(Value size) {
        if (size == null)
            throw new IllegalArgumentException("size cannot be null.");
        maxWidth = size;
        maxHeight = size;
        return this;
    }

    /** Sets the maxWidth and maxHeight to the specified values. */
    public RCell<T> maxSize(Value width, Value height) {
        if (width == null)
            throw new IllegalArgumentException("width cannot be null.");
        if (height == null)
            throw new IllegalArgumentException("height cannot be null.");
        maxWidth = width;
        maxHeight = height;
        return this;
    }

    public RCell<T> maxWidth(Value maxWidth) {
        if (maxWidth == null)
            throw new IllegalArgumentException("maxWidth cannot be null.");
        this.maxWidth = maxWidth;
        return this;
    }

    public RCell<T> maxHeight(Value maxHeight) {
        if (maxHeight == null)
            throw new IllegalArgumentException("maxHeight cannot be null.");
        this.maxHeight = maxHeight;
        return this;
    }

    /** Sets the maxWidth and maxHeight to the specified value. */
    public RCell<T> maxSize(float size) {
        maxSize(new Fixed(size));
        return this;
    }

    /** Sets the maxWidth and maxHeight to the specified values. */
    public RCell<T> maxSize(float width, float height) {
        maxSize(new Fixed(width), new Fixed(height));
        return this;
    }

    public RCell<T> maxWidth(float maxWidth) {
        this.maxWidth = new Fixed(maxWidth);
        return this;
    }

    public RCell<T> maxHeight(float maxHeight) {
        this.maxHeight = new Fixed(maxHeight);
        return this;
    }

    /** Sets the spaceTop, spaceLeft, spaceBottom, and spaceRight to the specified value. */
    public RCell<T> space(Value space) {
        if (space == null)
            throw new IllegalArgumentException("space cannot be null.");
        spaceTop = space;
        spaceLeft = space;
        spaceBottom = space;
        spaceRight = space;
        return this;
    }

    public RCell<T> space(Value top, Value left, Value bottom, Value right) {
        if (top == null)
            throw new IllegalArgumentException("top cannot be null.");
        if (left == null)
            throw new IllegalArgumentException("left cannot be null.");
        if (bottom == null)
            throw new IllegalArgumentException("bottom cannot be null.");
        if (right == null)
            throw new IllegalArgumentException("right cannot be null.");
        spaceTop = top;
        spaceLeft = left;
        spaceBottom = bottom;
        spaceRight = right;
        return this;
    }

    public RCell<T> spaceTop(Value spaceTop) {
        if (spaceTop == null)
            throw new IllegalArgumentException("spaceTop cannot be null.");
        this.spaceTop = spaceTop;
        return this;
    }

    public RCell<T> spaceLeft(Value spaceLeft) {
        if (spaceLeft == null)
            throw new IllegalArgumentException("spaceLeft cannot be null.");
        this.spaceLeft = spaceLeft;
        return this;
    }

    public RCell<T> spaceBottom(Value spaceBottom) {
        if (spaceBottom == null)
            throw new IllegalArgumentException("spaceBottom cannot be null.");
        this.spaceBottom = spaceBottom;
        return this;
    }

    public RCell<T> spaceRight(Value spaceRight) {
        if (spaceRight == null)
            throw new IllegalArgumentException("spaceRight cannot be null.");
        this.spaceRight = spaceRight;
        return this;
    }

    /** Sets the spaceTop, spaceLeft, spaceBottom, and spaceRight to the specified value. */
    public RCell<T> space(float space) {
        if (space < 0)
            throw new IllegalArgumentException("space cannot be < 0.");
        space(new Fixed(space));
        return this;
    }

    public RCell<T> space(float top, float left, float bottom, float right) {
        if (top < 0)
            throw new IllegalArgumentException("top cannot be < 0.");
        if (left < 0)
            throw new IllegalArgumentException("left cannot be < 0.");
        if (bottom < 0)
            throw new IllegalArgumentException("bottom cannot be < 0.");
        if (right < 0)
            throw new IllegalArgumentException("right cannot be < 0.");
        space(new Fixed(top), new Fixed(left), new Fixed(bottom), new Fixed(right));
        return this;
    }

    public RCell<T> spaceTop(float spaceTop) {
        if (spaceTop < 0)
            throw new IllegalArgumentException("spaceTop cannot be < 0.");
        this.spaceTop = new Fixed(spaceTop);
        return this;
    }

    public RCell<T> spaceLeft(float spaceLeft) {
        if (spaceLeft < 0)
            throw new IllegalArgumentException("spaceLeft cannot be < 0.");
        this.spaceLeft = new Fixed(spaceLeft);
        return this;
    }

    public RCell<T> spaceBottom(float spaceBottom) {
        if (spaceBottom < 0)
            throw new IllegalArgumentException("spaceBottom cannot be < 0.");
        this.spaceBottom = new Fixed(spaceBottom);
        return this;
    }

    public RCell<T> spaceRight(float spaceRight) {
        if (spaceRight < 0)
            throw new IllegalArgumentException("spaceRight cannot be < 0.");
        this.spaceRight = new Fixed(spaceRight);
        return this;
    }

    /** Sets the padTop, padLeft, padBottom, and padRight to the specified value. */
    public RCell<T> pad(Value pad) {
        if (pad == null)
            throw new IllegalArgumentException("pad cannot be null.");
        padTop = pad;
        padLeft = pad;
        padBottom = pad;
        padRight = pad;
        return this;
    }

    public RCell<T> pad(Value top, Value left, Value bottom, Value right) {
        if (top == null)
            throw new IllegalArgumentException("top cannot be null.");
        if (left == null)
            throw new IllegalArgumentException("left cannot be null.");
        if (bottom == null)
            throw new IllegalArgumentException("bottom cannot be null.");
        if (right == null)
            throw new IllegalArgumentException("right cannot be null.");
        padTop = top;
        padLeft = left;
        padBottom = bottom;
        padRight = right;
        return this;
    }

    public RCell<T> padTop(Value padTop) {
        if (padTop == null)
            throw new IllegalArgumentException("padTop cannot be null.");
        this.padTop = padTop;
        return this;
    }

    public RCell<T> padLeft(Value padLeft) {
        if (padLeft == null)
            throw new IllegalArgumentException("padLeft cannot be null.");
        this.padLeft = padLeft;
        return this;
    }

    public RCell<T> padBottom(Value padBottom) {
        if (padBottom == null)
            throw new IllegalArgumentException("padBottom cannot be null.");
        this.padBottom = padBottom;
        return this;
    }

    public RCell<T> padRight(Value padRight) {
        if (padRight == null)
            throw new IllegalArgumentException("padRight cannot be null.");
        this.padRight = padRight;
        return this;
    }

    /** Sets the padTop, padLeft, padBottom, and padRight to the specified value. */
    public RCell<T> pad(float pad) {
        pad(new Fixed(pad * RTable.virtualWidth));
        return this;
    }

    public RCell<T> pad(float top, float left, float bottom, float right) {
        pad(new Fixed(top), new Fixed(left), new Fixed(bottom), new Fixed(right));
        return this;
    }

    public RCell<T> padTop(float padTop) {
        this.padTop = new Fixed(padTop * RTable.virtualHeight);
        return this;
    }

    public RCell<T> padLeft(float padLeft) {
        this.padLeft = new Fixed(padLeft * RTable.virtualWidth);
        return this;
    }

    public RCell<T> padBottom(float padBottom) {
        this.padBottom = new Fixed(padBottom * RTable.virtualHeight);
        return this;
    }

    public RCell<T> padRight(float padRight) {
        this.padRight = new Fixed(padRight * RTable.virtualWidth);
        return this;
    }

    /** Sets fillX and fillY to 1. */
    public RCell<T> fill() {
        fillX = onef;
        fillY = onef;
        return this;
    }

    /** Sets fillX to 1. */
    public RCell<T> fillX() {
        fillX = onef;
        return this;
    }

    /** Sets fillY to 1. */
    public RCell<T> fillY() {
        fillY = onef;
        return this;
    }

    public RCell<T> fill(float x, float y) {
        fillX = x;
        fillY = y;
        return this;
    }

    /** Sets fillX and fillY to 1 if true, 0 if false. */
    public RCell<T> fill(boolean x, boolean y) {
        fillX = x ? onef : zerof;
        fillY = y ? onef : zerof;
        return this;
    }

    /** Sets fillX and fillY to 1 if true, 0 if false. */
    public RCell<T> fill(boolean fill) {
        fillX = fill ? onef : zerof;
        fillY = fill ? onef : zerof;
        return this;
    }

    /**
     * Sets the alignment of the actor within the RCell. Set to {@link Align#center}, {@link Align#top}, {@link Align#bottom},
     * {@link Align#left}, {@link Align#right}, or any combination of those.
     */
    public RCell<T> align(int align) {
        this.align = align;
        return this;
    }

    /** Sets the alignment of the actor within the RCell to {@link Align#center}. This clears any other alignment. */
    public RCell<T> center() {
        align = centeri;
        return this;
    }

    /** Adds {@link Align#top} and clears {@link Align#bottom} for the alignment of the actor within the RCell. */
    public RCell<T> top() {
        if (align == null)
            align = topi;
        else
            align = (align | Align.top) & ~Align.bottom;
        return this;
    }

    /** Adds {@link Align#left} and clears {@link Align#right} for the alignment of the actor within the RCell. */
    public RCell<T> left() {
        if (align == null)
            align = lefti;
        else
            align = (align | Align.left) & ~Align.right;
        return this;
    }

    /** Adds {@link Align#bottom} and clears {@link Align#top} for the alignment of the actor within the RCell. */
    public RCell<T> bottom() {
        if (align == null)
            align = bottomi;
        else
            align = (align | Align.bottom) & ~Align.top;
        return this;
    }

    /** Adds {@link Align#right} and clears {@link Align#left} for the alignment of the actor within the RCell. */
    public RCell<T> right() {
        if (align == null)
            align = righti;
        else
            align = (align | Align.right) & ~Align.left;
        return this;
    }

    /** Sets expandX and expandY to 1. */
    public RCell<T> expand() {
        expandX = onei;
        expandY = onei;
        return this;
    }

    /** Sets expandX to 1. */
    public RCell<T> expandX() {
        expandX = onei;
        return this;
    }

    /** Sets expandY to 1. */
    public RCell<T> expandY() {
        expandY = onei;
        return this;
    }

    public RCell<T> expand(int x, int y) {
        expandX = x;
        expandY = y;
        return this;
    }

    /** Sets expandX and expandY to 1 if true, 0 if false. */
    public RCell<T> expand(boolean x, boolean y) {
        expandX = x ? onei : zeroi;
        expandY = y ? onei : zeroi;
        return this;
    }

    public RCell<T> colspan(int colspan) {
        this.colspan = colspan;
        return this;
    }

    /** Sets uniformX and uniformY to true. */
    public RCell<T> uniform() {
        uniformX = Boolean.TRUE;
        uniformY = Boolean.TRUE;
        return this;
    }

    /** Sets uniformX to true. */
    public RCell<T> uniformX() {
        uniformX = Boolean.TRUE;
        return this;
    }

    /** Sets uniformY to true. */
    public RCell<T> uniformY() {
        uniformY = Boolean.TRUE;
        return this;
    }

    public RCell<T> uniform(boolean x, boolean y) {
        uniformX = x;
        uniformY = y;
        return this;
    }

    public void setActorBounds(float x, float y, float width, float height) {
        actorX = x;
        actorY = y;
        actorWidth = width;
        actorHeight = height;
    }

    public float getActorX() {
        return actorX;
    }

    public void setActorX(float actorX) {
        this.actorX = actorX;
    }

    public float getActorY() {
        return actorY;
    }

    public void setActorY(float actorY) {
        this.actorY = actorY;
    }

    public float getActorWidth() {
        return actorWidth;
    }

    public void setActorWidth(float actorWidth) {
        this.actorWidth = actorWidth;
    }

    public float getActorHeight() {
        return actorHeight;
    }

    public void setActorHeight(float actorHeight) {
        this.actorHeight = actorHeight;
    }

    public int getColumn() {
        return column;
    }

    public int getRow() {
        return row;
    }

    /**
     * @return May be null if this RCell is row defaults.
     */
    public Value getMinWidthValue() {
        return minWidth;
    }

    public float getMinWidth() {
        return minWidth.get(actor);
    }

    /**
     * @return May be null if this RCell is row defaults.
     */
    public Value getMinHeightValue() {
        return minHeight;
    }

    public float getMinHeight() {
        return minHeight.get(actor);
    }

    /**
     * @return May be null if this RCell is row defaults.
     */
    public Value getPrefWidthValue() {
        return prefWidth;
    }

    public float getPrefWidth() {
        return prefWidth.get(actor);
    }

    /**
     * @return May be null if this RCell is row defaults.
     */
    public Value getPrefHeightValue() {
        return prefHeight;
    }

    public float getPrefHeight() {
        return prefHeight.get(actor);
    }

    /**
     * @return May be null if this RCell is row defaults.
     */
    public Value getMaxWidthValue() {
        return maxWidth;
    }

    public float getMaxWidth() {
        return maxWidth.get(actor);
    }

    /**
     * @return May be null if this RCell is row defaults.
     */
    public Value getMaxHeightValue() {
        return maxHeight;
    }

    public float getMaxHeight() {
        return maxHeight.get(actor);
    }

    /**
     * @return May be null if this value is not set.
     */
    public Value getSpaceTopValue() {
        return spaceTop;
    }

    public float getSpaceTop() {
        return spaceTop.get(actor);
    }

    /**
     * @return May be null if this value is not set.
     */
    public Value getSpaceLeftValue() {
        return spaceLeft;
    }

    public float getSpaceLeft() {
        return spaceLeft.get(actor);
    }

    /**
     * @return May be null if this value is not set.
     */
    public Value getSpaceBottomValue() {
        return spaceBottom;
    }

    public float getSpaceBottom() {
        return spaceBottom.get(actor);
    }

    /**
     * @return May be null if this value is not set.
     */
    public Value getSpaceRightValue() {
        return spaceRight;
    }

    public float getSpaceRight() {
        return spaceRight.get(actor);
    }

    /**
     * @return May be null if this value is not set.
     */
    public Value getPadTopValue() {
        return padTop;
    }

    public float getPadTop() {
        return padTop.get(actor);
    }

    /**
     * @return May be null if this value is not set.
     */
    public Value getPadLeftValue() {
        return padLeft;
    }

    public float getPadLeft() {
        return padLeft.get(actor);
    }

    /**
     * @return May be null if this value is not set.
     */
    public Value getPadBottomValue() {
        return padBottom;
    }

    public float getPadBottom() {
        return padBottom.get(actor);
    }

    /**
     * @return May be null if this value is not set.
     */
    public Value getPadRightValue() {
        return padRight;
    }

    public float getPadRight() {
        return padRight.get(actor);
    }

    /** Returns {@link #getPadLeft()} plus {@link #getPadRight()}. */
    public float getPadX() {
        return padLeft.get(actor) + padRight.get(actor);
    }

    /** Returns {@link #getPadTop()} plus {@link #getPadBottom()}. */
    public float getPadY() {
        return padTop.get(actor) + padBottom.get(actor);
    }

    /**
     * @return May be null if this value is not set.
     */
    public float getFillX() {
        return fillX;
    }

    /**
     * @return May be null.
     */
    public float getFillY() {
        return fillY;
    }

    /**
     * @return May be null.
     */
    public int getAlign() {
        return align;
    }

    /**
     * @return May be null.
     */
    public int getExpandX() {
        return expandX;
    }

    /**
     * @return May be null.
     */
    public int getExpandY() {
        return expandY;
    }

    /**
     * @return May be null.
     */
    public int getColspan() {
        return colspan;
    }

    /**
     * @return May be null.
     */
    public boolean getUniformX() {
        return uniformX;
    }

    /**
     * @return May be null.
     */
    public boolean getUniformY() {
        return uniformY;
    }

    /** Returns true if this RCell is the last RCell in the row. */
    public boolean isEndRow() {
        return endRow;
    }

    /** The actual amount of combined padding and spacing from the last layout. */
    public float getComputedPadTop() {
        return computedPadTop;
    }

    /** The actual amount of combined padding and spacing from the last layout. */
    public float getComputedPadLeft() {
        return computedPadLeft;
    }

    /** The actual amount of combined padding and spacing from the last layout. */
    public float getComputedPadBottom() {
        return computedPadBottom;
    }

    /** The actual amount of combined padding and spacing from the last layout. */
    public float getComputedPadRight() {
        return computedPadRight;
    }

    public RCell<T> row() {
        return rtable.row();
    }

    public RTable getRTable() {
        return rtable;
    }

    /** Sets all constraint fields to null. */
    void clear() {
        minWidth = null;
        minHeight = null;
        prefWidth = null;
        prefHeight = null;
        maxWidth = null;
        maxHeight = null;
        spaceTop = null;
        spaceLeft = null;
        spaceBottom = null;
        spaceRight = null;
        padTop = null;
        padLeft = null;
        padBottom = null;
        padRight = null;
        fillX = null;
        fillY = null;
        align = null;
        expandX = null;
        expandY = null;
        colspan = null;
        uniformX = null;
        uniformY = null;
    }

    /** Reset state so the RCell can be reused. Doesn't reset the constraint fields. */
    @Override
    public void reset() {
        actor = null;
        rtable = null;
        endRow = false;
        RCellAboveIndex = -1;
    }

    /** Set all constraints to RCell default values. */
    void defaults() {
        minWidth = Value.minWidth;
        minHeight = Value.minHeight;
        prefWidth = Value.prefWidth;
        prefHeight = Value.prefHeight;
        maxWidth = Value.maxWidth;
        maxHeight = Value.maxHeight;
        spaceTop = Value.zero;
        spaceLeft = Value.zero;
        spaceBottom = Value.zero;
        spaceRight = Value.zero;
        padTop = Value.zero;
        padLeft = Value.zero;
        padBottom = Value.zero;
        padRight = Value.zero;
        fillX = zerof;
        fillY = zerof;
        align = centeri;
        expandX = zeroi;
        expandY = zeroi;
        colspan = onei;
        uniformX = null;
        uniformY = null;
    }

    void set(RCell defaults) {
        minWidth = defaults.minWidth;
        minHeight = defaults.minHeight;
        prefWidth = defaults.prefWidth;
        prefHeight = defaults.prefHeight;
        maxWidth = defaults.maxWidth;
        maxHeight = defaults.maxHeight;
        spaceTop = defaults.spaceTop;
        spaceLeft = defaults.spaceLeft;
        spaceBottom = defaults.spaceBottom;
        spaceRight = defaults.spaceRight;
        padTop = defaults.padTop;
        padLeft = defaults.padLeft;
        padBottom = defaults.padBottom;
        padRight = defaults.padRight;
        fillX = defaults.fillX;
        fillY = defaults.fillY;
        align = defaults.align;
        expandX = defaults.expandX;
        expandY = defaults.expandY;
        colspan = defaults.colspan;
        uniformX = defaults.uniformX;
        uniformY = defaults.uniformY;
    }

    /**
     * @param RCell
     *            May be null.
     */
    void merge(RCell RCell) {
        if (RCell == null)
            return;
        if (RCell.minWidth != null)
            minWidth = RCell.minWidth;
        if (RCell.minHeight != null)
            minHeight = RCell.minHeight;
        if (RCell.prefWidth != null)
            prefWidth = RCell.prefWidth;
        if (RCell.prefHeight != null)
            prefHeight = RCell.prefHeight;
        if (RCell.maxWidth != null)
            maxWidth = RCell.maxWidth;
        if (RCell.maxHeight != null)
            maxHeight = RCell.maxHeight;
        if (RCell.spaceTop != null)
            spaceTop = RCell.spaceTop;
        if (RCell.spaceLeft != null)
            spaceLeft = RCell.spaceLeft;
        if (RCell.spaceBottom != null)
            spaceBottom = RCell.spaceBottom;
        if (RCell.spaceRight != null)
            spaceRight = RCell.spaceRight;
        if (RCell.padTop != null)
            padTop = RCell.padTop;
        if (RCell.padLeft != null)
            padLeft = RCell.padLeft;
        if (RCell.padBottom != null)
            padBottom = RCell.padBottom;
        if (RCell.padRight != null)
            padRight = RCell.padRight;
        if (RCell.fillX != null)
            fillX = RCell.fillX;
        if (RCell.fillY != null)
            fillY = RCell.fillY;
        if (RCell.align != null)
            align = RCell.align;
        if (RCell.expandX != null)
            expandX = RCell.expandX;
        if (RCell.expandY != null)
            expandY = RCell.expandY;
        if (RCell.colspan != null)
            colspan = RCell.colspan;
        if (RCell.uniformX != null)
            uniformX = RCell.uniformX;
        if (RCell.uniformY != null)
            uniformY = RCell.uniformY;
    }

}
