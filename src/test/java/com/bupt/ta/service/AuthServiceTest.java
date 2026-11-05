package com.bupt.ta.service;

import com.bupt.ta.model.Roles;
import com.bupt.ta.model.User;
import com.bupt.ta.persistence.ServiceFactory;
import com.bupt.ta.security.PasswordHasher;
import com.bupt.ta.testsupport.FileTestSupport;
import com.bupt.ta.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    private ServiceFactory factory;
    private AuthService auth;

    @BeforeEach
    void setUp() throws Exception {
        factory = FileTestSupport.newFactory();
        auth = factory.getAuthService();
    }

    @Test
    void login_validCredentials_returnsUser() throws Exception {
        seedUser("ta@test.local", "secret123", true);
        Optional<User> u = auth.login("ta@test.local", "secret123");
        assertTrue(u.isPresent());
        assertEquals("ta@test.local", u.get().email);
    }

    @Test
    void login_wrongPassword_returnsEmpty() throws Exception {
        seedUser("ta@test.local", "secret123", true);
        assertFalse(auth.login("ta@test.local", "wrong").isPresent());
    }

    @Test
    void login_inactiveAccount_returnsEmpty() throws Exception {
        seedUser("ta@test.local", "secret123", false);
        assertFalse(auth.login("ta@test.local", "secret123").isPresent());
    }

    @Test
    void register_success_createsActiveTa() throws Exception {
        User u = auth.register("New TA", TestFixtures.validBuptStudentId(), "new@bupt.edu.cn", "pass");
        assertEquals(Roles.TA, u.role);
        assertTrue(u.active);
        assertEquals(1, auth.listAllUsers().size());
    }

    @Test
    void register_duplicateEmail_throws() throws Exception {
        seedUser("dup@bupt.edu.cn", "x", true);
        assertThrows(IllegalArgumentException.class,
                () -> auth.register("A", "2021000001", "dup@bupt.edu.cn", "y"));
    }

    @Test
    void verifyCredentials_inactiveStillReturnsUser() throws Exception {
        seedUser("inactive@bupt.edu.cn", "pass", false);
        assertTrue(auth.verifyCredentials("inactive@bupt.edu.cn", "pass").isPresent());
        assertFalse(auth.login("inactive@bupt.edu.cn", "pass").isPresent());
    }

    @Test
    void findByEmail_caseInsensitive() throws Exception {
        seedUser("Case@Bupt.edu.cn", "pass", true);
        assertTrue(auth.findByEmail("case@bupt.edu.cn").isPresent());
    }

    @Test
    void findById_nullOrEmpty_returnsEmpty() throws Exception {
        assertFalse(auth.findById(null).isPresent());
        assertFalse(auth.findById("").isPresent());
    }

    @Test
    void setUserActive_togglesAndPersists() throws Exception {
        seedUser("u@bupt.edu.cn", "p", true);
        String id = auth.findByEmail("u@bupt.edu.cn").get().id;
        auth.setUserActive(id, false);
        assertFalse(auth.login("u@bupt.edu.cn", "p").isPresent());
        auth.setUserActive(id, true);
        assertTrue(auth.login("u@bupt.edu.cn", "p").isPresent());
    }

    @Test
    void setUserActive_unknownUser_throws() throws Exception {
        assertThrows(IllegalStateException.class,
                () -> auth.setUserActive("missing", true));
    }

    @Test
    void removeUserRecord_deletesRow() throws Exception {
        seedUser("del@bupt.edu.cn", "p", true);
        String id = auth.findByEmail("del@bupt.edu.cn").get().id;
        auth.removeUserRecord(id);
        assertFalse(auth.findById(id).isPresent());
    }

    @Test
    void countAdmins_countsOnlyAdminRole() throws Exception {
        User admin = TestFixtures.sampleAdmin("a1");
        admin.passwordHash = PasswordHasher.hash("x");
        auth.insertUser(admin);
        User ta = TestFixtures.sampleTa("t1", "t@t.com");
        ta.passwordHash = PasswordHasher.hash("x");
        auth.insertUser(ta);
        assertEquals(1, auth.countAdmins());
    }

    @Test
    void updateUserBasics_changesFields() throws Exception {
        seedUser("old@bupt.edu.cn", "p", true);
        User u = auth.findByEmail("old@bupt.edu.cn").get();
        auth.updateUserBasics(u, "New Name", "2021000099", "new@bupt.edu.cn");
        User updated = auth.findById(u.id).get();
        assertEquals("New Name", updated.name);
        assertEquals("new@bupt.edu.cn", updated.email);
    }

    @Test
    void updateUserBasics_duplicateEmail_throws() throws Exception {
        User u1 = TestFixtures.sampleTa("u1", "a@t.com");
        u1.passwordHash = PasswordHasher.hash("x");
        auth.insertUser(u1);
        User u2 = TestFixtures.sampleTa("u2", "b@t.com");
        u2.passwordHash = PasswordHasher.hash("x");
        auth.insertUser(u2);
        assertThrows(IllegalArgumentException.class,
                () -> auth.updateUserBasics(u1, "A", "1", "b@t.com"));
    }

    @Test
    void listAllUsers_returnsAll() throws Exception {
        User u1 = TestFixtures.sampleTa("u1", "one@bupt.edu.cn");
        u1.passwordHash = PasswordHasher.hash("p");
        auth.insertUser(u1);
        User u2 = TestFixtures.sampleTa("u2", "two@bupt.edu.cn");
        u2.passwordHash = PasswordHasher.hash("p2");
        auth.insertUser(u2);
        assertEquals(2, auth.listAllUsers().size());
    }

    private void seedUser(String email, String password, boolean active) throws Exception {
        User u = auth.register("Test TA", "S001", email, password);
        if (!active) {
            auth.setUserActive(u.id, false);
        }
    }
}
