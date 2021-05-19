package xyz.xenondevs.nova.serialization.gson;

import com.google.gson.InstanceCreator;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.EnumMap;

// doesn't seem to work in kotlin
@SuppressWarnings({"unchecked", "rawtypes"})
public class EnumMapInstanceCreator implements InstanceCreator<EnumMap<?, ?>> {
    
    // https://stackoverflow.com/questions/54966118/how-to-deserialize-an-enummap
    public EnumMap<?, ?> createInstance(Type type) {
        return new EnumMap((Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0]);
    }
    
}
