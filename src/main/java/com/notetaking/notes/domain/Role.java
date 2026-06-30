package com.notetaking.notes.domain;

/**
 * Roles a user can hold within a team.
 */
public enum Role {
    /** Full control over the team and its members. */
    OWNER,
    /** Can manage members and notes. */
    ADMIN,
    /** Standard read/write access within the team. */
    MEMBER
}
