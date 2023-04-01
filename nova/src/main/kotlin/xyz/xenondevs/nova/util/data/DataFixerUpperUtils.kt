package xyz.xenondevs.nova.util.data

import com.google.gson.JsonParser
import com.mojang.datafixers.util.Either
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import xyz.xenondevs.nova.util.NMSUtils
import java.io.File
import com.mojang.datafixers.util.Pair as MojangPair

fun <T> Codec<T>.decodeJsonFile(file: File) =
    decode(NMSUtils.REGISTRY_OPS, file.reader().use(JsonParser::parseReader))!!

inline fun <reified T : S, S : Any> Codec<S>.subType(): Codec<T> =
    this.comapFlatMap({ (it as? T).asDataResult("Expected subtype ${T::class.simpleName} but got ${it::class.simpleName}") }, { it })

fun <R> DataResult<R>.resultOrNull(): R? = result().orElse(null)

fun <R> DataResult<Holder<R>>.resultValueOrNull() = result().orElse(null)?.value()

fun <F, S> DataResult<MojangPair<F, S>>.resultFirstOrNull() = result().orElse(null)?.first

fun <F, S> DataResult<MojangPair<Holder<F>, S>>.resultFirstValueOrNull() = result().orElse(null)?.first?.value()

fun <R> DataResult<R>.getOrThrow(message: String): R {
    return if (result().isPresent) result().get()
    else throw IllegalStateException(message, IllegalArgumentException(error().get().toString()))
}

fun <R> DataResult<R>.getOrThrow(): R {
    return if (result().isPresent) result().get()
    else throw IllegalArgumentException(error().get().toString())
}

fun <R> DataResult<Holder<R>>.getValueOrThrow(message: String): R {
    return if (result().isPresent) result().get().value()
    else throw IllegalStateException(message, IllegalArgumentException(error().get().toString()))
}

fun <R> DataResult<Holder<R>>.getValueOrThrow(): R {
    return if (result().isPresent) result().get().value()
    else throw IllegalArgumentException(error().get().toString())
}

fun <F, S> DataResult<MojangPair<F, S>>.getFirstOrThrow(message: String): F {
    return if (result().isPresent) result().get().first
    else throw IllegalStateException(message, IllegalArgumentException(error().get().toString()))
}

fun <F, S> DataResult<MojangPair<F, S>>.getFirstOrThrow(): F {
    return if (result().isPresent) result().get().first
    else throw IllegalArgumentException(error().get().toString())
}

fun <F : Any, S : Any> DataResult<MojangPair<Holder<F>, S>>.getFirstValueOrThrow(message: String): F {
    return if (result().isPresent) result().get().first.value()
    else throw IllegalStateException(message, IllegalArgumentException(error().get().toString()))
}

fun <F : Any, S : Any> DataResult<MojangPair<Holder<F>, S>>.getFirstValueOrThrow(): F {
    return if (result().isPresent) result().get().first.value()
    else throw IllegalArgumentException(error().get().toString())
}

fun <R> Result<R>.asDataResult(): DataResult<R> {
    return if (isSuccess) DataResult.success(getOrThrow())
    else DataResult.error { toString() }
}

fun <R : Any> R?.asDataResult(error: String): DataResult<R> =
    if (this != null) DataResult.success(this) else DataResult.error { error }

class ResourceLocationOrTagKey<T> internal constructor(internal val either: Either<ResourceLocation, TagKey<T>>) {
    
    val location: ResourceLocation
        get() = either.left().get()
    
    val tag: TagKey<T>
        get() = either.right().get()
    
    val isElement: Boolean
        get() = either.left().isPresent
    
    val isTag: Boolean
        get() = either.right().isPresent
    
    
    override fun toString(): String {
        return either.map({ it.toString() }, { it.location.toString() })
    }
    
    companion object {
        
        fun <T> ofTag(tag: TagKey<T>): ResourceLocationOrTagKey<T> {
            return ResourceLocationOrTagKey(Either.right(tag))
        }
        
        fun <T> ofLocation(location: ResourceLocation): ResourceLocationOrTagKey<T> {
            return ResourceLocationOrTagKey(Either.left(location))
        }
        
        @JvmStatic
        fun <T, R : Registry<T>> codec(registry: ResourceKey<R>): Codec<ResourceLocationOrTagKey<T>> {
            val tagKeyCodec = TagKey.hashedCodec(registry)
            return Codec.either(ResourceLocation.CODEC, tagKeyCodec).xmap(
                { either -> ResourceLocationOrTagKey(either) },
                { tagKeyOrElementLocation -> tagKeyOrElementLocation.either }
            )
        }
        
    }
    
}