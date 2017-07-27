package org.sdf.rkm.icd


enum class CodeSet(val text: String) {
    ICD_9_CM("ICD-9-CM"),
    ICD_10_CM("ICD-10-CM")
}

data class ICD(val icd: String, val description: String, val codeSet: CodeSet)