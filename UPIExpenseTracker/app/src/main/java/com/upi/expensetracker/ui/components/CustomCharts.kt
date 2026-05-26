package com.upi.expensetracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upi.expensetracker.ui.theme.Divider
import com.upi.expensetracker.ui.theme.GradientEnd
import com.upi.expensetracker.ui.theme.GradientMid
import com.upi.expensetracker.ui.theme.GradientStart
import com.upi.expensetracker.ui.theme.PrimaryMuted
import com.upi.expensetracker.ui.theme.PrimaryPink
import com.upi.expensetracker.ui.theme.PrimaryViolet
import com.upi.expensetracker.ui.theme.TextMuted
import com.upi.expensetracker.ui.theme.TextSecondary
import kotlin.math.max

@Composable
fun SpendBarChart(
    dailySpends: List<Double>,
    modifier: Modifier = Modifier.fillMaxWidth().height(180.dp)
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = TextMuted, fontSize = 10.sp)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingLeft = 40f
        val paddingBottom = 40f
        val chartWidth = width - paddingLeft
        val chartHeight = height - paddingBottom

        if (dailySpends.isEmpty()) {
            drawText(
                textMeasurer = textMeasurer,
                text = "No data available",
                style = TextStyle(color = TextSecondary, fontSize = 14.sp),
                topLeft = Offset(width / 2f - 60f, height / 2f - 20f)
            )
            return@Canvas
        }

        val maxSpend = max(dailySpends.maxOrNull() ?: 1.0, 1.0)

        // Draw horizontal grid lines (3 lines)
        val gridLines = 3
        for (i in 0..gridLines) {
            val y = chartHeight - (chartHeight / gridLines) * i
            drawLine(
                color = Divider,
                start = Offset(paddingLeft, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
            // Draw grid Y labels
            val labelVal = (maxSpend / gridLines * i).toInt()
            drawText(
                textMeasurer = textMeasurer,
                text = "₹$labelVal",
                style = labelStyle,
                topLeft = Offset(5f, y - 15f)
            )
        }

        // Draw bars with multi-color gradient (violet → coral)
        val barCount = dailySpends.size
        val spacing = chartWidth / barCount
        val barWidth = spacing * 0.6f

        for (i in 0 until barCount) {
            val spend = dailySpends[i]
            val barHeight = (spend / maxSpend) * chartHeight
            val x = paddingLeft + (spacing * i) + (spacing - barWidth) / 2f
            val y = chartHeight - barHeight

            if (spend > 0) {
                // Multi-color gradient for bars — Violet → Pink → Coral
                val brush = Brush.verticalGradient(
                    colors = listOf(GradientStart, GradientMid, GradientEnd),
                    startY = y.toFloat(),
                    endY = chartHeight
                )
                
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(x, y.toFloat()),
                    size = Size(barWidth, barHeight.toFloat()),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun TrendLineChart(
    monthlySpends: List<Pair<String, Double>>,
    modifier: Modifier = Modifier.fillMaxWidth().height(180.dp)
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = TextMuted, fontSize = 10.sp)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingLeft = 50f
        val paddingBottom = 40f
        val chartWidth = width - paddingLeft
        val chartHeight = height - paddingBottom

        if (monthlySpends.isEmpty()) {
            drawText(
                textMeasurer = textMeasurer,
                text = "No trend data",
                style = TextStyle(color = TextSecondary, fontSize = 14.sp),
                topLeft = Offset(width / 2f - 40f, height / 2f - 20f)
            )
            return@Canvas
        }

        val maxSpend = max(monthlySpends.map { it.second }.maxOrNull() ?: 1.0, 1.0)

        // Draw Grid Lines
        val gridLines = 3
        for (i in 0..gridLines) {
            val y = chartHeight - (chartHeight / gridLines) * i
            drawLine(
                color = Divider,
                start = Offset(paddingLeft, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
            val labelVal = (maxSpend / gridLines * i).toInt()
            drawText(
                textMeasurer = textMeasurer,
                text = "₹$labelVal",
                style = labelStyle,
                topLeft = Offset(5f, y - 15f)
            )
        }

        val pointsCount = monthlySpends.size
        val xSpacing = chartWidth / (pointsCount - 1).coerceAtLeast(1)
        val points = monthlySpends.mapIndexed { index, pair ->
            val x = paddingLeft + index * xSpacing
            val y = chartHeight - ((pair.second / maxSpend) * chartHeight)
            Offset(x, y.toFloat())
        }

        // Draw background gradient fill under line path (violet → transparent)
        if (points.isNotEmpty()) {
            val fillPath = Path().apply {
                moveTo(points.first().x, chartHeight)
                for (i in points.indices) {
                    val p = points[i]
                    if (i == 0) {
                        lineTo(p.x, p.y)
                    } else {
                        // Smooth Bezier line to the next point
                        val prev = points[i - 1]
                        val controlX = (prev.x + p.x) / 2f
                        cubicTo(controlX, prev.y, controlX, p.y, p.x, p.y)
                    }
                }
                lineTo(points.last().x, chartHeight)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PrimaryViolet.copy(alpha = 0.4f),
                        PrimaryPink.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = chartHeight
                )
            )

            // Draw gradient stroke path
            val strokePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val p = points[i]
                    val prev = points[i - 1]
                    val controlX = (prev.x + p.x) / 2f
                    cubicTo(controlX, prev.y, controlX, p.y, p.x, p.y)
                }
            }

            drawPath(
                path = strokePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(PrimaryViolet, PrimaryPink, GradientEnd)
                ),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw points with glow rings
            for (i in points.indices) {
                val p = points[i]
                // Outer glow ring
                drawCircle(
                    color = PrimaryViolet.copy(alpha = 0.2f),
                    radius = 6.dp.toPx(),
                    center = p
                )
                // Middle ring
                drawCircle(
                    color = PrimaryMuted,
                    radius = 4.dp.toPx(),
                    center = p
                )
                // Inner dot
                drawCircle(
                    color = PrimaryPink,
                    radius = 2.5.dp.toPx(),
                    center = p
                )

                // Draw X Label (Month e.g. "Jun")
                val labelText = monthlySpends[i].first
                drawText(
                    textMeasurer = textMeasurer,
                    text = labelText,
                    style = labelStyle,
                    topLeft = Offset(p.x - 20f, chartHeight + 10f)
                )
            }
        }
    }
}
