package eu.jelinek.hranolky.data.network

import eu.jelinek.hranolky.ui.shared.formatDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class SheetDbPostJointerReportingBody(

    @SerialName("data")
    val data: List<JointerReportingRow>,

    @SerialName("sheet")
    val sheet: String? = "AUTOIMPORT_TEST"
)

@Serializable
data class JointerReportingRow(
    // Properties that match the Google Sheet columns
    @SerialName("Datum")
    val date: String = formatDate(Date()),

    @SerialName("Dřevina")
    val quality: String,

    @SerialName("Tloušťka")
    val thickness: String,

    @SerialName("Šířka")
    val width: String,

    @SerialName("Délka")
    val length: String,

    @SerialName("Počet (ks)")
    val quantityChange: String,

    @SerialName("Určeno pro")
    val nextUse: String = "SKLAD",

    @SerialName("Vyrobil")
    val madeBy: String,
)

// Example Response for a successful POST (SheetDB might return {"created": 1} or similar)
@Serializable
data class SheetDbPostResponse(
    val created: Int? = null,
    // SheetDB might return other fields or an error message structure
    val error: String? = null
)

// You might also want a generic error data class if SheetDB has a standard error format
@Serializable
data class SheetDbErrorResponse(
    val error: String
)