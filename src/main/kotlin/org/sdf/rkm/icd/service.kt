package org.sdf.rkm.icd

import com.google.cloud.bigquery.*
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.TimeoutException

@Service
class Service {
    companion object: KLogging()

    // http://www.icd10data.com/ICD10CM/Duplicate_Codes
    fun lookupIcd(icd: String): List<Icd> {
        val sql = "SELECT\n" +
                "  ICD,\n" +
                "  Description,\n" +
                "  CASE WHEN (STARTS_WITH(_TABLE_SUFFIX, '10cm')) THEN 'ICD_10_CM' ELSE 'ICD_9_CM' END as CodeSet\n" +
                "FROM `ICD_Codes.icd*`\n" +
                "WHERE ICD = '${normalizeIcd(icd)}'\n" +
                "ORDER BY CodeSet, ICD"
        return runStandardSqlQuery(sql, icdTransformer)
    }

    fun expandIcd(icd: String): List<Icd> {
        val sql = "SELECT\n" +
                "  ICD,\n" +
                "  Description, " +
                "  CASE WHEN (STARTS_WITH(_TABLE_SUFFIX, '10cm')) THEN 'ICD_10_CM' ELSE 'ICD_9_CM' END as CodeSet\n" +
                "FROM `ICD_Codes.icd*`\n" +
                "WHERE ICD LIKE '${normalizeIcd(icd)}%'\n" +
                "ORDER BY CodeSet, ICD"
        return runStandardSqlQuery(sql, icdTransformer)
    }

    fun gemIcd(icd: String): List<IcdGem> {
            val sql = "SELECT\n" +
                    "  Source,\n" +
                    "  CASE\n" +
                    "    WHEN (STARTS_WITH(g._TABLE_SUFFIX, '10gem')) THEN 'ICD_10_CM'\n" +
                    "    ELSE 'ICD_9_CM'\n" +
                    "  END AS SourceCodeSet,\n" +
                    "  Target,\n" +
                    "  CASE\n" +
                    "    WHEN (STARTS_WITH(c._TABLE_SUFFIX, '10cm')) THEN 'ICD_10_CM'\n" +
                    "    ELSE 'ICD_9_CM'\n" +
                    "  END AS TargetCodeSet,\n" +
                    "  Description,\n" +
                    "  Approximate,\n" +
                    "  Combination,\n" +
                    "  Scenario,\n" +
                    "  ChoiceList\n" +
                    "FROM\n" +
                    "  `ICD_Codes.I*` AS g,\n" +
                    "  `ICD_Codes.icd*` AS c\n" +
                    "WHERE\n" +
                    "  Source = '${normalizeIcd(icd)}'\n" +
                    "  AND Target = ICD\n" +
                    "ORDER BY\n" +
                    "  Source,\n" +
                    "  Scenario,\n" +
                    "  ChoiceList"
            return runStandardSqlQuery(sql, gemTransformer)
    }

    fun normalizeIcd(icd: String) = icd.replace(".", "")

    val icdTransformer = fun(v: List<String>): Icd = Icd(insertDecimal(v[0]), v[1], CodeSet.valueOf(v[2]))
    val gemTransformer = fun(v: List<String>): IcdGem = IcdGem(insertDecimal(v[0]), CodeSet.valueOf(v[1]), insertDecimal(v[2]),
            CodeSet.valueOf(v[3]), v[4], v[5].toBoolean(), v[6].toBoolean(), v[7].toInt(), v[8].toInt())

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

    private fun insertDecimal(icd: String): String {
        return if (icd.length > 3) {
            "${icd.substring(0..2)}.${icd.substring(3)}"
        } else icd
    }

    @Throws(TimeoutException::class, InterruptedException::class)
    private fun <T> runStandardSqlQuery(queryString: String, transformer: (List<String>) -> T): List<T> {
        logger.debug { "Querying\n$queryString" }
        val queryConfig = QueryJobConfiguration.newBuilder(queryString)
                .setUseLegacySql(false)
                .build()

        return runQuery(queryConfig, transformer)
    }
}