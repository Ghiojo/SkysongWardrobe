package org.skysongdev.skysongwardrobe.menu;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GalleryPagesTest {
    private final List<Integer> hundred = IntStream.range(0, 100).boxed().toList();

    @Test
    void countsPagesOf45() {
        assertEquals(1, GalleryPages.count(0));
        assertEquals(1, GalleryPages.count(45));
        assertEquals(2, GalleryPages.count(46));
        assertEquals(3, GalleryPages.count(100));
    }

    @Test
    void slicesPages() {
        assertEquals(45, GalleryPages.page(hundred, 0).size());
        assertEquals(45, GalleryPages.page(hundred, 1).getFirst());
        assertEquals(10, GalleryPages.page(hundred, 2).size());
        assertEquals(List.of(), GalleryPages.page(hundred, 5));
    }
}
