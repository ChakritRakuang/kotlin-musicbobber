package e.chakritrakhuang.kotlinmusicbobber

/**
 * Helpful utils class.
 */
internal object DrawableUtils {

    fun customFunction(t : Float , vararg pairs : Float) : Float {
        if (pairs.size == 0 || pairs.size % 2 != 0) {
            throw IllegalArgumentException("Length of pairs must be multiple by 2 and greater than zero.")
        }
        if (t < pairs[1]) {
            return pairs[0]
        }
        val size = pairs.size / 2
        for (i in 0 .. size - 1 - 1) {
            val a = pairs[2 * i]
            val b = pairs[2 * (i + 1)]
            val aT = pairs[2 * i + 1]
            val bT = pairs[2 * (i + 1) + 1]
            if (t >= aT && t <= bT) {
                val norm = normalize(t , aT , bT)
                return a + norm * (b - a)
            }
        }
        return pairs[pairs.size - 2]
    }

    /**
     * Normalize value between minimum and maximum.
     * @param val value
     * @param minVal minimum value
     * @param maxVal maximum value
     * @return normalized value in range `0..1`
     * @throws IllegalArgumentException if value is out of range `[minVal, maxVal]`
     */
    fun normalize(`val` : Float , minVal : Float , maxVal : Float) : Float {
        if (`val` < minVal)
            return 0f
        return if (`val` > maxVal) 1f else (`val` - minVal) / (maxVal - minVal)
    }

    /**
     * Rotate point P around center point C.
     * @param pX x coordinate of point P
     * @param pY y coordinate of point P
     * @param cX x coordinate of point C
     * @param cY y coordinate of point C
     * @param angleInDegrees rotation angle in degrees
     * @return new x coordinate
     */
    fun rotateX(pX : Float , pY : Float , cX : Float , cY : Float , angleInDegrees : Float) : Float {
        val angle = Math.toRadians(angleInDegrees.toDouble())
        return (Math.cos(angle) * (pX - cX) - Math.sin(angle) * (pY - cY) + cX).toFloat()
    }

    /**
     * Rotate point P around center point C.
     * @param pX x coordinate of point P
     * @param pY y coordinate of point P
     * @param cX x coordinate of point C
     * @param cY y coordinate of point C
     * @param angleInDegrees rotation angle in degrees
     * @return new y coordinate
     */
    fun rotateY(pX : Float , pY : Float , cX : Float , cY : Float , angleInDegrees : Float) : Float {
        val angle = Math.toRadians(angleInDegrees.toDouble())
        return (Math.sin(angle) * (pX - cX) + Math.cos(angle) * (pY - cY) + cY.toDouble()).toFloat()
    }

    /**
     * Checks if value belongs to range `[start, end]`
     * @param value value
     * @param start start of range
     * @param end end of range
     * @return true if value belongs to range, false otherwise
     */
    fun isBetween(value : Float , start : Float , end : Float) : Boolean {
        var start = start
        var end = end
        if (start > end) {
            val tmp = start
            start = end
            end = tmp
        }
        return value >= start && value <= end
    }

    fun between(`val` : Float , min : Float , max : Float) : Float {
        return Math.min(Math.max(`val` , min) , max)
    }

    fun between(`val` : Int , min : Int , max : Int) : Int {
        return Math.min(Math.max(`val` , min) , max)
    }

    /**
     * Enlarge value from startValue to endValue
     * @param startValue start size
     * @param endValue end size
     * @param time time of animation
     * @return new size value
     */
    fun enlarge(startValue : Float , endValue : Float , time : Float) : Float {
        if (startValue > endValue)
            throw IllegalArgumentException("Start size can't be larger than end size.")
        return startValue + (endValue - startValue) * time
    }

    /**
     * Reduce value from startValue to endValue
     * @param startValue start size
     * @param endValue end size
     * @param time time of animation
     * @return new size value
     */
    fun reduce(startValue : Float , endValue : Float , time : Float) : Float {
        if (startValue < endValue)
            throw IllegalArgumentException("End size can't be larger than start size.")
        return endValue + (startValue - endValue) * (1 - time)
    }

    /**
     * Exponential smoothing (Holt - Winters).
     * @param prevValue previous values in series `X[i-1]`
     * @param newValue new value in series `X[i]`
     * @param a smooth coefficient
     * @return smoothed value
     */
    fun smooth(prevValue : Float , newValue : Float , a : Float) : Float {
        return a * newValue + (1 - a) * prevValue
    }

}
