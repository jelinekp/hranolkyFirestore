package eu.jelinek.hranolky.data

import android.content.Context
import android.util.Log
import eu.jelinek.hranolky.R
import eu.jelinek.hranolky.data.network.JointerReportingRow
import eu.jelinek.hranolky.data.network.SheetDbPostJointerReportingBody
import eu.jelinek.hranolky.data.network.SheetDbPostResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.nio.channels.UnresolvedAddressException

// Define the interface as per your initial request
interface SheetDbRepository {
    suspend fun addLogRow(rowData: JointerReportingRow): Result<SheetDbPostResponse>
    // You can add other methods here later (GET, PUT, DELETE)
}

class KtorSheetDbRepository(private val context: Context) : SheetDbRepository {

    private val sheetDbApiUrl = "https://sheetdb.io/api/v1/phkjjcljgdah5"

    private val client = HttpClient(CIO) { // Or OkHttp if you chose that engine

        // Install JSON content negotiation (Ktor plugin)
        install(ContentNegotiation) { // Use install directly from HttpClientConfig scope
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true // Important if SheetDB returns extra fields
                encodeDefaults = true // to ensure default values are always in the JSON string
            })
        }

        // Optional: Install logging for debugging requests/responses (Ktor plugin)
        install(Logging) { // Use install directly from HttpClientConfig scope
            logger = object : Logger {
                override fun log(message: String) {
                    // Use android.util.Log for Android logging
                    Log.d("KtorLogger", message)
                }
            }
            level = LogLevel.ALL // Log everything (headers, body, etc.)
        }

        // Optional: Default request configuration (e.g., headers)
        // expectSuccess = true // Ktor will throw exceptions for non-2xx responses by default

        // More advanced error handling
        HttpResponseValidator {
            validateResponse { response ->
                val statusCode = response.status.value
                if (statusCode >= 300) { // Or specific error codes you want to handle
                    val errorBody = response.bodyAsText()
                    Log.e("KtorHttpError", "Status: $statusCode, Body: $errorBody")
                    // You could try to parse errorBody into a SheetDbErrorResponse here if SheetDB has a standard format
                }
            }
            // Ktor throws different exceptions for different error types:
            // ClientRequestException (4xx), RedirectResponseException (3xx), ServerResponseException (5xx)
        }
    }

    override suspend fun addLogRow(rowData: JointerReportingRow): Result<SheetDbPostResponse> {
        return try {

            val requestBody = SheetDbPostJointerReportingBody(data = listOf(rowData), sheet = "AUTOIMPORT_TEST")

            val response = client.post(sheetDbApiUrl) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                Result.success(response.body()) // Ktor automatically deserializes to SheetDbPostResponse
            } else {
                // Handle non-successful status codes that weren't caught by HttpResponseValidator as exceptions
                val errorBody = response.bodyAsText()
                val errorMessage = context.getString(R.string.odpov_serveru, "${response.status.value}: $errorBody")
                Log.e("SheetDbRepo", "Error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: ClientRequestException) { // 4xx errors
            val errorBody = e.response.bodyAsText()
            val errorMessage = context.getString(R.string.odpov_serveru, "${e.response.status.value}: $errorBody")
            Log.e("SheetDbRepo", "Client Error: $errorMessage", e)
            Result.failure(Exception(errorMessage, e))
        } catch (e: ServerResponseException) { // 5xx errors
            val errorBody = e.response.bodyAsText()
            val errorMessage = context.getString(R.string.odpov_serveru, "${e.response.status.value}: $errorBody")
            Log.e("SheetDbRepo", "Server Error: $errorMessage", e)
            Result.failure(Exception(errorMessage, e))
        } catch (e: RedirectResponseException) { // 3xx errors
            val errorBody = e.response.bodyAsText()
            val errorMessage = context.getString(R.string.odpov_serveru, "${e.response.status.value}: $errorBody")
            Log.e("SheetDbRepo", "Redirect Error: $errorMessage", e)
            Result.failure(Exception(errorMessage, e))
        }
        catch (e: UnresolvedAddressException) { // No internet connection or DNS issue
            Log.e("SheetDbRepo", "No internet connection or DNS issue", e)
            Result.failure(Exception(context.getString(R.string.nejste_p_ipojeni_k_internetu), e))
        }
        catch (e: Exception) { // Catch-all for other Ktor or network exceptions
            Log.e("SheetDbRepo", "Generic Ktor/Network Error", e)
            Result.failure(Exception(context.getString(R.string.unknown_error_occurred, e.message), e))
        }
    }

    fun closeClient() {
        client.close()
    }
}

