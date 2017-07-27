package org.sdf.rkm.icd

import com.google.cloud.bigquery.*
import org.springframework.stereotype.Service
import java.util.concurrent.TimeoutException
import java.util.UUID

@Service
class Service {
    fun expandIcds(icd: String): List<ICD> {
        val normalizedIcd = icd.replace(".", "")
        return runStandardSqlQuery("SELECT ICD, Description, CASE WHEN (STARTS_WITH(_TABLE_SUFFIX, '10cm')) THEN 'ICD_10_CM' ELSE 'ICD_9_CM' END as CodeSet FROM `ICD_Codes.icd*` WHERE ICD LIKE '$normalizedIcd%' ORDER BY CodeSet, ICD")
    }

    @Throws(TimeoutException::class, InterruptedException::class)
    private fun runQuery(queryConfig: QueryJobConfiguration): List<ICD> {
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
        val results = mutableListOf<ICD>()
        while (result != null) {
            results.addAll(result.iterateAll().map { ICD(insertDecimal(it[0].stringValue),
                    it[1].stringValue, CodeSet.valueOf(it[2].stringValue)) })
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
    private fun runStandardSqlQuery(queryString: String): List<ICD> {
        val queryConfig = QueryJobConfiguration.newBuilder(queryString)
                .setUseLegacySql(false)
                .build()

        return runQuery(queryConfig)
    }
}