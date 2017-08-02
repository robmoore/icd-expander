package org.sdf.rkm.icd

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class Controller(val service: Service) {
    @RequestMapping("/icds/{icd}") // https://carbon-web-124017.appspot.com/icds/T83018A
    fun lookup(@PathVariable(value = "icd") icd: String): List<Icd> {
        return service.lookupIcd(icd)
    }

    @RequestMapping("/icds/{icd}/expand") // https://carbon-web-124017.appspot.com/icds/T83/expand
    fun expand(@PathVariable(value = "icd") icd: String): List<Icd> {
        return service.expandIcd(icd)
    }

    @RequestMapping("/icds/{icd}/gem") // https://carbon-web-124017.appspot.com/icds/T83/gem
    fun gem(@PathVariable(value = "icd") icd: String): List<IcdGem> {
        return service.gemIcd(icd)
    }

    /*
        Use referencedComponentId, sctName, mapTarget, icdName
        T78.1XX? => A, D, S // in case of these 7 character codes ? used
        E10.3299 // 7 character example

        if 7 digits, then try variation with ? at 7 digit as well as the full 7 digits
        if 6 digits, then try with ? at the 7 digit

        If ICD-9, then use ICD-9 to SNOMED tables (ICD9CM_SNOMED_MAP_1TOM_201612)
        If ICD-10, then use reverse mapping of SNOMED to ICD-10 tables (tls_Icd10cmHumanReadableMap_US1000124_20170301)

        #standardSql
        select referencedComponentId, sctName, mapTarget, icdName from `Snomed.tls*`
        where mapTarget = "A15.7" // OR mapTarget IN ("T78.1XXA", "T78.1XX?"), mapTarget IN ("T78.1XX", "T78.1XX?") [not sure this is a legit code but for example's sake]
        order by mapPriority

        NB: ICD codes in Snomed table has decimals!
     */

//    @RequestMapping("/icds/{icd}/snomed") // https://carbon-web-124017.appspot.com/icds/T83/snomed
//    fun snomed(@PathVariable(value = "icd") icd: String): List<SnomedGem> {
//        return service.snomed(icd)
//    }
}