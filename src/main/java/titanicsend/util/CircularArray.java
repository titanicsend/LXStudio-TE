package titanicsend.util;

public class CircularArray {
    /*
        This class is a constant size array. As you add elements, they are appended to the end.

        Very simple -- there is no way to remove elements. They just get overwritten with time.

        Nice for small, constant time access latency and memory usage.
     */
    private float[] buf; // array of actual float data
    private int head; // where the most recent element is held
    private int tail; // where the oldest element is held
    private int capacity; // most elements this list can hold
    private int size; // number of elements added to list

    public CircularArray(int capacity) {
        this.capacity = capacity;
        this.buf = new float[capacity];
        this.head = 0;
        this.tail = -1;
        this.size = 0;
    }

    public void add(float f) {
        buf[head] = f;
        if (size < capacity) size += 1;
        head = (head + 1) % capacity;
        tail = (tail + 1) % capacity;
    }

    public float get() throws Exception {
        if (size == 0)
            throw new Exception("No elements have been added to this CircularArray yet!");
        return buf[head];
    }

    public float get(int idx) throws IndexOutOfBoundsException {
        if (idx > 0) {
            throw new IndexOutOfBoundsException(
                    "Cannot read from index=" +Integer.toString(idx)+ ", please use integer index <= 0\n");
        }
        int wrappedIndex = (head + 1) % capacity;
        return buf[wrappedIndex];
    }

    public float[] getAll() {
        float[] inOrderBuf = new float[this.size];
        int writeIdx = 0;
        int readIdx = head;
        int toAdd = size;
        while (toAdd > 0) {
            inOrderBuf[writeIdx] = buf[readIdx];
            writeIdx++;
            readIdx = (readIdx + 1) % capacity;
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
}
