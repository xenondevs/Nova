package xyz.xenondevs.nova.initialize

/**
 * An exception specifically made to be thrown during initialization of an [Initializable].
 * When such an exception is thrown there, only the message but not the stack trace is printed.
 */
internal class InitializationException(message: String) : Exception(message)