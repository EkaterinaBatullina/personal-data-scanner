package scanner.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import scanner.controller.model.ScanRequest
import scanner.service.ScanService

@RestController
@RequestMapping("/api/v1/scans")
class ScanController(
    private val scanService: ScanService,
) {
    /*
        GET http://localhost:8080/api/v1/scans?url=https://example.com
    */
    @PostMapping
    fun scan(@Valid @RequestBody request: ScanRequest): String {
        return scanService.scan(request.url)
    }
}
