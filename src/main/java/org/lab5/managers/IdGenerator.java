package org.lab5.managers;

import org.lab5.models.Organization;

import java.util.Collection;

public class IdGenerator {
    private int nextId = 1;

    public void bootstrap(Collection<Organization> organizations) {
        int max = 0;

        for (Organization organization : organizations) {
            if (organization.getId() > max) {
                max = organization.getId();
            }
        }

        nextId = max + 1;
    }

    public int nextId() {
        return nextId++;
    }
}