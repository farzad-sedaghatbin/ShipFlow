package com.github.farzadsedaghatbin.shipflow.service.email;

import com.github.farzadsedaghatbin.shipflow.entity.User;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Picks the address ShipFlow should email for a user.
 *
 * <p>{@code users.email} is rarely filled in (accounts are created with a username and password;
 * the address lives on the linked person), so this falls back to the person's email. Placeholder
 * addresses that cannot receive mail ({@code admin@shipflow.local} and the like) are treated as
 * "no address" rather than being handed to the mail server, where they would bounce.
 */
public final class EmailAddressResolver {

  private static final Pattern SIMPLE_ADDRESS = Pattern.compile("^[^\\s@<>,;\"]+@[^\\s@<>,;\"]+\\.[^\\s@<>,;\"]+$");

  private EmailAddressResolver() {
  }

  /** The user's own address, else their person's, else {@code null}. */
  public static String of(User user) {
    if (user == null) {
      return null;
    }
    String own = usable(user.getEmail());
    if (own != null) {
      return own;
    }
    return user.getPerson() != null ? usable(user.getPerson().getEmail()) : null;
  }

  /** The address trimmed, or {@code null} when blank, malformed or a reserved placeholder domain. */
  public static String usable(String address) {
    if (address == null) {
      return null;
    }
    String trimmed = address.trim();
    if (!SIMPLE_ADDRESS.matcher(trimmed).matches()) {
      return null;
    }
    String domain = trimmed.substring(trimmed.indexOf('@') + 1).toLowerCase(Locale.ROOT);
    boolean placeholder = domain.endsWith(".local") || domain.endsWith(".invalid") || domain.endsWith(".example")
        || domain.endsWith(".test") || domain.equals("example.com");
    return placeholder ? null : trimmed;
  }
}
