package com.epatientfinder.icd

import com.google.cloud.bigquery.*
import org.springframework.stereotype.Service
import java.util.concurrent.TimeoutException
import java.util.UUID

@Service
class Service {
    fun expandIcds(icd: String): List<String> {
        val normalizedIcd = icd.replace(".", "")
        return runStandardSqlQuery("SELECT ICD FROM `AS_Sample.icd*` WHERE ICD LIKE '$normalizedIcd'")
    }

    @Throws(TimeoutException::class, InterruptedException::class)
    private fun runQuery(queryConfig: QueryJobConfiguration): List<String> {
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
        val results = mutableListOf<String>()
        while (result != null) {
            results.addAll(result.iterateAll().map { it[0].stringValue })
            result = result.nextPage
        }

        return results
    }

    @Throws(TimeoutException::class, InterruptedException::class)
    private fun runStandardSqlQuery(queryString: String): List<String> {
        val queryConfig = QueryJobConfiguration.newBuilder(queryString)
                .setUseLegacySql(false)
                .build()

        return runQuery(queryConfig)
    }
}