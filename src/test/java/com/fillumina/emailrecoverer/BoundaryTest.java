package com.fillumina.emailrecoverer;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
public class BoundaryTest {

    @Test
    public void shouldRejectInvalidBoundaries() {
        assertFalse(Boundary.isValid("short"));
        assertFalse(Boundary.isValid("----------------"));
        assertFalse(Boundary.isValid("abcdefghkjlmnopqrstuvwxyz"));
        assertFalse(Boundary.isValid("-space in between"));
        assertFalse(Boundary.isValid("-not_7_bit_chars_è"));
        assertFalse(Boundary.isValid("-8.4(v)8.8(ed)-402.6(in)]TJ"));
        assertFalse(Boundary.isValid("-alt:"));
        assertFalse(Boundary.isValid(""));
    }

    @Test
    public void shouldAcceptValidBoundaries() {
        assertTrue(Boundary.isValid("--b1_b77a10ae4f560f0f5285e85c4064b8ce"));
        assertTrue(Boundary.isValid("--AltPart531185729bbd6--"));
        assertTrue(Boundary.isValid("--CCC6079EA0A.1393690145/mail.hurricane.it"));
        assertTrue(Boundary.isValid("--99957d0a4cad95bf6cff0929d704e86bbe09edaa"));
        assertTrue(Boundary.isValid("--1387814656.B2C86.20226--"));
        assertTrue(Boundary.isValid("--Boundary_(ID_Og4R3brzTOfgO/AFeihPzA)--"));
        assertTrue(Boundary.isValid("--_000_583AF1FC97082145A3207241E430E65101019A9992FAPCPSMBX01_--"));
        assertTrue(Boundary.isValid("----boundary_26910_5a2595cd-87d4-40a4-a760-59655890d566--"));
        assertTrue(Boundary.isValid("--boundaryTagForMixed--"));
        assertTrue(Boundary.isValid("----------MB_8CFEC52E23A10A7_C9C_B25E2_webmail-d185.sysops.aol.com"));
    }

    @Test
    public void shouldRecognizeCloseBoundary() {
        assertTrue(Boundary.isClose("--_000_583AF1FC97082145A3207241E430E65101019A9992FAPCPSMBX01_--"));
        assertTrue(Boundary.isClose("----boundary_26910_5a2595cd-87d4-40a4-a760-59655890d566--"));
        assertTrue(Boundary.isClose("--boundaryTagForMixed--"));
        assertTrue(Boundary.isClose("--df1edb1a9857266055560da51b37a009--"));
        assertTrue(Boundary.isClose("--b1_7814f27d677d5cee0815033e9fc92a63--"));
        assertTrue(Boundary.isClose("--part2_a0.be2e8d8.2dcac8da_boundary--"));
    }

    @Test
    public void shouldNotRecognizeOpenBoundary() {
        assertFalse(Boundary.isClose("--b1_b77a10ae4f560f0f5285e85c4064b8ce"));
        assertFalse(Boundary.isClose("--CCC6079EA0A.1393690145/mail.hurricane.it"));
        assertFalse(Boundary.isClose("--99957d0a4cad95bf6cff0929d704e86bbe09edaa"));
        assertFalse(Boundary.isClose("----------MB_8CFEC52E23A10A7_C9C_B25E2_webmail-d185.sysops.aol.com"));
    }

    @Test
    public void shouldRemoveTheClosingSymbol() {
        assertEquals("--_000_583AF1FC97082145A3207241E430E65101019A9992FAPCPSMBX01_",
                Boundary.removeCloseSimbol("--_000_583AF1FC97082145A3207241E430E65101019A9992FAPCPSMBX01_--"));
        assertEquals("----boundary_26910_5a2595cd-87d4-40a4-a760-59655890d566",
                Boundary.removeCloseSimbol("----boundary_26910_5a2595cd-87d4-40a4-a760-59655890d566--"));
        assertEquals("--boundaryTagForMixed",
                Boundary.removeCloseSimbol("--boundaryTagForMixed--"));
        assertEquals("--df1edb1a9857266055560da51b37a009",
                Boundary.removeCloseSimbol("--df1edb1a9857266055560da51b37a009--"));
        assertEquals("--part2_a0.be2e8d8.2dcac8da_boundary",
                Boundary.removeCloseSimbol("--part2_a0.be2e8d8.2dcac8da_boundary--"));
    }
}
