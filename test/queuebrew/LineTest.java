package queuebrew;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the array-based Line, with no game or GUI involved.
 */
public class LineTest {

    private Line line;
    private Customer alex;
    private Customer sam;
    private Customer mia;

    @BeforeEach
    public void setUp() {
        line = new Line(6);
        alex = new Customer("Alex", new Order("Latte"));
        sam = new Customer("Sam", new Order("Mocha"));
        mia = new Customer("Mia", new Order("Espresso"));
    }

    @Test
    public void newLineIsEmptyWithCapacity() {
        assertTrue(line.isEmpty());
        assertEquals(0, line.getSize());
        assertEquals(6, line.getCapacity());
    }

    @Test
    public void constructorRejectsBadCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new Line(0));
        assertThrows(IllegalArgumentException.class, () -> new Line(-3));
    }

    @Test
    public void enqueueKeepsArrivalOrder() {
        assertTrue(line.enqueue(alex));
        assertTrue(line.enqueue(sam));
        assertTrue(line.enqueue(mia));
        assertEquals(3, line.getSize());
        assertFalse(line.isEmpty());
        assertArrayEquals(new Customer[] { alex, sam, mia }, line.toArray());
    }

    @Test
    public void enqueueRejectsNull() {
        assertFalse(line.enqueue(null));
        assertEquals(0, line.getSize());
    }

    @Test
    public void enqueueRejectsWhenFull() {
        for (int i = 0; i < 6; i++) {
            assertTrue(line.enqueue(alex));
        }
        assertFalse(line.enqueue(sam));
        assertEquals(6, line.getSize());
        assertEquals(6, line.getCapacity());
        for (Customer c : line.toArray()) {
            assertSame(alex, c);
        }
    }

    @Test
    public void dequeueReturnsFrontAndShifts() {
        line.enqueue(alex);
        line.enqueue(sam);
        assertSame(alex, line.dequeue());
        assertEquals(1, line.getSize());
        assertSame(sam, line.peek());
    }

    @Test
    public void dequeueOnEmptyReturnsNull() {
        assertNull(line.dequeue());
        assertTrue(line.isEmpty());
    }

    @Test
    public void fifoSurvivesWrapAroundUse() {
        for (int i = 0; i < 6; i++) {
            line.enqueue(alex);
        }
        line.dequeue();
        assertTrue(line.enqueue(sam));
        assertSame(sam, line.toArray()[5]);
        for (int i = 0; i < 5; i++) {
            assertSame(alex, line.dequeue());
        }
        assertSame(sam, line.dequeue());
        assertNull(line.dequeue());
    }

    @Test
    public void peekDoesNotRemove() {
        line.enqueue(alex);
        line.enqueue(sam);
        assertSame(alex, line.peek());
        assertEquals(2, line.getSize());
    }

    @Test
    public void peekOnEmptyReturnsNull() {
        assertNull(line.peek());
        assertTrue(line.isEmpty());
    }

    @Test
    public void clearEmptiesTheLineAndItCanBeReused() {
        line.enqueue(alex);
        line.enqueue(sam);
        line.clear();
        assertTrue(line.isEmpty());
        assertEquals(0, line.toArray().length);
        assertNull(line.peek());
        assertEquals(6, line.getCapacity());
        assertTrue(line.enqueue(mia));
        assertSame(mia, line.peek());
    }

    @Test
    public void clearOnEmptyLineIsHarmless() {
        line.clear();
        assertTrue(line.isEmpty());
    }

    @Test
    public void toArrayIsACopy() {
        line.enqueue(alex);
        line.enqueue(sam);
        Customer[] copy = line.toArray();
        copy[0] = mia;
        assertArrayEquals(new Customer[] { alex, sam }, line.toArray());
    }
}
