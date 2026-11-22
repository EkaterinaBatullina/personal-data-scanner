package scanner.controller.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class ScanRequest(
    @field:NotBlank(message = "URL must not be blank")
    @field:Pattern(
        regexp = "^https?://.+",
        message = "URL must start with http:// or https://"
    )
    val url: String
)