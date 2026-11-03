package com.dowglasmaia.financialtransaction.account.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void linkingToASubjectReturnsACopyAndKeepsTheOtherFields() {
        User user = User.restore("user-1", "Demo User", "11144477735", "demo@example.test", 11, 999998888, null);

        User linked = user.linkedTo("auth0|abc");

        assertThat(linked.getExternalSubject()).isEqualTo("auth0|abc");
        assertThat(linked.getId()).isEqualTo("user-1");
        assertThat(linked.getCpf()).isEqualTo("11144477735");
        assertThat(user.getExternalSubject()).isNull();
    }

    @Test
    void anAlreadyLinkedUserIsRelinkedToTheNewSubject() {
        User user = User.restore("user-1", "Demo User", "11144477735", "demo@example.test", 11, 999998888, "auth0|old");

        assertThat(user.linkedTo("auth0|new").getExternalSubject()).isEqualTo("auth0|new");
    }

    @Test
    void createHasNoIdNorSubjectUntilSaved() {
        User user = User.create("Second", "52998224725", "s@example.test", 11, 999997777);

        assertThat(user.getId()).isNull();
        assertThat(user.getExternalSubject()).isNull();
    }
}
