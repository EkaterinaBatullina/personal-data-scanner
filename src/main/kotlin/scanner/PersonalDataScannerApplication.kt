package scanner

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PersonalDataScannerApplication

fun main(args: Array<String>) {
    runApplication<PersonalDataScannerApplication>(*args)
}