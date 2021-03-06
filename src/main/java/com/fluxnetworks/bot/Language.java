package com.fluxnetworks.bot;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.fluxnetworks.bot.connections.BackendStorageException;
import com.fluxnetworks.java_api.FluxAPI;
import com.fluxnetworks.java_api.FluxException;
import com.fluxnetworks.java_api.FluxUser;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class Language {

	private static final Logger LOGGER = LoggerFactory.getLogger("Translation");

	public enum Term {

		ERROR_GENERIC,
		ERROR_NOT_SET_UP,
		ERROR_NOT_LINKED,
		ERROR_WEBSITE_CONNECTION,
		ERROR_WEBSITE_VERSION("version", "compatibleVersions"),
		ERROR_NO_PERMISSION,
		ERROR_READ_ONLY_STORAGE,

		VERIFY_DESCRIPTION,
		VERIFY_OPTION_TOKEN,
		VERIFY_TOKEN_INVALID,
		VERIFY_SUCCESS,

		PING_DESCRIPTION,
		PING_WORKING("time"),

		APIURL_DESCRIPTION,
		APIURL_OPTION_URL,
		APIURL_URL_INVALID,
		APIURL_URL_MALFORMED,
		APIURL_FAILED_CONNECTION,
		APIURL_ALREADY_USED("command"),
		APIURL_SUCCESS_UPDATED,
		APIURL_SUCCESS_NEW,
		APIURL_UNLINKED,

		GUILD_JOIN_SUCCESS("command"),
		GUILD_JOIN_NEEDS_RENEW("command"),
		GUILD_JOIN_WELCOME_BACK("command"),

		UPDATEUSERNAME_DESCRIPTION,
		UPDATEUSERNAME_SUCCESS,

		;

		private final String[] placeholders;

		Term(final String... placeholders) {
			this.placeholders = placeholders;
		}

		public String[] getPlaceholders() {
			return this.placeholders;
		}

		@Override
		public String toString() {
			return this.name().toLowerCase();
		}

	}

	private static final Map<String, String> FLUX_TO_POSIX = new HashMap<>();

	static {
		FLUX_TO_POSIX.put("Czech", "cs_CZ");
		FLUX_TO_POSIX.put("German", "de_DE");
		FLUX_TO_POSIX.put("Greek", "el_GR");
		FLUX_TO_POSIX.put("EnglishUK", "en_UK");
		FLUX_TO_POSIX.put("EnglishUS", "en_US");
		FLUX_TO_POSIX.put("Spanish", "es_419");
		FLUX_TO_POSIX.put("SpanishES", "es_ES");
		FLUX_TO_POSIX.put("French", "fr_FR");
		FLUX_TO_POSIX.put("Hungarian", "hu_HU");
		FLUX_TO_POSIX.put("Italian", "it_IT");
		FLUX_TO_POSIX.put("Lithuanian", "lt_LT");
		FLUX_TO_POSIX.put("Norwegian", "nb_NO");
		FLUX_TO_POSIX.put("Dutch", "nl_NL");
		FLUX_TO_POSIX.put("Polish", "pl_PL");
		FLUX_TO_POSIX.put("Portuguese", "pt_BR");
		FLUX_TO_POSIX.put("Romanian", "ro_RO");
		FLUX_TO_POSIX.put("Russian", "ru_RU");
		FLUX_TO_POSIX.put("Slovak", "sk_SK");
		FLUX_TO_POSIX.put("SwedishSE", "sv_SE");
		FLUX_TO_POSIX.put("Turkish", "tr_TR");
		FLUX_TO_POSIX.put("Chinese(Simplified)", "zh_CN");
	}

	private static Language defaultLanguage;
	public static Language getDefaultLanguage() { return defaultLanguage; }

	static void setDefaultLanguage(final String languageCode) throws LanguageLoadException {
		defaultLanguage = new Language(languageCode);
	}

	// Avoid having to instantiate new language objects all the time
	private static final Map<String, Language> LANGUAGE_CACHE = new HashMap<>();

	private final String language;
//	public static Language getLanguage() { return language; }

	private transient JsonObject json;

	private Language(final String language) throws LanguageLoadException {
		this.language = Objects.requireNonNull(language, "Language string is null");
		readFromFile();
	}

	private void readFromFile() throws LanguageLoadException {
		try (InputStream stream = Language.class.getResourceAsStream("/languages/" + this.language + ".json")) {
			if (stream == null) {
				throw new LanguageLoadException();
			}

			try (Reader reader = new InputStreamReader(stream)) {
				this.json = JsonParser.parseReader(reader).getAsJsonObject();
			}
		} catch (final IOException e) {
			throw new LanguageLoadException(e);
		}
	}

	public String get(final Term term, final Object... replacements) {
		Objects.requireNonNull(term, "Term is null");
		checkReplacements(term, replacements);

		String translation;
		if (this.json.has(term.toString())) {
			translation = this.json.get(term.toString()).getAsString();
		} else if (this == getDefaultLanguage()) {
			// oh no, cannot fall back to default translation if we are the default translation
			throw new RuntimeException(
					String.format("Term '%s' is missing from default (%s) translation", term, getDefaultLanguage().language));
		} else {
			LOGGER.warn("Language '{}' is missing term '{}', using default ({}) term instead.",
					this.language, term, getDefaultLanguage().language);
			translation = getDefaultLanguage().get(term, replacements);
		}

		for (int i = 0; i < replacements.length; i += 2) {
			final String key = (String) replacements[i];
			final String value = replacements[i + 1].toString();
			translation = translation.replace("{" + key + "}", value);
		}

		if (!checkLength(term, translation.length())) {
			translation = "message too long (bug)";
		}

		return translation;
	}

	private void checkReplacements(final Term term, final Object... replacements) {
		if (replacements == null || replacements.length == 0) {
			return;
		}

		Preconditions.checkArgument(replacements.length % 2 == 0, "Replacements array must have even length");

		final String[] required = term.getPlaceholders();
		final boolean[] valid = new boolean[required.length];

		for (int i = 0; i < replacements.length; i += 2) {
			Preconditions.checkArgument(replacements[i] instanceof String, "Replacement keys must be strings");
			final String key = (String) replacements[i];
			if (Objects.equals(key, required[i / 2])) {
				valid[i / 2] = true;
			} else {
				throw new IllegalArgumentException("Invalid replacement key '" + key + "'");
			}
		}

		for (int i = 0; i < required.length; i++) {
			if (!valid[i]) {
				throw new IllegalArgumentException("Missing replacement key '" + required[i] + "'");
			}
		}
	}

	private boolean checkLength(Term term, int length) {
		switch(term) {
			case VERIFY_DESCRIPTION:
			case APIURL_DESCRIPTION:
			case PING_DESCRIPTION:
			case UPDATEUSERNAME_DESCRIPTION:
				return length < 100;
			default:
				return true;
		}
	}

	public static Language getGuildLanguage(final Guild guild) {
		final Optional<FluxAPI> api;
		try {
			api = Main.getConnectionManager().getApi(guild.getIdLong());
		} catch (final BackendStorageException e) {
			e.printStackTrace();
			return getDefaultLanguage();
		}

		if (api.isPresent()) {
			try {
				final String language = api.get().getWebsite().getLanguage();
				final String posix = FLUX_TO_POSIX.get(language);
				if (posix == null) {
					LOGGER.warn("Website linked to guild {} uses unknown language '{}'", guild.getIdLong(), language);
					return getDefaultLanguage();
				} else {
					return getLanguage(posix);
				}
			} catch (final Exception e) {
				LOGGER.warn("Cannot retrieve language for guild {}, falling back to default language.", guild.getIdLong());
				return getDefaultLanguage();
			}
		} else {
			return getDefaultLanguage();
		}
	}

	@Deprecated
	public static Language getDiscordUserLanguage(final FluxAPI api, final User user) {
		Objects.requireNonNull(api, "API is null");
		Objects.requireNonNull(user, "User is null");
		try {
			final Optional<FluxUser> flux = api.getUserByDiscordId(user.getIdLong());
			if (flux.isPresent()) {
				return getLanguage(FLUX_TO_POSIX.get(flux.get().getLanguage()));
			} else {
				return getLanguage(FLUX_TO_POSIX.get(api.getWebsite().getLanguage()));
			}
		} catch (final FluxException e) {
			// If we can't communicate with the website, fall back to english
			return getDefaultLanguage();
		}
	}

	public static Language getLanguage(final String languageName) {
		if (languageName == null) {
			return getDefaultLanguage();
		}

		Language language = LANGUAGE_CACHE.get(languageName);
		if (language != null) {
			return language;
		}

		try {
			language = new Language(languageName);
		} catch (final LanguageLoadException e) {
			LOGGER.error("Failed to load language '{}', falling back to '{}'.", languageName, getDefaultLanguage().language);
			LOGGER.error("Error loading language", e);
			language = getDefaultLanguage();
		}

		LANGUAGE_CACHE.put(languageName, language);

		return language;
	}

	public static class LanguageLoadException extends Exception {

		private static final long serialVersionUID = 1335651150585947607L;

		public LanguageLoadException(final Throwable cause) {
			super("Language failed to load", cause);
		}

		public LanguageLoadException() {
			super("Language failed to load");
		}

	}

}
