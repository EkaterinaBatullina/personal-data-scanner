package scanner.validator

import org.springframework.stereotype.Component
import java.net.InetAddress
import java.net.URI

private const val IPV6_ADDRESS_SIZE_BYTES = 16
private const val IPV6_UNIQUE_LOCAL_MASK = 0xFE
private const val IPV6_UNIQUE_LOCAL_PREFIX = 0xFC

private val ACCEPTABLE_PROTOCOL_SET = setOf("http", "https")

@Component
class UrlValidator {

    fun validate(url: String): URI {
        val uri = parseUrl(url)

        validateProtocol(uri)
        validateHostPresence(uri)
        validateUserInfo(uri)

        return uri
    }

    private fun parseUrl(url: String): URI {
        return try {
            URI(url).normalize()
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid URL", e)
        }
    }

    private fun validateProtocol(uri: URI) {
        if (uri.scheme?.lowercase() !in ACCEPTABLE_PROTOCOL_SET) {
            throw IllegalArgumentException("Only HTTP and HTTPS are allowed")
        }
    }

    private fun validateHostPresence(uri: URI) {
        if (uri.host.isNullOrBlank()) {
            throw IllegalArgumentException("URL must contain a host")
        }
    }

    private fun validateUserInfo(uri: URI) {
        if (uri.userInfo != null) {
            throw IllegalArgumentException("Credentials in URL are not allowed")
        }
    }

    fun validateHost(uri: URI) {
        val addresses = try {
            InetAddress.getAllByName(uri.host)
        } catch (e: Exception) {
            throw IllegalArgumentException("Unable to resolve host", e)
        }

        if (addresses.isEmpty() || addresses.any { address ->
                address.isAnyLocalAddress ||
                        address.isLoopbackAddress ||
                        address.isLinkLocalAddress ||
                        address.isSiteLocalAddress ||
                        address.isMulticastAddress ||
                        isIpv6UniqueLocal(address)
            }) {
            throw IllegalArgumentException("Access to private or reserved IP addresses is forbidden")
        }
    }

    private fun isIpv6UniqueLocal(address: InetAddress): Boolean {
        val bytes = address.address
        /*
            Диапазон IPv6 fc00::/7 зарезервирован для локальных уникальных адресов
         */
        return bytes.size == IPV6_ADDRESS_SIZE_BYTES &&
                (bytes[0].toInt() and IPV6_UNIQUE_LOCAL_MASK) == IPV6_UNIQUE_LOCAL_PREFIX
    }
}
