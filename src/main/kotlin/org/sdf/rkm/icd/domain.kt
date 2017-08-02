package org.sdf.rkm.icd


enum class CodeSet(val text: String) {
    ICD_9_CM("ICD-9-CM"),
    ICD_10_CM("ICD-10-CM"),
    SNOMED("SNOMED-CT")
}

data class SnomedGem(val source: String, val sourceCodeSet: CodeSet, val target: String, val targetCodeSet: CodeSet,
                     val targetDescription: String)

data class IcdGem(val source: String, val sourceCodeSet: CodeSet, val target: String, val targetCodeSet: CodeSet,
                  val targetDescription: String, val approximate: Boolean, val combination: Boolean, val scenario: Int,
                  val choiceList: Int)

data class Icd(val icd: String, val description: String, val codeSet: CodeSet)
