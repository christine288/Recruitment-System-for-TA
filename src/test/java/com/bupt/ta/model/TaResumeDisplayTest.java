package com.bupt.ta.model;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaResumeDisplayTest {

    @Test
    void hasSavedProfile_whenProfilePresent() {
        User u = new User("u1", "Ann", "2021000001", "a@bupt.edu.cn", "x");
        ApplicantProfile p = new ApplicantProfile();
        p.userId = "u1";
        p.fullName = "Ann";
        TaResumeDisplay d = new TaResumeDisplay(u, p, Collections.emptyList());
        assertTrue(d.hasSavedProfile);
    }

    @Test
    void hasSavedProfile_falseWhenNoProfile() {
        User u = new User("u1", "Ann", "2021000001", "a@bupt.edu.cn", "x");
        TaResumeDisplay d = new TaResumeDisplay(u, null, Collections.emptyList());
        assertFalse(d.hasSavedProfile);
    }

    @Test
    void filtersBlankEducationRows() {
        User u = new User("u1", "Ann", "2021000001", "a@bupt.edu.cn", "x");
        EducationEntry blank = new EducationEntry();
        EducationEntry filled = new EducationEntry();
        filled.school = "BUPT";
        TaResumeDisplay d = new TaResumeDisplay(u, null, java.util.Arrays.asList(blank, filled));
        assertEquals(1, d.visibleEducation.size());
        assertTrue(d.hasEducationRows());
    }

    @Test
    void hasEducationRows_falseWhenAllBlank() {
        User u = new User("u1", "Ann", "2021000001", "a@bupt.edu.cn", "x");
        TaResumeDisplay d = new TaResumeDisplay(u, null, Collections.singletonList(new EducationEntry()));
        assertFalse(d.hasEducationRows());
    }

}
