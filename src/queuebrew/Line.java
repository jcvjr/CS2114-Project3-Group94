package queuebrew;

import java.util.Arrays;

/**
 * Array-based FIFO queue of customers with a fixed capacity. Customers are kept
 * packed at the front of the array: index 0 is the front of the line, and
 * dequeue shifts everyone behind it forward by one.
 */
public class Line {

    private final Customer[] items;
    private int size;

    /**
     * Creates an empty line.
     *
     * @param capacity
     *            the maximum number of customers
     * @throws IllegalArgumentException
     *             if capacity is zero or negative
     */
    public Line(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        items = new Customer[capacity];
        size = 0;
    }

    /**
     * Adds a customer to the back of the line.
     *
     * @param customer
     *            the customer to add
     * @return true if added; false (and nothing changes) if the line is full or
     *         customer is null
     */
    public boolean enqueue(Customer customer) {
        if (customer == null || size == items.length) {
            return false;
        }
        items[size] = customer;
        size++;
        return true;
    }

    /**
     * Removes and returns the customer at the front of the line.
     *
     * @return the front customer, or null if the line is empty
     */
    public Customer dequeue() {
        if (size == 0) {
            return null;
        }
        Customer front = items[0];
        for (int i = 1; i < size; i++) {
            items[i - 1] = items[i];
        }
        size--;
        items[size] = null;
        return front;
    }

    /**
     * @return the front customer without removing them, or null if the line is
     *         empty
     */
    public Customer peek() {
        return size == 0 ? null : items[0];
    }

    /**
     * @return true if no one is in line
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * @return how many customers are in line
     */
    public int getSize() {
        return size;
    }

    /**
     * @return the maximum number of customers the line can hold
     */
    public int getCapacity() {
        return items.length;
    }

    /**
     * @return a copy of the customers in arrival order, front first
     */
    public Customer[] toArray() {
        return Arrays.copyOf(items, size);
    }
}
