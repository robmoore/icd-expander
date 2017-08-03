package org.sdf.rkm.icd

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/*
children instead of expand?
equivalances instead of gem?
 */
@RestController
class Controller(val service: Service) {
    @RequestMapping("/icds/{icd}") // /icds/T83018A
    fun lookup(@PathVariable(value = "icd") icd: String): List<Icd> = service.lookupIcd(icd)

    @RequestMapping("/icds/{icd}/expand") // /icds/T83/expand
    fun expand(@PathVariable(value = "icd") icd: String): List<Icd> = service.expandIcd(icd)

    @RequestMapping("/icds/{icd}/gem") // /icds/T83/gem
    fun gem(@PathVariable(value = "icd") icd: String): List<Gem> = service.gemIcd(icd)
}