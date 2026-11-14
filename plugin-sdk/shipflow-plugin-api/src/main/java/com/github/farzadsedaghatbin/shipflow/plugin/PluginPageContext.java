package com.github.farzadsedaghatbin.shipflow.plugin;

/**
 * Who is asking for a plugin page. ShipFlow has already checked that the user may open the page
 * and, when {@code projectId} is set, that they can access that project.
 *
 * @param username the signed-in user
 * @param role the user's role, for example {@code ADMIN}
 * @param projectId the project the user is working in, or {@code null}
 * @param locale the user's language code, for example {@code en} or {@code fa}
 */
public record PluginPageContext(String username, String role, Long projectId, String locale) {}
