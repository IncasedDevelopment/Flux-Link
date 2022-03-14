package com.fluxnetworks.bot.connections;

import java.net.URL;
import java.util.List;
import java.util.Optional;

import com.fluxnetworks.java_api.FluxAPI;

public abstract class ConnectionManager {

	public abstract boolean isReadOnly();

	public abstract Optional<FluxAPI> getApi(long guildId) throws BackendStorageException;

	public abstract void newConnection(long guildId, URL apiUrl) throws BackendStorageException;

	public abstract boolean updateConnection(long guildId, URL apiUrl) throws BackendStorageException;

	public abstract boolean removeConnection(long guildId) throws BackendStorageException;

	public abstract int countConnections() throws BackendStorageException;

	public abstract List<URL> listConnections() throws BackendStorageException;

	public abstract List<URL> listConnectionsUsedBefore(long time) throws BackendStorageException;

	public abstract List<URL> listConnectionsUsedSince(long time) throws BackendStorageException;

	public abstract Optional<Long> getLastUsed(long guildId) throws BackendStorageException;

	public abstract Optional<Long> getGuildIdByURL(URL url) throws BackendStorageException;

}