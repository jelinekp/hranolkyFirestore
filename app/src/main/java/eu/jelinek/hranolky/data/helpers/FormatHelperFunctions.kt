package eu.jelinek.hranolky.data.helpers

import android.os.Build
import androidx.annotation.RequiresApi
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}

fun formatOnlyDate(date: Date): String {
    val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return formatter.format(date)
}

@RequiresApi(Build.VERSION_CODES.O)
fun formatShortDate(date: Date): String {
    val inputLocalDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    return when (inputLocalDate) {
        today -> "dnes"
        yesterday -> "včera"
        else -> {
            val formatter = SimpleDateFormat("dd.MM.", Locale.getDefault())
            formatter.format(date)
        }
    }
}


fun formatCubicMeters(volume: Double?): String {
    return if (volume == null) {
        "0.0 m³"
    } else
        String.format("%.3f m³", volume)
}

fun formatCubicMetersTwo(volume: Double?): String {
    return if (volume == null) {
        "0.0"
    } else
        String.format("%.2f", volume)
}

fun Float?.toCustomCommaDecimalString(): String {
    if (this == null) {
        return "0,0"
    }

    val symbols = DecimalFormatSymbols(Locale.ROOT) // Start with neutral symbols
    symbols.decimalSeparator = ',' // Set the decimal separator to a comma
    // symbols.groupingSeparator = '.' // Optionally set thousands separator if needed

    val decimalFormat = DecimalFormat("#.##", symbols) // Basic pattern, adjust as needed
    // "#.##" means optional digits before decimal, up to 2 digits after.
    // Use "0.00" if you always want at least two decimal places.
    // For your case, to ensure "42,4" and not "42,40", a more adaptive pattern or
    // controlling fraction digits might be better:

    val customFormat = DecimalFormat()
    customFormat.decimalFormatSymbols = symbols
    // Control how many decimal places are shown:
    customFormat.minimumFractionDigits = 0 // Show at least 0
    customFormat.maximumFractionDigits = 2 // Show at most 2 (or your desired precision)

    return customFormat.format(this)
}