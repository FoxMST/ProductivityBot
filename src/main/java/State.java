/**
 * Состояния диалога с пользователем.
 */
public enum State {
    /**
     * Ожидание команды.
     */
    IDLE,
    /**
     * Ожидание текста новой задачи.
     */
    ADD_TASK_WAIT_TEXT,
    /**
     * Ожидание выбора дня для показа списка.
     */
    SHOW_TASKLIST_WAIT_DATE,
    /**
     * Ожидание ввода даты для показа списка.
     */
    SHOW_TASKLIST_WAIT_CUSTOM_DATE,
    /**
     * Ожидание ID или выбора дня для удаления.
     */
    DELETE_TASK_WAIT_ID_OR_DATE,
    /**
     * Ожидание ввода даты для удаления.
     */
    DELETE_TASK_WAIT_CUSTOM_DATE,
    /**
     * Ожидание номера задачи для удаления.
     */
    DELETE_TASK_WAIT_LIST_NUMBER,
    /**
     * Ожидание подтверждения удаления.
     */
    DELETE_TASK_CONFIRM,
    /**
     * Ожидание ID или выбора дня для редактирования.
     */
    EDIT_TASK_WAIT_ID_OR_DATE,
    /**
     * Ожидание ввода даты для редактирования.
     */
    EDIT_TASK_WAIT_CUSTOM_DATE,
    /**
     * Ожидание номера задачи для редактирования.
     */
    EDIT_TASK_WAIT_LIST_NUMBER,
    /**
     * Ожидание выбора поля для редактирования.
     */
    EDIT_TASK_WAIT_FIELD,
    /**
     * Ожидание нового текста задачи.
     */
    EDIT_TASK_WAIT_NEW_TEXT
}