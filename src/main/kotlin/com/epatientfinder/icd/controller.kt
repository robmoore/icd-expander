package com.epatientfinder.icd

import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@RestController
class Controller(val service: Service) {
    @RequestMapping("/expandIcd")
    fun expandIcd(@RequestParam(value = "icd") icd: String): List<String> {
        return service.expandIcds(icd)
    }
}