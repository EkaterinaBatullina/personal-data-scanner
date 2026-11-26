package scanner.fetcher

import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import scanner.validator.UrlValidator
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/*
   Выбранный тех лимит загрузки страницы - 2 мб
   (дабы предотвратить загрузку огромного документа)
*/
private const val MAX_RESPONSE_SIZE = 2 * 1024 * 1024
/*
   Выбранный тех лимит перенаправлений - 5
   (дабы сервер не зациклил)
*/
private const val MAX_REDIRECTS = 5
private const val CONNECT_TIMEOUT_SECONDS = 5L
private const val REQUEST_TIMEOUT_SECONDS = 10L
private const val USER_AGENT_HEADER = "User-Agent"
private const val USER_AGENT = "PersonalDataScanner/1.0"
private const val ACCEPT_HEADER = "Accept"
private const val HTML_MEDIA_TYPE = "text/html"
private const val LOCATION_HEADER = "Location"
private const val CONTENT_TYPE_HEADER = "Content-Type"
private const val SUCCESS_STATUS_MIN = 200
private const val SUCCESS_STATUS_MAX = 299
private const val CONTENT_LENGTH_HEADER = "Content-Length"

/*
   Обрабатываем конкретные статусы,
   предполагающие переход по другому адресу
*/
private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)

@Component
class HtmlFetcher(
    private val urlValidator: UrlValidator,
) {

    private val httpClient = HttpClient.newBuilder()
        /*
           Лимит по установлению соединения с сервером
        */
        .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
        /*
           Запрещаем автоматический переход,
           самостоятельно решаем, что делать с полученным ответом
        */
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()

    fun fetch(url: String): String {
        /*
            Проверяет структуру и допустимость URL
         */
        var currentUri = urlValidator.validate(url)

        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            /*
                Проверяет IP-адреса, соответствующие его хосту.
            */
            urlValidator.validateHost(currentUri)

            val request = HttpRequest.newBuilder()
                .uri(currentUri)
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .header(USER_AGENT_HEADER, USER_AGENT)
                .header(ACCEPT_HEADER, HTML_MEDIA_TYPE)
                .GET()
                .build()

            val response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofInputStream()
            )

            response.body().use { body ->
                val status = response.statusCode()

                if (status in REDIRECT_CODES) {
                    if (redirectCount == MAX_REDIRECTS) {
                        throw IllegalStateException("Too many redirects")
                    }
                    /*
                        Location - куда необходимо перенаправить запрос
                    */
                    val location = response.headers()
                        .firstValue(LOCATION_HEADER)
                        .orElseThrow {
                            IllegalStateException("Redirect without Location header")
                        }

                    currentUri = urlValidator.validate(
                        /*
                            resolve - формируем полноценный юрл
                            если Location относительный
                        */
                        currentUri.resolve(location).toString()
                    )

                    return@repeat
                }

                if (status !in SUCCESS_STATUS_MIN..SUCCESS_STATUS_MAX) {
                    throw IllegalStateException("HTTP request failed: $status")
                }

                val contentType = response.headers()
                    .firstValue(CONTENT_TYPE_HEADER)
                    .orElse("")

                /*
                    не обрабатываем условную картинку
                */
                val mediaType = contentType.substringBefore(';').trim()

                if (!mediaType.equals(HTML_MEDIA_TYPE, ignoreCase = true)) {
                    throw IllegalStateException("Unsupported content type: $contentType")
                }

                val contentLength = response.headers()
                    .firstValueAsLong(CONTENT_LENGTH_HEADER)
                    .orElse(-1L)

                if (contentLength > MAX_RESPONSE_SIZE) {
                    throw IllegalStateException("Response is too large")
                }

                val bytes = body.readNBytes(MAX_RESPONSE_SIZE + 1)

                if (bytes.size > MAX_RESPONSE_SIZE) {
                    throw IllegalStateException("Response is too large")
                }

                return Jsoup.parse(
                    bytes.inputStream(),
                    null,
                    currentUri.toString()
                ).outerHtml()
            }
        }

        throw IllegalStateException("Unable to fetch page")
    }
}
