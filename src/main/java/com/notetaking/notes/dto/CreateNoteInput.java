package com.notetaking.notes.dto;

import java.util.UUID;

public record CreateNoteInput(
    String title,
    String content,
    UUID teamId
) {}
