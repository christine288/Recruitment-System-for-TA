package com.bupt.ta.service;

import com.bupt.ta.model.ApplicantProfile;
import com.bupt.ta.model.EducationEntry;
import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.testsupport.FileTestSupport;
import com.bupt.ta.testsupport.TestFixtures;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileServiceTest {

    @Test
    void isApplicantProfileComplete_falseWhenSkillsMissing() {
        ApplicantProfile p = TestFixtures.completeProfile("u1");
        p.skills = "";
        assertFalse(ProfileService.isApplicantProfileComplete(p));
    }

    @Test
    void isApplicantProfileComplete_trueWhenCvMissing() {
        ApplicantProfile p = TestFixtures.completeProfile("u1");
        p.cvFileName = "";
        assertTrue(ProfileService.isApplicantProfileComplete(p));
    }

    @Test
    void isApplicantProfileComplete_trueWhenAllRequiredFieldsPresent() {
        assertTrue(ProfileService.isApplicantProfileComplete(TestFixtures.completeProfile("u1")));
    }

    @Test
    void isApplicantProfileComplete_falseWhenNull() {
        assertFalse(ProfileService.isApplicantProfileComplete(null));
    }

    @Test
    void isApplicantProfileComplete_falseWhenDegreeBachelor() {
        ApplicantProfile p = TestFixtures.completeProfile("u1");
        p.degree = "Bachelor";
        assertFalse(ProfileService.isApplicantProfileComplete(p));
    }

    @Test
    void isApplicantProfileComplete_falseWhenEducationEmptyArray() {
        ApplicantProfile p = TestFixtures.completeProfile("u1");
        p.educationJson = "[]";
        assertFalse(ProfileService.isApplicantProfileComplete(p));
    }

    @Test
    void parseEducationJson_validArray() {
        String json = "[{\"school\":\"BUPT\",\"degree\":\"BSc\",\"major\":\"CS\",\"period\":\"2020-2024\"}]";
        List<EducationEntry> entries = ProfileService.parseEducationJson(json);
        assertEquals(1, entries.size());
        assertEquals("BUPT", entries.get(0).school);
    }

    @Test
    void parseEducationJson_invalidReturnsEmpty() {
        assertTrue(ProfileService.parseEducationJson("not-json").isEmpty());
        assertTrue(ProfileService.parseEducationJson("{}").isEmpty());
    }

    @Test
    void buildEducationJson_roundTrip() {
        EducationEntry e = new EducationEntry();
        e.school = "BUPT";
        e.degree = "BSc";
        e.major = "CS";
        e.period = "2020-2024";
        String json = ProfileService.buildEducationJson(Collections.singletonList(e));
        List<EducationEntry> parsed = ProfileService.parseEducationJson(json);
        assertEquals(1, parsed.size());
        assertEquals("BUPT", parsed.get(0).school);
    }

    @Test
    void getByUserId_loadsStoredProfile() throws Exception {
        ServiceFactory factory = FileTestSupport.newFactory();
        FileTestSupport.seedUser("u9", "u9@bupt.edu.cn");
        ProfileService svc = factory.getProfileService();
        ApplicantProfile p = TestFixtures.completeProfile("u9");
        p.fullName = "Ann";
        svc.upsert(p);
        Optional<ApplicantProfile> loaded = svc.getByUserId("u9");
        assertTrue(loaded.isPresent());
        assertEquals("Ann", loaded.get().fullName);
    }

    @Test
    void upsert_overwritesExistingProfile() throws Exception {
        ServiceFactory factory = FileTestSupport.newFactory();
        FileTestSupport.seedUser("u1", "u1@bupt.edu.cn");
        ProfileService svc = factory.getProfileService();
        ApplicantProfile p1 = TestFixtures.completeProfile("u1");
        p1.fullName = "First";
        svc.upsert(p1);
        ApplicantProfile p2 = TestFixtures.completeProfile("u1");
        p2.fullName = "Second";
        svc.upsert(p2);
        assertEquals("Second", svc.getByUserId("u1").get().fullName);
    }

    @Test
    void deleteByUserId_removesProfile() throws Exception {
        ServiceFactory factory = FileTestSupport.newFactory();
        FileTestSupport.seedUser("u1", "u1@bupt.edu.cn");
        ProfileService svc = factory.getProfileService();
        svc.upsert(TestFixtures.completeProfile("u1"));
        svc.deleteByUserId("u1");
        assertFalse(svc.getByUserId("u1").isPresent());
    }

    @Test
    void hasAiMatchingInput_skillsOrCourses() {
        ApplicantProfile p = new ApplicantProfile();
        assertFalse(ProfileService.hasAiMatchingInput(p));
        p.skills = "Java";
        assertTrue(ProfileService.hasAiMatchingInput(p));
        p.skills = "";
        p.courses = "CS101";
        assertTrue(ProfileService.hasAiMatchingInput(p));
    }

    @Test
    void buildAiCapabilityText_includesSkillsAndCourses() {
        ApplicantProfile p = TestFixtures.completeProfile("u1");
        String text = ProfileService.buildAiCapabilityText(p);
        assertTrue(text.contains("Skills:"));
        assertTrue(text.contains("Courses:"));
        assertEquals("", ProfileService.buildAiCapabilityText(null));
    }

    @Test
    void getByUserId_missing_returnsEmpty() throws Exception {
        ServiceFactory factory = FileTestSupport.newFactory();
        assertFalse(factory.getProfileService().getByUserId("missing").isPresent());
    }
}
