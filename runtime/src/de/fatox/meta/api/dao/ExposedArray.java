/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package de.fatox.meta.api.dao;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Predicate;
import com.badlogic.gdx.utils.Select;
import com.badlogic.gdx.utils.Sort;
import com.badlogic.gdx.utils.reflect.ArrayReflection;
import com.google.gson.annotations.Expose;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/** A resizable, ordered or unordered exposedArray of objects. If unordered, this class avoids a memory copy when removing elements (the
 * last element is moved to the removed element's position).
 * @author Nathan Sweet */
public class ExposedArray<T> implements Iterable<T> {
    /** Provides direct access to the underlying exposedArray. If the ExposedArray's generic type is not Object, this field may only be accessed
     * if the {@link ExposedArray#ExposedArray(boolean, int, Class)} constructor was used. */
    @Expose
    public T[] items;

    @Expose
    public int size;
    public boolean ordered;

    private ArrayIterable iterable;
    private Predicate.PredicateIterable<T> predicateIterable;

    /** Creates an ordered exposedArray with a capacity of 16. */
    public ExposedArray() {
        this(true, 2);
    }

    /** Creates an ordered exposedArray with the specified capacity. */
    public ExposedArray(int capacity) {
        this(true, capacity);
    }

    /** @param ordered If false, methods that remove elements may change the order of other elements in the exposedArray, which avoids a
     *           memory copy.
     * @param capacity Any elements added beyond this will cause the backing exposedArray to be grown. */
    public ExposedArray(boolean ordered, int capacity) {
        this.ordered = ordered;
        items = (T[])new Object[capacity];
    }

    /** Creates a new exposedArray with {@link #items} of the specified type.
     * @param ordered If false, methods that remove elements may change the order of other elements in the exposedArray, which avoids a
     *           memory copy.
     * @param capacity Any elements added beyond this will cause the backing exposedArray to be grown. */
    public ExposedArray(boolean ordered, int capacity, Class arrayType) {
        this.ordered = ordered;
        items = (T[])ArrayReflection.newInstance(arrayType, capacity);
    }

    /** Creates an ordered exposedArray with {@link #items} of the specified type and a capacity of 16. */
    public ExposedArray(Class arrayType) {
        this(true, 16, arrayType);
    }

    /** Creates a new exposedArray containing the elements in the specified exposedArray. The new exposedArray will have the same type of backing exposedArray
     * and will be ordered if the specified exposedArray is ordered. The capacity is set to the number of elements, so any subsequent
     * elements added will cause the backing exposedArray to be grown. */
    public ExposedArray(ExposedArray<? extends T> exposedArray) {
        this(exposedArray.ordered, exposedArray.size, exposedArray.items.getClass().getComponentType());
        size = exposedArray.size;
        System.arraycopy(exposedArray.items, 0, items, 0, size);
    }

    /** Creates a new ordered exposedArray containing the elements in the specified exposedArray. The new exposedArray will have the same type of
     * backing exposedArray. The capacity is set to the number of elements, so any subsequent elements added will cause the backing exposedArray
     * to be grown. */
    public ExposedArray(T[] array) {
        this(true, array, 0, array.length);
    }

    /** Creates a new exposedArray containing the elements in the specified exposedArray. The new exposedArray will have the same type of backing exposedArray.
     * The capacity is set to the number of elements, so any subsequent elements added will cause the backing exposedArray to be grown.
     * @param ordered If false, methods that remove elements may change the order of other elements in the exposedArray, which avoids a
     *           memory copy. */
    public ExposedArray(boolean ordered, T[] array, int start, int count) {
        this(ordered, count, (Class)array.getClass().getComponentType());
        size = count;
        System.arraycopy(array, start, items, 0, size);
    }

    public void add (T value) {
        T[] items = this.items;
        if (size == items.length) items = resize(Math.max(8, (int)(size * 1.75f)));
        items[size++] = value;
    }

    public void addAll (ExposedArray<? extends T> exposedArray) {
        addAll(exposedArray, 0, exposedArray.size);
    }

    public void addAll (ExposedArray<? extends T> exposedArray, int start, int count) {
        if (start + count > exposedArray.size)
            throw new IllegalArgumentException("start + count must be <= size: " + start + " + " + count + " <= " + exposedArray.size);
        addAll((T[]) exposedArray.items, start, count);
    }

    public void addAll (T... array) {
        addAll(array, 0, array.length);
    }

    public void addAll (T[] array, int start, int count) {
        T[] items = this.items;
        int sizeNeeded = size + count;
        if (sizeNeeded > items.length) items = resize(Math.max(8, (int)(sizeNeeded * 1.75f)));
        System.arraycopy(array, start, items, size, count);
        size += count;
    }

    public T get (int index) {
        if (index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
        return items[index];
    }

    public void set (int index, T value) {
        if (index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
        items[index] = value;
    }

    public void insert (int index, T value) {
        if (index > size) throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + size);
        T[] items = this.items;
        if (size == items.length) items = resize(Math.max(8, (int)(size * 1.75f)));
        if (ordered)
            System.arraycopy(items, index, items, index + 1, size - index);
        else
            items[size] = items[index];
        size++;
        items[index] = value;
    }

    public void swap (int first, int second) {
        if (first >= size) throw new IndexOutOfBoundsException("first can't be >= size: " + first + " >= " + size);
        if (second >= size) throw new IndexOutOfBoundsException("second can't be >= size: " + second + " >= " + size);
        T[] items = this.items;
        T firstValue = items[first];
        items[first] = items[second];
        items[second] = firstValue;
    }

    /** Returns if this exposedArray contains value.
     * @param value May be null.
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
     * @return true if exposedArray contains value, false if it doesn't */
    public boolean contains (T value, boolean identity) {
        T[] items = this.items;
        int i = size - 1;
        if (identity || value == null) {
            while (i >= 0)
                if (items[i--] == value) return true;
        } else {
            while (i >= 0)
                if (value.equals(items[i--])) return true;
        }
        return false;
    }

    /** Returns the index of first occurrence of value in the exposedArray, or -1 if no such value exists.
     * @param value May be null.
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
     * @return An index of first occurrence of value in exposedArray or -1 if no such value exists */
    public int indexOf (T value, boolean identity) {
        T[] items = this.items;
        if (identity || value == null) {
            for (int i = 0, n = size; i < n; i++)
                if (items[i] == value) return i;
        } else {
            for (int i = 0, n = size; i < n; i++)
                if (value.equals(items[i])) return i;
        }
        return -1;
    }

    /** Returns an index of last occurrence of value in exposedArray or -1 if no such value exists. Search is started from the end of an
     * exposedArray.
     * @param value May be null.
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
     * @return An index of last occurrence of value in exposedArray or -1 if no such value exists */
    public int lastIndexOf (T value, boolean identity) {
        T[] items = this.items;
        if (identity || value == null) {
            for (int i = size - 1; i >= 0; i--)
                if (items[i] == value) return i;
        } else {
            for (int i = size - 1; i >= 0; i--)
                if (value.equals(items[i])) return i;
        }
        return -1;
    }

    /** Removes the first instance of the specified value in the exposedArray.
     * @param value May be null.
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
     * @return true if value was found and removed, false otherwise */
    public boolean removeValue (T value, boolean identity) {
        T[] items = this.items;
        if (identity || value == null) {
            for (int i = 0, n = size; i < n; i++) {
                if (items[i] == value) {
                    removeIndex(i);
                    return true;
                }
            }
        } else {
            for (int i = 0, n = size; i < n; i++) {
                if (value.equals(items[i])) {
                    removeIndex(i);
                    return true;
                }
            }
        }
        return false;
    }

    /** Removes and returns the item at the specified index. */
    public T removeIndex (int index) {
        if (index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
        T[] items = this.items;
        T value = (T)items[index];
        size--;
        if (ordered)
            System.arraycopy(items, index + 1, items, index, size - index);
        else
            items[index] = items[size];
        items[size] = null;
        return value;
    }

    /** Removes the items between the specified indices, inclusive. */
    public void removeRange (int start, int end) {
        if (end >= size) throw new IndexOutOfBoundsException("end can't be >= size: " + end + " >= " + size);
        if (start > end) throw new IndexOutOfBoundsException("start can't be > end: " + start + " > " + end);
        T[] items = this.items;
        int count = end - start + 1;
        if (ordered)
            System.arraycopy(items, start + count, items, start, size - (start + count));
        else {
            int lastIndex = this.size - 1;
            for (int i = 0; i < count; i++)
                items[start + i] = items[lastIndex - i];
        }
        size -= count;
    }

    /** Removes from this exposedArray all of elements contained in the specified exposedArray.
     * @param identity True to use ==, false to use .equals().
     * @return true if this exposedArray was modified. */
    public boolean removeAll (ExposedArray<? extends T> exposedArray, boolean identity) {
        int size = this.size;
        int startSize = size;
        T[] items = this.items;
        if (identity) {
            for (int i = 0, n = exposedArray.size; i < n; i++) {
                T item = exposedArray.get(i);
                for (int ii = 0; ii < size; ii++) {
                    if (item == items[ii]) {
                        removeIndex(ii);
                        size--;
                        break;
                    }
                }
            }
        } else {
            for (int i = 0, n = exposedArray.size; i < n; i++) {
                T item = exposedArray.get(i);
                for (int ii = 0; ii < size; ii++) {
                    if (item.equals(items[ii])) {
                        removeIndex(ii);
                        size--;
                        break;
                    }
                }
            }
        }
        return size != startSize;
    }

    /** Removes and returns the last item. */
    public T pop () {
        if (size == 0) throw new IllegalStateException("ExposedArray is empty.");
        --size;
        T item = items[size];
        items[size] = null;
        return item;
    }

    /** Returns the last item. */
    public T peek () {
        if (size == 0) throw new IllegalStateException("ExposedArray is empty.");
        return items[size - 1];
    }

    /** Returns the first item. */
    public T first () {
        if (size == 0) throw new IllegalStateException("ExposedArray is empty.");
        return items[0];
    }

    public void clear () {
        T[] items = this.items;
        for (int i = 0, n = size; i < n; i++)
            items[i] = null;
        size = 0;
    }

    /** Reduces the size of the backing exposedArray to the size of the actual items. This is useful to release memory when many items
     * have been removed, or if it is known that more items will not be added.
     * @return {@link #items} */
    public T[] shrink () {
        if (items.length != size) resize(size);
        return items;
    }

    /** Increases the size of the backing exposedArray to accommodate the specified number of additional items. Useful before adding many
     * items to avoid multiple backing exposedArray resizes.
     * @return {@link #items} */
    public T[] ensureCapacity (int additionalCapacity) {
        int sizeNeeded = size + additionalCapacity;
        if (sizeNeeded > items.length) resize(Math.max(8, sizeNeeded));
        return items;
    }

    /** Sets the exposedArray size, leaving any values beyond the current size null.
     * @return {@link #items} */
    public T[] setSize (int newSize) {
        truncate(newSize);
        if (newSize > items.length) resize(Math.max(8, newSize));
        size = newSize;
        return items;
    }

    /** Creates a new backing exposedArray with the specified size containing the current items. */
    protected T[] resize (int newSize) {
        T[] items = this.items;
        T[] newItems = (T[])ArrayReflection.newInstance(items.getClass().getComponentType(), newSize);
        System.arraycopy(items, 0, newItems, 0, Math.min(size, newItems.length));
        this.items = newItems;
        return newItems;
    }

    /** Sorts this exposedArray. The exposedArray elements must implement {@link Comparable}. This method is not thread safe (uses
     * {@link Sort#instance()}). */
    public void sort () {
        Sort.instance().sort(items, 0, size);
    }

    /** Sorts the exposedArray. This method is not thread safe (uses {@link Sort#instance()}). */
    public void sort (Comparator<? super T> comparator) {
        Sort.instance().sort(items, comparator, 0, size);
    }

    /** Selects the nth-lowest element from the ExposedArray according to Comparator ranking. This might partially sort the ExposedArray. The
     * exposedArray must have a size greater than 0, or a {@link com.badlogic.gdx.utils.GdxRuntimeException} will be thrown.
     * @see Select
     * @param comparator used for comparison
     * @param kthLowest rank of desired object according to comparison, n is based on ordinal numbers, not exposedArray indices. for min
     *           value use 1, for max value use size of exposedArray, using 0 results in runtime exception.
     * @return the value of the Nth lowest ranked object. */
    public T selectRanked (Comparator<T> comparator, int kthLowest) {
        if (kthLowest < 1) {
            throw new GdxRuntimeException("nth_lowest must be greater than 0, 1 = first, 2 = second...");
        }
        return Select.instance().select(items, comparator, kthLowest, size);
    }

    /** @see ExposedArray#selectRanked(java.util.Comparator, int)
     * @param comparator used for comparison
     * @param kthLowest rank of desired object according to comparison, n is based on ordinal numbers, not exposedArray indices. for min
     *           value use 1, for max value use size of exposedArray, using 0 results in runtime exception.
     * @return the index of the Nth lowest ranked object. */
    public int selectRankedIndex (Comparator<T> comparator, int kthLowest) {
        if (kthLowest < 1) {
            throw new GdxRuntimeException("nth_lowest must be greater than 0, 1 = first, 2 = second...");
        }
        return Select.instance().selectIndex(items, comparator, kthLowest, size);
    }

    public void reverse () {
        T[] items = this.items;
        for (int i = 0, lastIndex = size - 1, n = size / 2; i < n; i++) {
            int ii = lastIndex - i;
            T temp = items[i];
            items[i] = items[ii];
            items[ii] = temp;
        }
    }

    public void shuffle () {
        T[] items = this.items;
        for (int i = size - 1; i >= 0; i--) {
            int ii = MathUtils.random(i);
            T temp = items[i];
            items[i] = items[ii];
            items[ii] = temp;
        }
    }

    /** Returns an iterator for the items in the exposedArray. Remove is supported. Note that the same iterator instance is returned each
     * time this method is called. Use the {@link ArrayIterator} constructor for nested or multithreaded iteration. */
    public Iterator<T> iterator () {
        if (iterable == null) iterable = new ArrayIterable(this);
        return iterable.iterator();
    }

    /** Returns an iterable for the selected items in the exposedArray. Remove is supported, but not between hasNext() and next(). Note
     * that the same iterable instance is returned each time this method is called. Use the {@link Predicate.PredicateIterable}
     * constructor for nested or multithreaded iteration. */
    public Iterable<T> select (Predicate<T> predicate) {
        if (predicateIterable == null)
            predicateIterable = new Predicate.PredicateIterable<T>(this, predicate);
        else
            predicateIterable.set(this, predicate);
        return predicateIterable;
    }

    /** Reduces the size of the exposedArray to the specified size. If the exposedArray is already smaller than the specified size, no action is
     * taken. */
    public void truncate (int newSize) {
        if (size <= newSize) return;
        for (int i = newSize; i < size; i++)
            items[i] = null;
        size = newSize;
    }

    /** Returns a random item from the exposedArray, or null if the exposedArray is empty. */
    public T random () {
        if (size == 0) return null;
        return items[MathUtils.random(0, size - 1)];
    }

    /** Returns the items as an exposedArray. Note the exposedArray is typed, so the {@link #ExposedArray(Class)} constructor must have been used.
     * Otherwise use {@link #toArray(Class)} to specify the exposedArray type. */
    public T[] toArray () {
        return (T[])toArray(items.getClass().getComponentType());
    }

    public <V> V[] toArray (Class type) {
        V[] result = (V[])ArrayReflection.newInstance(type, size);
        System.arraycopy(items, 0, result, 0, size);
        return result;
    }

    public int hashCode () {
        if (!ordered) return super.hashCode();
        Object[] items = this.items;
        int h = 1;
        for (int i = 0, n = size; i < n; i++) {
            h *= 31;
            Object item = items[i];
            if (item != null) h += item.hashCode();
        }
        return h;
    }

    public boolean equals (Object object) {
        if (object == this) return true;
        if (!ordered) return false;
        if (!(object instanceof ExposedArray)) return false;
        ExposedArray exposedArray = (ExposedArray)object;
        if (!exposedArray.ordered) return false;
        int n = size;
        if (n != exposedArray.size) return false;
        Object[] items1 = this.items;
        Object[] items2 = exposedArray.items;
        for (int i = 0; i < n; i++) {
            Object o1 = items1[i];
            Object o2 = items2[i];
            if (!(o1 == null ? o2 == null : o1.equals(o2))) return false;
        }
        return true;
    }

    public String toString () {
        if (size == 0) return "[]";
        T[] items = this.items;
        StringBuilder buffer = new StringBuilder(32);
        buffer.append('[');
        buffer.append(items[0]);
        for (int i = 1; i < size; i++) {
            buffer.append(", ");
            buffer.append(items[i]);
        }
        buffer.append(']');
        return buffer.toString();
    }

    public String toString (String separator) {
        if (size == 0) return "";
        T[] items = this.items;
        StringBuilder buffer = new StringBuilder(32);
        buffer.append(items[0]);
        for (int i = 1; i < size; i++) {
            buffer.append(separator);
            buffer.append(items[i]);
        }
        return buffer.toString();
    }

    /** @see #ExposedArray(Class) */
    static public <T> ExposedArray<T> of (Class<T> arrayType) {
        return new ExposedArray<T>(arrayType);
    }

    /** @see #ExposedArray(boolean, int, Class) */
    static public <T> ExposedArray<T> of (boolean ordered, int capacity, Class<T> arrayType) {
        return new ExposedArray<T>(ordered, capacity, arrayType);
    }

    /** @see #ExposedArray(Object[]) */
    static public <T> ExposedArray<T> with (T... array) {
        return new ExposedArray(array);
    }

    static public class ArrayIterator<T> implements Iterator<T>, Iterable<T> {
        private final ExposedArray<T> exposedArray;
        private final boolean allowRemove;
        int index;
        boolean valid = true;

// ArrayIterable<T> iterable;

        public ArrayIterator (ExposedArray<T> exposedArray) {
            this(exposedArray, true);
        }

        public ArrayIterator (ExposedArray<T> exposedArray, boolean allowRemove) {
            this.exposedArray = exposedArray;
            this.allowRemove = allowRemove;
        }

        public boolean hasNext () {
            if (!valid) {
// System.out.println(iterable.lastAcquire);
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            return index < exposedArray.size;
        }

        public T next () {
            if (index >= exposedArray.size) throw new NoSuchElementException(String.valueOf(index));
            if (!valid) {
// System.out.println(iterable.lastAcquire);
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            return exposedArray.items[index++];
        }

        public void remove () {
            if (!allowRemove) throw new GdxRuntimeException("Remove not allowed.");
            index--;
            exposedArray.removeIndex(index);
        }

        public void reset () {
            index = 0;
        }

        public Iterator<T> iterator () {
            return this;
        }
    }

    static public class ArrayIterable<T> implements Iterable<T> {
        private final ExposedArray<T> exposedArray;
        private final boolean allowRemove;
        private ArrayIterator iterator1, iterator2;

// java.io.StringWriter lastAcquire = new java.io.StringWriter();

        public ArrayIterable (ExposedArray<T> exposedArray) {
            this(exposedArray, true);
        }

        public ArrayIterable (ExposedArray<T> exposedArray, boolean allowRemove) {
            this.exposedArray = exposedArray;
            this.allowRemove = allowRemove;
        }

        public Iterator<T> iterator () {
// lastAcquire.getBuffer().setLength(0);
// new Throwable().printStackTrace(new java.io.PrintWriter(lastAcquire));
            if (iterator1 == null) {
                iterator1 = new ArrayIterator(exposedArray, allowRemove);
                iterator2 = new ArrayIterator(exposedArray, allowRemove);
// iterator1.iterable = this;
// iterator2.iterable = this;
            }
            if (!iterator1.valid) {
                iterator1.index = 0;
                iterator1.valid = true;
                iterator2.valid = false;
                return iterator1;
            }
            iterator2.index = 0;
            iterator2.valid = true;
            iterator1.valid = false;
            return iterator2;
        }
    }
}