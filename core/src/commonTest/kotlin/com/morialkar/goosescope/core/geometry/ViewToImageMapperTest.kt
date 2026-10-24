package com.morialkar.goosescope.core.geometry

import com.morialkar.goosescope.core.sample.Point
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ViewToImageMapperTest {
    @Test
    fun `center of the view is the center of the image`() {
        assertEquals(Point(640, 360), ViewToImageMapper.map(540f, 1200f, 1080, 2400, 1280, 720))
        assertEquals(Point(640, 360), ViewToImageMapper.map(1200f, 540f, 2400, 1080, 1280, 720))
    }

    @Test
    fun `portrait view crops the sides of a landscape image`() {
        // Vue 1000×2000, image 1280×720: échelle 2000/720, l'image déborde à gauche et à droite.
        val left = checkNotNull(ViewToImageMapper.map(0f, 1000f, 1000, 2000, 1280, 720))
        val right = checkNotNull(ViewToImageMapper.map(999f, 1000f, 1000, 2000, 1280, 720))
        assertEquals(460, left.x)
        assertEquals(819, right.x)
        assertEquals(360, left.y)
    }

    @Test
    fun `view wider than the image crops top and bottom`() {
        val top = checkNotNull(ViewToImageMapper.map(500f, 0f, 1000, 200, 100, 100))
        assertEquals(Point(50, 40), top)
    }

    @Test
    fun `round trips through toView`() {
        val p = Point(300, 500)
        val (vx, vy) = ViewToImageMapper.toView(p, 1080, 2400, 720, 1280)
        assertEquals(p, ViewToImageMapper.map(vx, vy, 1080, 2400, 720, 1280))
    }

    @Test
    fun `tap outside the view is ignored`() {
        assertNull(ViewToImageMapper.map(-1f, 10f, 100, 100, 100, 100))
        assertNull(ViewToImageMapper.map(10f, 100f, 100, 100, 100, 100))
    }

    @Test
    fun `rejects empty dimensions`() {
        assertFailsWith<IllegalArgumentException> { ViewToImageMapper.map(1f, 1f, 0, 100, 100, 100) }
    }
}
