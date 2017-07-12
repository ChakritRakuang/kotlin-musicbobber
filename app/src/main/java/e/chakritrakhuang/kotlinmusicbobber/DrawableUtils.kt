package e.chakritrakhuang.kotlinmusicbobber

internal object DrawableUtils {

    fun customFunction(t : Float , vararg pairs : Float) : Float {
        if (pairs.isEmpty() || pairs.size % 2 != 0) {
            throw IllegalArgumentException("Length of pairs must be multiple by 2 and greater than zero.")
        }
        if (t < pairs[1]) {
            return pairs[0]
        }
        val size = pairs.size / 2
        for (i in 0 until size - 1) {
            val a = pairs[2 * i]
            val b = pairs[2 * (i + 1)]
            val aT = pairs[2 * i + 1]
            val bT = pairs[2 * (i + 1) + 1]
            if (t in aT .. bT) {
                val norm = normalize(t , aT , bT)
                return a + norm * (b - a)
            }
        }
        return pairs[pairs.size - 2]
    }

    fun normalize(`val` : Float , minVal : Float , maxVal : Float) : Float {
        if (`val` < minVal)
            return 0f
        return if (`val` > maxVal) 1f else (`val` - minVal) / (maxVal - minVal)
    }

    fun rotateX(pX : Float , pY : Float , cX : Float , cY : Float , angleInDegrees : Float) : Float {
        val angle = Math.toRadians(angleInDegrees.toDouble())
        return (Math.cos(angle) * (pX - cX) - Math.sin(angle) * (pY - cY) + cX).toFloat()
    }

    fun rotateY(pX : Float , pY : Float , cX : Float , cY : Float , angleInDegrees : Float) : Float {
        val angle = Math.toRadians(angleInDegrees.toDouble())
        return (Math.sin(angle) * (pX - cX) + Math.cos(angle) * (pY - cY) + cY.toDouble()).toFloat()
    }

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

    fun enlarge(startValue : Float , endValue : Float , time : Float) : Float {
        if (startValue > endValue)
            throw IllegalArgumentException("Start size can't be larger than end size.")
        return startValue + (endValue - startValue) * time
    }

    fun reduce(startValue : Float , endValue : Float , time : Float) : Float {
        if (startValue < endValue)
            throw IllegalArgumentException("End size can't be larger than start size.")
        return endValue + (startValue - endValue) * (1 - time)
    }

    fun smooth(prevValue : Float , newValue : Float , a : Float) : Float {
        return a * newValue + (1 - a) * prevValue
    }

}