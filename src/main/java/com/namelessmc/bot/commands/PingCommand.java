package com.fluxnetworks.bot.commands;

import com.google.common.base.Ascii;
import com.fluxnetworks.bot.Language;
import com.fluxnetworks.bot.Language.Term;
import com.fluxnetworks.bot.Main;
import com.fluxnetworks.bot.connections.BackendStorageException;
import com.fluxnetworks.java_api.FluxAPI;
import com.fluxnetworks.java_api.FluxException;
import com.fluxnetworks.java_api.FluxVersion;
import com.fluxnetworks.java_api.Website;
import com.fluxnetworks.java_api.exception.UnknownFluxVersionException;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.stream.Collectors;

public class PingCommand extends Command {

	private static final Logger LOGGER = LoggerFactory.getLogger("Ping command");

	PingCommand() {
		super("ping");
	}

	@Override
	public CommandData getCommandData(final Language language) {
		return new CommandData(this.name, language.get(Term.PING_DESCRIPTION));
	}

	@Override
	public void execute(final SlashCommandEvent event) {
		final Guild guild = event.getGuild();
		final Language language = Language.getGuildLanguage(guild);

		Main.canModifySettings(event.getUser(), guild, (canModifySettings) -> {
			if (!canModifySettings) {
				event.reply(language.get(Term.ERROR_NO_PERMISSION)).setEphemeral(true).queue();
				return;
			}

			// Check if API URL works
			Optional<FluxAPI> optApi;
			try {
				optApi = Main.getConnectionManager().getApi(guild.getIdLong());
			} catch (final BackendStorageException e) {
				event.reply(language.get(Term.ERROR_GENERIC)).setEphemeral(true).queue();
				LOGGER.error("storage backend", e);
				return;
			}

			if (optApi.isEmpty()) {
				event.reply(language.get(Term.ERROR_NOT_SET_UP)).setEphemeral(true).queue();
				return;
			}

			// Now that we actually need to connect to the API, it may take a while
			event.deferReply().setEphemeral(true).queue(hook -> {
				Main.getExecutorService().execute(() -> {
					final FluxAPI api = optApi.get();

					try {
						final long start = System.currentTimeMillis();
						final Website info = api.getWebsite();
						try {
							if (!Main.SUPPORTED_WEBSITE_VERSIONS.contains(info.getParsedVersion())) {
								final String supportedVersions = Main.SUPPORTED_WEBSITE_VERSIONS.stream().map(FluxVersion::getName).collect(Collectors.joining(", "));
								hook.sendMessage(language.get(Term.ERROR_WEBSITE_VERSION, "version", info.getVersion(), "compatibleVersions", supportedVersions)).queue();
								return;
							}
						} catch (final UnknownFluxVersionException e) {
							// API doesn't recognize this version, but we can still display the unparsed name
							final String supportedVersions = Main.SUPPORTED_WEBSITE_VERSIONS.stream().map(FluxVersion::getName).collect(Collectors.joining(", "));
							hook.sendMessage(language.get(Term.ERROR_WEBSITE_VERSION, "version", info.getVersion(), "compatibleVersions", supportedVersions)).queue();
							return;
						}
						final long time = System.currentTimeMillis() - start;
						hook.sendMessage(language.get(Term.PING_WORKING, "time", time)).queue();
					} catch (final FluxException e) {
						hook.sendMessage(new MessageBuilder().appendCodeBlock(Ascii.truncate(e.getMessage(), 1500, "[truncated]"), "txt").build()).queue();
						hook.sendMessage(language.get(Term.APIURL_FAILED_CONNECTION)).queue();
						Main.logConnectionError(LOGGER, "FluxException during ping", e);
					}
				});
			});
		});
	}

}
