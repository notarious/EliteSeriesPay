package com.eliteseriespay.exception;

public class NotFoundException extends RuntimeException {

    private final String entityName;
    private final Long id;

    public NotFoundException(String entityName, Long id) {
        super(entityName + " not found: " + id);
        this.entityName = entityName;
        this.id = id;
    }

    public String getEntityName() {
        return entityName;
    }

    public Long getId() {
        return id;
    }
}
