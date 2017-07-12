package e.chakritrakhuang.kotlinmusicbobber

import android.graphics.Color

/**
 * Helper class for changing color.
 */
internal class ColorChanger {

    private val fromColorHsv : FloatArray
    private val toColorHsv : FloatArray
    private val resultColorHsv : FloatArray

    init {
        fromColorHsv = FloatArray(3)
        toColorHsv = FloatArray(3)
        resultColorHsv = FloatArray(3)
    }

    fun fromColor(fromColor : Int) : ColorChanger {
        Color.colorToHSV(fromColor , fromColorHsv)
        return this
    }

    fun toColor(toColor : Int) : ColorChanger {
        Color.colorToHSV(toColor , toColorHsv)
        return this
    }

    fun nextColor(dt : Float) : Int {
        for (k in 0 .. 2) {
            resultColorHsv[k] = fromColorHsv[k] + (toColorHsv[k] - fromColorHsv[k]) * dt
        }
        return Color.HSVToColor(resultColorHsv)
    }
}
