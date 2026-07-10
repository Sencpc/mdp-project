package mad.project.mdp_project.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.util.Calendar

/**
 * Custom View that renders a 7-day sleep bar chart with a dotted baseline line.
 * Each bar represents one day's total sleep hours (Mon–Sun).
 * Today's bar is highlighted with the app's primary color.
 */
class SleepChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dayLabels = arrayOf("M", "T", "W", "T", "F", "S", "S")

    // Map of dayOfWeek (Calendar.MONDAY=2 .. Calendar.SUNDAY=1) to total sleep hours
    private var dailySleep: Map<Int, Double> = emptyMap()
    private var baseline: Double = 7.0
    private var todayDayOfWeek: Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0")
    }

    private val todayBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#004B4F")
    }

    private val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF6B6B")
        strokeWidth = 3f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }

    private val baselineLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF6B6B")
        textSize = 28f
        textAlign = Paint.Align.RIGHT
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#757575")
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }

    /**
     * Set chart data.
     * @param sleep Map of Calendar day-of-week constant to total sleep hours for that day
     * @param baselineHours The recommended daily sleep hours (dotted line)
     */
    fun setData(sleep: Map<Int, Double>, baselineHours: Double) {
        dailySleep = sleep
        baseline = baselineHours
        todayDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (180 * resources.displayMetrics.density).toInt()
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val density = resources.displayMetrics.density

        val labelHeight = 32f * density
        val topPadding = 12f * density
        val chartHeight = h - labelHeight - topPadding
        val barCount = 7
        val barSpacing = 8f * density
        val totalSpacing = barSpacing * (barCount - 1)
        val barWidth = (w - totalSpacing - 16f * density) / barCount
        val startX = 8f * density

        val maxSleep = maxOf(
            dailySleep.values.maxOrNull() ?: 0.0,
            baseline * 1.3,
            1.0
        )

        val calDays = intArrayOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        )

        for (i in 0 until barCount) {
            val calDay = calDays[i]
            val sleepVal = dailySleep[calDay] ?: 0.0
            val barHeight = if (sleepVal > 0) (sleepVal.toFloat() / maxSleep.toFloat() * chartHeight) else (2f * density)

            val x = startX + i * (barWidth + barSpacing)
            val barTop = topPadding + chartHeight - barHeight
            val rect = RectF(x, barTop, x + barWidth, topPadding + chartHeight)
            val cornerRadius = 6f * density

            val paint = if (calDay == todayDayOfWeek) todayBarPaint else barPaint
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

            if (sleepVal > 0) {
                // Round to 1 decimal place
                val text = String.format(java.util.Locale.getDefault(), "%.1f", sleepVal)
                canvas.drawText(
                    text,
                    x + barWidth / 2,
                    barTop - 4f * density,
                    valuePaint
                )
            }

            canvas.drawText(
                dayLabels[i],
                x + barWidth / 2,
                h - 4f * density,
                labelPaint
            )
        }

        val average = if (dailySleep.isNotEmpty()) dailySleep.values.average() else 0.0
        if (average >= baseline) {
            baselinePaint.color = Color.parseColor("#4CAF50")
            baselineLabelPaint.color = Color.parseColor("#4CAF50")
        } else {
            baselinePaint.color = Color.parseColor("#FF6B6B")
            baselineLabelPaint.color = Color.parseColor("#FF6B6B")
        }

        val baselineY = topPadding + chartHeight - (baseline.toFloat() / maxSleep.toFloat() * chartHeight)
        if (baselineY > topPadding && baselineY < topPadding + chartHeight) {
            canvas.drawLine(startX, baselineY, w - startX, baselineY, baselinePaint)

            canvas.drawText(
                String.format(java.util.Locale.getDefault(), "%.1f", baseline),
                w - startX,
                baselineY - 6f * density,
                baselineLabelPaint
            )
        }
    }
}
