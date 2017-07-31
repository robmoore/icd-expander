package org.sdf.rkm.icd

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class Controller(val service: Service) {
    @RequestMapping("/icds/{icd}") // https://carbon-web-124017.appspot.com/icds/T83018A
    fun lookup(@PathVariable(value = "icd") icd: String): List<ICD> {
        return service.lookupIcd(icd)
    }

    @RequestMapping("/icds/{icd}/expand") // https://carbon-web-124017.appspot.com/icds/T83/expand
    fun expand(@PathVariable(value = "icd") icd: String): List<ICD> {
        return service.expandIcd(icd)
    }

    @RequestMapping("/icds/{icd}/gem") // https://carbon-web-124017.appspot.com/icds/T83/gem
    fun gem(@PathVariable(value = "icd") icd: String): List<GEM> {
        return service.gemIcd(icd)
    }
}