package org.sdf.rkm.icd

import com.google.cloud.bigquery.*
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.TimeoutException

@Service
class Service {
    companion object : KLogging()

    // http://www.icd10data.com/ICD10CM/Duplicate_Codes
    // https://aqua.io/codes/icd9/documentation.html
    fun lookupIcd(icd: String): List<Icd> {
        val sql = """
        SELECT ICD,
            Description,
            CASE WHEN (STARTS_WITH(_TABLE_SUFFIX, '10cm')) THEN 'ICD_10_CM' ELSE 'ICD_9_CM' END as CodeSet
        FROM `ICD_Codes.icd*`
        WHERE ICD = '${normalizeIcd(icd)}'
        ORDER BY CodeSet, ICD"""

        return runStandardSqlQuery(sql, icdTransformer)
    }

    fun expandIcd(icd: String): List<Icd> {
        val sql = """
        SELECT ICD,
            Description,
            CASE WHEN (STARTS_WITH(_TABLE_SUFFIX, '10cm')) THEN 'ICD_10_CM' ELSE 'ICD_9_CM' END as CodeSet
        FROM `ICD_Codes.icd*`
        WHERE ICD LIKE '${normalizeIcd(icd)}%'
        ORDER BY CodeSet, ICD"""

        return runStandardSqlQuery(sql, icdTransformer)
    }

    fun gemIcd(icd: String): List<Gem> {
        val icdSql = """
        SELECT Source,
          CASE
            WHEN (STARTS_WITH(g._TABLE_SUFFIX, '10gem')) THEN 'ICD_10_CM'
            ELSE 'ICD_9_CM'
          END AS SourceCodeSet,
          Target,
          CASE
            WHEN (STARTS_WITH(c._TABLE_SUFFIX, '10cm')) THEN 'ICD_10_CM'
            ELSE 'ICD_9_CM'
          END AS TargetCodeSet,
          Description,
          Approximate,
          Combination,
          Scenario,
          ChoiceList
        FROM
          `ICD_Codes.I*` AS g,
          `ICD_Codes.icd*` AS c
        WHERE
          Source = '${normalizeIcd(icd)}'
          AND Target = ICD
        ORDER BY
          Source,
          Scenario,
          ChoiceList"""

        val icdGems = runStandardSqlQuery(icdSql, icdGemTransformer)

        val icd9Regex = Regex("^(V\\d{2}(\\.\\d{1,2})?|\\d{3}(\\.\\d{1,2})?|E\\d{3}(\\.\\d)?)\$")
        val snomedGems = if (icd9Regex.matchEntire(icd) != null) {
            val icd9toSnomedSql = """
            SELECT ICD_CODE, SNOMED_CID, SNOMED_FSN
            FROM `Snomed.ICD9CM_SNOMED_MAP*`
            WHERE ICD_CODE = '$icd'"""

            runStandardSqlQuery(icd9toSnomedSql, icd9SnomedGemTransformer)
        } else {
            // if 7 digits, then try variation with ? at 7 digit as well as the full 7 digits
            // if 6 digits, then try with ? at the 7 digit
            val extraCriteria = if (icd.length >= 7) {
                "OR mapTarget = '${icd.substring(0, 7)}?'"
            } else ""
            val icd10ToSnomedSql = """
            SELECT mapTarget, referencedComponentId, sctName
            FROM `Snomed.tls_Icd10cmHumanReadableMap*`
            WHERE mapTarget = '$icd' $extraCriteria""" // account for case where ? in 7th digit and need to add to sixth

            runStandardSqlQuery(icd10ToSnomedSql, icd10SnomedGemTransformer)
        }

        return icdGems.plus(snomedGems)
    }

    fun normalizeIcd(icd: String) = icd.replace(".", "")


    val icdTransformer = fun(v: List<String>): Icd = Icd(insertDecimal(v[0], CodeSet.valueOf(v[2])), v[1],
            CodeSet.valueOf(v[2]))
    val icdGemTransformer = fun(v: List<String>): Gem = Gem(insertDecimal(v[0], CodeSet.valueOf(v[1])),
            CodeSet.valueOf(v[1]), insertDecimal(v[2], CodeSet.valueOf(v[3])), CodeSet.valueOf(v[3]), v[4],
            v[5].toBoolean(), v[6].toBoolean(), v[7].toInt(), v[8].toInt())
    val icd9SnomedGemTransformer = fun(v: List<String>): Gem = Gem(source = v[0], sourceCodeSet = CodeSet.ICD_9_CM,
            target = v[1], targetCodeSet =  CodeSet.SNOMED_CT, targetDescription = v[2])
    val icd10SnomedGemTransformer = fun(v: List<String>): Gem = Gem(source = v[0], sourceCodeSet =  CodeSet.ICD_10_CM,
            target = v[1], targetCodeSet = CodeSet.SNOMED_CT, targetDescription = v[2])


    @Throws(TimeoutException::class, InterruptedException::class)
    private fun <T> runQuery(queryConfig: QueryJobConfiguration, transformer: (List<String>) -> T): List<T> {
        val bigquery = BigQueryOptions.getDefaultInstance().service

        // Create a job ID so that we can safely retry.
        val jobId = JobId.of(UUID.randomUUID().toString())
        var queryJob: Job = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build())

        // Wait for the query to complete.
        queryJob = queryJob.waitFor()

        // Check for errors
        if (queryJob == null) {
            throw RuntimeException("Job no longer exists")
        } else if (queryJob.status.error != null) {
            // You can also look at queryJob.getStatus().getExecutionErrors() for all
            // errors, not just the latest one.
            throw RuntimeException(queryJob.status.error.toString())
        }

        // Get the results.
        val response = bigquery.getQueryResults(jobId)
        var result = response.result


        // Print all pages of the results.
        val results = mutableListOf<T>()
        while (result != null) {
            results.addAll(result.iterateAll().map { transformer(it.map { it.stringValue }.toList()) })
            result = result.nextPage
        }

        return results
    }

    /*
       For ICD-9:
        - V Codes also follow the XXX.XX format --> V54.31
        - E Codes follow XXXX.X --> E850.9
     */
    private fun insertDecimal(icd: String, codeSet: CodeSet): String {
        return if (icd.length > 3) {
            if (codeSet == CodeSet.ICD_10_CM || icd.first().isDigit() || icd.first() == 'V') {
                "${icd.substring(0..2)}.${icd.substring(3)}"
            } else {
                "${icd.substring(0..3)}.${icd.substring(4)}"
            }
        } else icd
    }

    @Throws(TimeoutException::class, InterruptedException::class)
    private fun <T> runStandardSqlQuery(queryString: String, transformer: (List<String>) -> T): List<T> {
        logger.debug { "Querying $queryString" }
        val queryConfig = QueryJobConfiguration.newBuilder(queryString)
                .setUseLegacySql(false)
                .build()

        return runQuery(queryConfig, transformer)
    }
}