/**
 * Text-based wire protocol and client/server-facing abstractions.
 * <p>
 * This package defines the format of messages exchanged between clients and
 * the server via {@link chat.protocol.Protocol}, along with the core
 * interfaces used by the server side: {@link chat.protocol.Backend} for
 * message delivery/storage and {@link chat.protocol.ClientSession} for
 * per-connection command handling.
 * </p>
 */
package chat.protocol;