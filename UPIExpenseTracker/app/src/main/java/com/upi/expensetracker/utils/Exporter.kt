package com.upi.expensetracker.utils

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.upi.expensetracker.data.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Exporter {

    fun generateCSVString(transactions: List<TransactionEntity>): String {
        val builder = java.lang.StringBuilder()
        builder.append("ID,Date,Time,Amount,Merchant,Category,Description,Notes,Split,SplitWith,SplitAmount,Settled,ReferenceID,BankLast4,Recurring\n")
        
        for (t in transactions) {
            val splitFlag = if (t.isSplit) "YES" else "NO"
            val settledFlag = if (t.isSettled) "YES" else "NO"
            val recurringFlag = if (t.isRecurring) "YES" else "NO"
            
            // Escape double quotes in text fields
            val merchantEsc = t.merchant.replace("\"", "\"\"")
            val descEsc = t.description.replace("\"", "\"\"")
            val notesEsc = t.notes.replace("\"", "\"\"")
            val splitWithEsc = t.splitWith.replace("\"", "\"\"")
            
            builder.append("${t.id},${t.date},${t.time},${t.amount},\"$merchantEsc\",${t.category},\"$descEsc\",\"$notesEsc\",$splitFlag,\"$splitWithEsc\",${t.splitAmount},$settledFlag,${t.refId},${t.accountLast4},$recurringFlag\n")
        }
        
        return builder.toString()
    }

    fun shareCSV(context: Context, transactions: List<TransactionEntity>) {
        try {
            val csvContent = generateCSVString(transactions)
            val cacheFile = File(context.cacheDir, "upi_expense_report.csv")
            val writer = FileWriter(cacheFile)
            writer.write(csvContent)
            writer.close()

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, cacheFile)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "UPI Transactions Expense Report")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(intent, "Export Report via"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sharePDF(context: Context, transactions: List<TransactionEntity>) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val paint = Paint()
            val textPaint = Paint().apply {
                textSize = 10f
                color = android.graphics.Color.BLACK
            }
            val headerPaint = Paint().apply {
                textSize = 16f
                isFakeBoldText = true
                color = android.graphics.Color.BLACK
            }
            val subHeaderPaint = Paint().apply {
                textSize = 9f
                color = android.graphics.Color.GRAY
            }

            var y = 50f
            canvas.drawText("UPI Expense Tracker Report", 50f, y, headerPaint)
            y += 20f
            canvas.drawText("Generated on: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())}", 50f, y, subHeaderPaint)
            y += 30f

            // Table headers
            canvas.drawText("Date", 50f, y, textPaint)
            canvas.drawText("Merchant/Payee", 130f, y, textPaint)
            canvas.drawText("Category", 320f, y, textPaint)
            canvas.drawText("Amount", 480f, y, textPaint)
            
            y += 10f
            paint.color = android.graphics.Color.DKGRAY
            paint.strokeWidth = 1f
            canvas.drawLine(50f, y, 545f, y, paint)
            y += 20f

            for (t in transactions) {
                if (y > 800f) {
                    pdfDocument.finishPage(page)
                    val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                    page = pdfDocument.startPage(newPageInfo)
                    canvas = page.canvas
                    y = 50f
                    
                    // Table headers on new page
                    canvas.drawText("Date", 50f, y, textPaint)
                    canvas.drawText("Merchant/Payee", 130f, y, textPaint)
                    canvas.drawText("Category", 320f, y, textPaint)
                    canvas.drawText("Amount", 480f, y, textPaint)
                    y += 10f
                    canvas.drawLine(50f, y, 545f, y, paint)
                    y += 20f
                }

                canvas.drawText(t.date, 50f, y, textPaint)
                val merchantLim = if (t.merchant.length > 25) t.merchant.substring(0, 22) + "..." else t.merchant
                canvas.drawText(merchantLim, 130f, y, textPaint)
                canvas.drawText(t.category, 320f, y, textPaint)
                canvas.drawText("Rs. ${String.format("%.2f", t.amount)}", 480f, y, textPaint)
                y += 20f
            }

            pdfDocument.finishPage(page)

            val cacheFile = File(context.cacheDir, "upi_expense_report.pdf")
            val outputStream = FileOutputStream(cacheFile)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, cacheFile)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_SUBJECT, "UPI Transactions Expense Report (PDF)")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(intent, "Export Report via"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
