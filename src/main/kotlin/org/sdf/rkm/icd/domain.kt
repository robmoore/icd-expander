package org.sdf.rkm.icd


enum class CodeSet(val text: String) {
    ICD_9_CM("ICD-9-CM"),
    ICD_10_CM("ICD-10-CM"),
    SNOMED_CT("SNOMED-CT")
}

data class Gem(val source: String, val sourceCodeSet: CodeSet, val target: String, val targetCodeSet: CodeSet,
                  val targetDescription: String, val approximate: Boolean? = null, val combination: Boolean? = null,
                  val scenario: Int? = null, val choiceList: Int? = null)

data class Icd(val icd: String, val description: String, val codeSet: CodeSet)
