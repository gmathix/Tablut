package model;


import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class MoveTest {

    @Mock TablutBoard board;
    @Mock Pawn pawn;

    @Test
    void move_noCapture_createdCorrectly() {
        when(board.getElement(2, 3)).thenReturn(pawn);
        when(pawn.getColor()).thenReturn(Pawn.PAWN_SOLDIER);
        when(board.checkCaptures(anyBoolean(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of());

        Move move = new Move(board, 3, 2, 5, 2);

        assertEquals(3, move.srcX());
        assertEquals(2, move.srcY());
        assertEquals(5, move.dstX());
        assertEquals(2, move.dstY());
        assertTrue(move.getCaptures().isEmpty());
    }
}
