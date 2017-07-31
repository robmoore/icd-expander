package org.sdf.rkm.icd


enum class CodeSet(val text: String) {
    ICD_9_CM("ICD-9-CM"),
    ICD_10_CM("ICD-10-CM")
}

data class GEM(val source: String, val sourceCodeSet: CodeSet, val target: String, val targetCodeSet: CodeSet,
               val targetDescription: String, val approximate: Boolean, val noMap: Boolean,
               val combination: Boolean, val scenario: Int, val choiceList: Int)

data class ICD(val icd: String, val description: String, val codeSet: CodeSet)
