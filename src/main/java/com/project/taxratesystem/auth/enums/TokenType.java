package com.project.taxratesystem.auth.enums;

/**
 * The kind of credential a JWT represents (API.md §3).
 *
 * <p>The schema claim {@code typ} of every issued access token carries {@link #ACCESS}, so a
 * refresh credential can never be replayed as a bearer token.
 */
public enum TokenType {
    ACCESS,
    REFRESH
}

