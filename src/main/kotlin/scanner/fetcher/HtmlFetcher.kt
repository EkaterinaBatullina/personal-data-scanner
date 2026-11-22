package scanner.fetcher

import org.jsoup.Jsoup
import org.springframework.stereotype.Component

@Component
class HtmlFetcher {

    fun fetch(url: String): String {
        return Jsoup.connect(url)
            .userAgent("PersonalDataScanner/1.0")
            .timeout(10_000)
            .get()
            .html()
    }
}
