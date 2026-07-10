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
 * Custom View that renders a 7-day calorie bar chart with a dotted baseline line.
 * Each bar represents one day's total calories (Mon–Sun).
 * Today's bar is highlighted with the app's primary color.
 */
class CalorieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dayLabels = arrayOf("M", "T", "W", "T", "F", "S", "S")

    // Map of dayOfWeek (Calendar.MONDAY=2 .. Calendar.SUNDAY=1) to total calories
    private var dailyCalories: Map<Int, Int> = emptyMap()
    private var baseline: Int = 2000
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
     * @param calories Map of Calendar day-of-week constant to total calories for that day
     * @param baselineCalories The recommended daily calorie intake (dotted line)
     */
    fun setData(calories: Map<Int, Int>, baselineCalories: Int) {
        dailyCalories = calories
        baseline = baselineCalories
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

        val labelHeight = 32f * density // space for day labels at bottom
        val topPadding = 12f * density // space for value labels on top
        val chartHeight = h - labelHeight - topPadding
        val barCount = 7
        val barSpacing = 8f * density
        val totalSpacing = barSpacing * (barCount - 1)
        val barWidth = (w - totalSpacing - 16f * density) / barCount // 16dp total side padding
        val startX = 8f * density

        // Determine the max value for scaling (at least baseline * 1.3 to leave room)
        val maxCalories = maxOf(
            dailyCalories.values.maxOrNull() ?: 0,
            (baseline * 1.3).toInt(),
            1 // prevent division by zero
        )

        // Day-of-week mapping: Monday=0, Tuesday=1, ..., Sunday=6
        // Calendar: MONDAY=2, TUESDAY=3, ..., SATURDAY=7, SUNDAY=1
        val calDays = intArrayOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        )

        // Draw bars
        for (i in 0 until barCount) {
            val calDay = calDays[i]
            val cal = dailyCalories[calDay] ?: 0
            val barHeight = if (cal > 0) (cal.toFloat() / maxCalories * chartHeight) else (2f * density)

            val x = startX + i * (barWidth + barSpacing)
            val barTop = topPadding + chartHeight - barHeight
            val rect = RectF(x, barTop, x + barWidth, topPadding + chartHeight)
            val cornerRadius = 6f * density

            val paint = if (calDay == todayDayOfWeek) todayBarPaint else barPaint
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

            // Draw calorie value above bar (only if > 0)
            if (cal > 0) {
                canvas.drawText(
                    cal.toString(),
                    x + barWidth / 2,
                    barTop - 4f * density,
                    valuePaint
                )
            }

            // Draw day label below bar
            canvas.drawText(
                dayLabels[i],
                x + barWidth / 2,
                h - 4f * density,
                labelPaint
            )
        }

        // Draw baseline dotted line
        val baselineY = topPadding + chartHeight - (baseline.toFloat() / maxCalories * chartHeight)
        if (baselineY > topPadding && baselineY < topPadding + chartHeight) {
            canvas.drawLine(startX, baselineY, w - startX, baselineY, baselinePaint)

            // Draw baseline label
            canvas.drawText(
                "${baseline}",
                w - startX,
                baselineY - 6f * density,
                baselineLabelPaint
            )
        }
    }
}
