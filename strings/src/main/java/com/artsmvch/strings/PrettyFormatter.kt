package com.artsmvch.strings

internal object PrettyFormatter {
    fun table(rows: List<List<String>>): String {
        // Calculating column lengths
        val columnLengths = ArrayList<Int>()
        var columnIndex = 0
        while (true) {
            var hasColumn = false
            var maxColumnLength = 0
            rows.forEach { row ->
                if (columnIndex < row.size) {
                    hasColumn = true
                    val columnLength = row[columnIndex].length
                    if (columnLength > maxColumnLength) {
                        maxColumnLength = columnLength
                    }
                }
            }
            columnIndex++
            if (!hasColumn) break
            columnLengths.add(maxColumnLength)
        }
        // Building a table
        val builder = StringBuilder()
        rows.forEach { row ->
            row.forEachIndexed { index, column ->
                val columnLength = columnLengths[index]
                builder.append(column)
                repeat((columnLength - column.length).coerceAtLeast(0)) {
                    builder.append(' ')
                }
                builder.append(' ')
            }
            builder.append('\n')
        }
        return builder.toString()
    }
}