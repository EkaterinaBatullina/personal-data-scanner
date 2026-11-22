package scanner.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import scanner.controller.model.ScanRequest
import scanner.fetcher.HtmlFetcher

@RestController
@RequestMapping("/api/v1/scans")
class ScanController(
    private val fetcher: HtmlFetcher
) {
    /*
        GET http://localhost:8080/api/scans?url=https://example.com
    */
    @PostMapping
    fun scan(@Valid @RequestBody request: ScanRequest): String {
        return fetcher.fetch(request.url)
    }
}
