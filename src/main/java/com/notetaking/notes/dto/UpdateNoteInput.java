package com.notetaking.notes.dto;

/**
 * Input payload for updating an existing note (partial updates supported at service layer).
 *
 * @param title   new title (null keeps original)
 * @param content new content (null keeps original)
 */
public record UpdateNoteInput(
    String title,
    String content
) {}
