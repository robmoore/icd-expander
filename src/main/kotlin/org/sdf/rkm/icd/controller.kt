package org.sdf.rkm.icd

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping

@RestController
class Controller(val service: Service) {
    @RequestMapping("/icds/{icd}/expand") // https://carbon-web-124017.appspot.com/icds/T83/expand
    fun expand(@PathVariable(value = "icd") icd: String): Set<String> {
        return service.expandIcds(icd)
    }
}