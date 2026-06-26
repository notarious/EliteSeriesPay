package com.eliteseriespay.backup;

public class DatabaseBackupException extends RuntimeException {

    public static final String SOURCE_MISSING = "Файл базы данных не найден.";
    public static final String SOURCE_EMPTY = "Файл базы данных пуст. Резервная копия не создана.";
    public static final String CREATE_FAILED = "Не удалось создать резервную копию.";
    public static final String LIST_FAILED = "Не удалось загрузить список резервных копий.";
    public static final String NOT_FOUND = "Резервная копия не найдена.";

    private final String userMessage;

    public DatabaseBackupException(String userMessage) {
        super(userMessage);
        this.userMessage = userMessage;
    }

    public DatabaseBackupException(String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.userMessage = userMessage;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
