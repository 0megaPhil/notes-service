package com.notetaking.notes.dto;

/**
 * Input payload for creating a new note via GraphQL.
 *
 * @param title   required title
 * @param content note body
 * @param teamId  optional team; null for personal note
 */
public record CreateNoteInput(
    String title,
    String content,
    String teamId
) {}
