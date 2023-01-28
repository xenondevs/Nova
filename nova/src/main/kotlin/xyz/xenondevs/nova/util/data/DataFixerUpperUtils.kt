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

fun <R> DataResult<R>.resultOrNull() = result().orElse(null)

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

class TagKeyOrElementLocation<T>(internal val either: Either<ResourceLocation, TagKey<T>>) {
    
    val element: ResourceLocation
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
    
}

object DataFixerUpperUtils {
    
    @JvmStatic
    fun <T, R : Registry<T>> tagOrElementCodec(registry: ResourceKey<R>): Codec<TagKeyOrElementLocation<T>> {
        val tagKeyCodec = TagKey.hashedCodec(registry)
        return Codec.either(ResourceLocation.CODEC, tagKeyCodec).xmap(
            { either -> TagKeyOrElementLocation(either) },
            { tagKeyOrElementLocation -> tagKeyOrElementLocation.either }
        )
    }
    
}