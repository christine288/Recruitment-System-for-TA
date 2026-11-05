package com.bupt.ta.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicantFieldValidationTest {

    @ParameterizedTest
    @ValueSource(strings = {"Li Wei", "Mary-Jane O'Brien", "王明", "Ab"})
    void isValidFullName_acceptsValidNames(String name) {
        assertTrue(ApplicantFieldValidation.isValidFullName(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {"A", "123", "x@y", ""})
    void isValidFullName_rejectsInvalidNames(String name) {
        assertFalse(ApplicantFieldValidation.isValidFullName(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {"user@bupt.edu.cn", "a.b+c@example.co.uk", "x@mail.org"})
    void isValidEmailWithRealDomain_acceptsValid(String email) {
        assertTrue(ApplicantFieldValidation.isValidEmailWithRealDomain(email));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "@bupt.edu.cn", "user@", "user@localhost", "user@a.b"})
    void isValidEmailWithRealDomain_rejectsInvalid(String email) {
        assertFalse(ApplicantFieldValidation.isValidEmailWithRealDomain(email));
    }

    @ParameterizedTest
    @CsvSource({
            "13800138000, +8613800138000",
            "8613800138000, +8613800138000",
            "138 0013 8000, +8613800138000"
    })
    void normalizeChinaPhone_acceptsCommonForms(String raw, String expected) {
        assertEquals(expected, ApplicantFieldValidation.normalizeChinaPhone(raw));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "21800138000", "abc"})
    void normalizeChinaPhone_rejectsInvalid(String raw) {
        assertNull(ApplicantFieldValidation.normalizeChinaPhone(raw));
    }

    @Test
    void isValidChinaMobileNormalized_validNormalized() {
        assertTrue(ApplicantFieldValidation.isValidChinaMobileNormalized("+8613800138000"));
        assertFalse(ApplicantFieldValidation.isValidChinaMobileNormalized("+8612800138000"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"123456789", "12345678901", "19991234567"})
    void isValidBuptTenDigitStudentId_rejectsInvalid(String raw) {
        assertFalse(ApplicantFieldValidation.isValidBuptTenDigitStudentId(raw));
    }

    @Test
    void isValidBuptTenDigitStudentId_acceptsCurrentCohort() {
        String id = java.time.Year.now().getValue() + "123456";
        assertTrue(ApplicantFieldValidation.isValidBuptTenDigitStudentId(id));
    }

    @Test
    void isValidChineseResidentId18_validChecksum() {
        assertTrue(ApplicantFieldValidation.isValidChineseResidentId18("11010519491231002X"));
    }

    @Test
    void isValidChineseResidentId18_invalidChecksum() {
        assertFalse(ApplicantFieldValidation.isValidChineseResidentId18("110105194912310021"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Master", "Doctoral"})
    void isAllowedApplicantDegreeLevel_acceptsGraduate(String degree) {
        assertTrue(ApplicantFieldValidation.isAllowedApplicantDegreeLevel(degree));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bachelor", "PhD", ""})
    void isAllowedApplicantDegreeLevel_rejectsNonGraduate(String degree) {
        assertFalse(ApplicantFieldValidation.isAllowedApplicantDegreeLevel(degree));
    }

    @Test
    void applicantDegreeLevelWhitelist_isImmutable() {
        Set<String> w = ApplicantFieldValidation.applicantDegreeLevelWhitelist();
        assertTrue(w.contains("Master"));
        assertThrows(UnsupportedOperationException.class, () -> w.add("Other"));
    }
}
