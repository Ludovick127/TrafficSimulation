import org.junit.Test;

import java.awt.Color;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the color blending utilities in {@link TrafficSimulation}.
 */
public class TrafficSimulationTest {

    private static final float DELTA = 1e-6f;

    @Test
    public void blendWithRatioOneReturnsFirstColor() {
        Color result = TrafficSimulation.blend(Color.RED, Color.BLUE, 1.0);
        assertEquals(Color.RED, result);
    }

    @Test
    public void blendWithRatioZeroReturnsSecondColor() {
        Color result = TrafficSimulation.blend(Color.RED, Color.BLUE, 0.0);
        assertEquals(Color.BLUE, result);
    }

    @Test
    public void blendWithHalfRatioMixesColors() {
        Color result = TrafficSimulation.blend(Color.BLACK, Color.WHITE, 0.5);
        float[] rgb = result.getColorComponents(null);
        assertEquals(0.5f, rgb[0], 0.01f);
        assertEquals(0.5f, rgb[1], 0.01f);
        assertEquals(0.5f, rgb[2], 0.01f);
    }

    @Test
    public void getFractionIndicesReturnsSurroundingIndices() {
        float[] fractions = {0f, 0.33f, 0.34f, 0.66f, 0.67f, 1f};
        assertArrayEquals(new int[]{0, 1}, TrafficSimulation.getFractionIndices(fractions, 0.1f));
        assertArrayEquals(new int[]{2, 3}, TrafficSimulation.getFractionIndices(fractions, 0.5f));
        assertArrayEquals(new int[]{4, 5}, TrafficSimulation.getFractionIndices(fractions, 1f));
    }

    @Test(expected = IllegalArgumentException.class)
    public void blendColorsRejectsNullFractions() {
        TrafficSimulation.blendColors(null, TrafficSimulation.COLORS, 0.5f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void blendColorsRejectsMismatchedLengths() {
        TrafficSimulation.blendColors(new float[]{0f, 1f}, new Color[]{Color.RED}, 0.5f);
    }

    @Test
    public void blendColorsProducesColorWithinRange() {
        Color result = TrafficSimulation.blendColors(TrafficSimulation.FRACTIONS, TrafficSimulation.COLORS, 0.5f);
        float[] rgb = result.getColorComponents(null);
        for (float component : rgb) {
            assertEquals(true, component >= 0f - DELTA && component <= 1f + DELTA);
        }
    }
}
