package com.upi.expensetracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upi.expensetracker.ui.theme.Accent
import com.upi.expensetracker.ui.theme.AccentDim
import com.upi.expensetracker.ui.theme.Divider
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
            drawText(textMeasurer, "No data available", TextStyle(color = TextSecondary, fontSize = 14.sp), Offset(width / 2f - 60f, height / 2f - 20f))
            return@Canvas
        }

        val maxSpend = max(dailySpends.maxOrNull() ?: 1.0, 1.0)

        // Grid lines
        val gridLines = 3
        for (i in 0..gridLines) {
            val y = chartHeight - (chartHeight / gridLines) * i
            drawLine(Divider, Offset(paddingLeft, y), Offset(width, y), 1f)
            drawText(textMeasurer, "₹${(maxSpend / gridLines * i).toInt()}", labelStyle, Offset(2f, y - 15f))
        }

        // Bars — single teal color, rounded tops
        val barCount = dailySpends.size
        val spacing = chartWidth / barCount
        val barWidth = spacing * 0.6f

        for (i in 0 until barCount) {
            val spend = dailySpends[i]
            val barHeight = (spend / maxSpend) * chartHeight
            val x = paddingLeft + (spacing * i) + (spacing - barWidth) / 2f
            val y = chartHeight - barHeight

            if (spend > 0) {
                drawRoundRect(
                    color = Accent,
                    topLeft = Offset(x, y.toFloat()),
                    size = Size(barWidth, barHeight.toFloat()),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
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
            drawText(textMeasurer, "No trend data", TextStyle(color = TextSecondary, fontSize = 14.sp), Offset(width / 2f - 40f, height / 2f - 20f))
            return@Canvas
        }

        val maxSpend = max(monthlySpends.map { it.second }.maxOrNull() ?: 1.0, 1.0)

        // Grid lines
        val gridLines = 3
        for (i in 0..gridLines) {
            val y = chartHeight - (chartHeight / gridLines) * i
            drawLine(Divider, Offset(paddingLeft, y), Offset(width, y), 1f)
            drawText(textMeasurer, "₹${(maxSpend / gridLines * i).toInt()}", labelStyle, Offset(2f, y - 15f))
        }

        val pointsCount = monthlySpends.size
        val xSpacing = chartWidth / (pointsCount - 1).coerceAtLeast(1)
        val points = monthlySpends.mapIndexed { index, pair ->
            Offset(paddingLeft + index * xSpacing, (chartHeight - ((pair.second / maxSpend) * chartHeight)).toFloat())
        }

        if (points.isNotEmpty()) {
            // Soft fill under the curve
            val fillPath = Path().apply {
                moveTo(points.first().x, chartHeight)
                points.forEachIndexed { i, p ->
                    if (i == 0) lineTo(p.x, p.y)
                    else {
                        val prev = points[i - 1]
                        val cx = (prev.x + p.x) / 2f
                        cubicTo(cx, prev.y, cx, p.y, p.x, p.y)
                    }
                }
                lineTo(points.last().x, chartHeight)
                close()
            }
            drawPath(fillPath, Accent.copy(alpha = 0.08f))

            // Stroke line — solid teal
            val strokePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val p = points[i]
                    val prev = points[i - 1]
                    val cx = (prev.x + p.x) / 2f
                    cubicTo(cx, prev.y, cx, p.y, p.x, p.y)
                }
            }
            drawPath(strokePath, Accent, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))

            // Data points — simple dots
            points.forEachIndexed { i, p ->
                drawCircle(AccentDim, 4.dp.toPx(), p)
                drawCircle(Accent, 2.5.dp.toPx(), p)
                drawText(textMeasurer, monthlySpends[i].first, labelStyle, Offset(p.x - 15f, chartHeight + 10f))
            }
        }
    }
}
