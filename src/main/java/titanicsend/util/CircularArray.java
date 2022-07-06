package titanicsend.util;

import java.lang.reflect.Array;

public class CircularArray<T> {
    /*
            This class is a constant size array. As you add elements, they are appended to the end.

            Very simple -- there is no way to remove elements. They just get overwritten with time.

            Nice for small, constant time access latency and memory usage.
     */
    private final Class<T> clazz;
    private T[] buf; // array of actual data
    private int start; // where the most recent element is held
    private int end; // where the oldest element is held
    private int capacity; // most elements this list can hold
    private int size; // number of elements added to list

    public CircularArray(Class<T> clazz, int capacity) {
        this.clazz = clazz;
        this.capacity = capacity;
        this.clear();
    }

    public void add(T element) {
        assert element != null : "Cannot add null to CircularArray!";

        start = (start + 1) % capacity;
        buf[start] = element;

        if (size < capacity)
            size += 1;
        else if (size == capacity)
            end = (end + 1) % capacity;

        assert size <= capacity : "Cannot have size larger than caapcity!";
        assert (end + size) % capacity - 1 == start : "Invariant violated!";
        //System.out.printf("start=%d, end=%d, size=%d, capacity=%d\n", start, end, size, capacity);
    }

    /*
        Retrieve the most recently added element.
     */
    public T get() throws IndexOutOfBoundsException {
        if (size == 0)
            throw new IndexOutOfBoundsException("No elements have been added to this CircularArray yet!");
        return get(0);
    }

    /*
        Retrieve an arbitrary element by index. Indexing starts at 0 (most recent) and goes NEGATIVE to get
        previously added elements!

        So as an example:
            circArray.get(-3) -> 4th most recent element added, if it exists

        Positive integer indices have no meaning and throw an exception.
     */
    public T get(int idx) throws IndexOutOfBoundsException {
        if (idx > 0) {
            throw new IndexOutOfBoundsException(
                    "Cannot read from index=" +Integer.toString(idx)+ ", please use integer index <= 0\n");
        } else if (size == 0) {
            throw new IndexOutOfBoundsException("No elements have been added to this CircularArray yet!");
        } else if (Math.abs(idx) >= size) {
            throw new IndexOutOfBoundsException("Index out of range\n");
        }
        int wrappedIndex = (start + idx) % capacity;
        return buf[wrappedIndex];
    }

    /*
        If you'd like to get the entire array, but in order, you can do so with this method.
     */
    public T[] getAll() {
        T[] inOrderBuf = (T[]) Array.newInstance(clazz, size);
        int writeIdx = 0;
        int readIdx = start;
        int toAdd = size;

        while (toAdd > 0) {
            assert buf[readIdx] != null : "Should never return a null element to user!";
            inOrderBuf[writeIdx] = buf[readIdx];
            writeIdx++;

            readIdx = (readIdx - 1);
            if (readIdx == -1)
                readIdx = capacity - 1;

            toAdd--;
        }
        return inOrderBuf;
    }

    public int getSize() {
        return size;
    }

    public int getCapacity() {
        return capacity;
    }

    public void clear() {
        this.buf = (T[]) Array.newInstance(clazz, capacity);
        this.start = -1;
        this.end = 0;
        this.size = 0;
    }
}
