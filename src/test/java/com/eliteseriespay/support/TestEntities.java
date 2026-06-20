package com.eliteseriespay.support;

import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.domain.Project;
import java.math.BigDecimal;
import org.springframework.test.util.ReflectionTestUtils;

public final class TestEntities {

    private TestEntities() {
    }

    public static void setId(Object entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }

    public static Project project(long id, String name, BigDecimal episodeCostRub) {
        Project project = new Project(name, episodeCostRub);
        setId(project, id);
        return project;
    }

    public static Participant participant(long id, String vkId, String name, String comment) {
        Participant participant = new Participant(vkId, name, comment);
        setId(participant, id);
        return participant;
    }
}
