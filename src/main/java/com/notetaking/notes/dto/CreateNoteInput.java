package com.notetaking.notes.dto;

import java.util.UUID;

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
    UUID teamId
) {}
