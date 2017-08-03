package org.sdf.rkm.icd

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.springframework.beans.factory.annotation.Autowired

@RunWith(SpringRunner::class)
@SpringBootTest
class SpringBootExampleApplicationTests {
    @Autowired
    lateinit var service: Service

    @Test
    fun contextLoads() {
    }

    @Test
    fun testLookup() {
        // ICD-9: 279.3
        val icd9s = service.lookupIcd("279.3")
        assertNotNull(icd9s)
        assertEquals(1, icd9s.size)
        assertEquals("Unspecified immunity deficiency", icd9s.first().description)
        assertEquals(CodeSet.ICD_9_CM, icd9s.first().codeSet)

        // ICD-10: S32.010A
        val icd10s = service.lookupIcd("S32.010A")
        assertNotNull(icd10s)
        assertEquals(1, icd10s.size)
        assertEquals("Wedge compression fracture of first lumbar vertebra, initial encounter for closed fracture",
                icd10s.first().description)
        assertEquals(CodeSet.ICD_10_CM, icd10s.first().codeSet)
    }

    @Test
    fun testExpansion() {
        // ICD-9: 279.5
        val icd9s = service.expandIcd("279.5")
        assertNotNull(icd9s)
        assertEquals(4, icd9s.size)
        assertEquals(listOf("279.50", "279.51", "279.52", "279.53"), icd9s.map { it.icd })
        assertTrue(icd9s.all { it.codeSet == CodeSet.ICD_9_CM })

        // ICD-10: S32.010
        val icd10s = service.expandIcd("S32.010")
        assertNotNull(icd10s)
        assertEquals(6, icd10s.size)
        assertEquals(listOf("S32.010A", "S32.010B", "S32.010D", "S32.010G", "S32.010K", "S32.010S"),
                icd10s.map { it.icd })
        assertTrue(icd10s.all { it.codeSet == CodeSet.ICD_10_CM })
    }

    @Test
    fun testGem() {
        // ICD-9: 279.3
        val icd9s = service.gemIcd("279.3")
        assertNotNull(icd9s)
        assertEquals(2, icd9s.size)
        /* Add this Snomed, too:
            second item:
            source: 279.3
            ICD_9_CM
            234532001
            SNOMED_CT
         */
        assertEquals(CodeSet.ICD_9_CM, icd9s.first().sourceCodeSet)
        assertEquals(CodeSet.ICD_10_CM, icd9s.first().targetCodeSet)
        assertEquals("279.3", icd9s.first().source)
        assertEquals("D84.9", icd9s.first().target)

        // ICD-10: A34
        val icd10s = service.gemIcd("A34")
        assertNotNull(icd10s)
        assertEquals(5, icd10s.size)
        assertEquals(CodeSet.ICD_10_CM, icd10s.first().sourceCodeSet)
        assertEquals(CodeSet.ICD_9_CM, icd10s.first().targetCodeSet)
        assertEquals("A34", icd10s.first().source)
        assertEquals("639.0", icd10s.first().target)
        /* Add these Snomeds, too:
            186378005
            609493004
            13766008
            240429008
        */
    }
}


