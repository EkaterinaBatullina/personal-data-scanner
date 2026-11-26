package scanner.service

import org.springframework.stereotype.Service
import scanner.fetcher.HtmlFetcher

@Service
class ScanService(
    private val fetcher: HtmlFetcher,
) {
    fun scan(url: String): String {
        return fetcher.fetch(url)
    }
}