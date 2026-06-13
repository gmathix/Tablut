package model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;


public class CaptureTest {

    @Test
    void capture_storedCorrectly() {
        Capture c = new Capture(3, 5, Pawn.PAWN_MOSCOVITE);
        assertEquals(3, c.x());
        assertEquals(5, c.y());
        assertEquals(Pawn.PAWN_MOSCOVITE, c.piece());
    }

    @Test
    void capture_equality() {
        Capture c1 = new Capture(1, 2, Pawn.PAWN_SOLDIER);
        Capture c2 = new Capture(1, 2, Pawn.PAWN_SOLDIER);
        assertEquals(c1, c2);
    }
}
