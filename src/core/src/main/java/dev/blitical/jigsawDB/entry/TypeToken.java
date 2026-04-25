package dev.blitical.jigsawDB.entry;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class TypeToken<T> {
    private final Type type;

    protected TypeToken() {
        Type superclass = getClass().getGenericSuperclass();
        if (!(superclass instanceof ParameterizedType parameterized)) {
            throw new RuntimeException("TypeToken must be created with generic type parameter: new TypeToken<>(){}");
        }
        this.type = parameterized.getActualTypeArguments()[0];
    }

    public Type getType() {
        return type;
    }

    @SuppressWarnings("unchecked")
    public Class<T> getRawType() {
        if (type instanceof Class<?> cls) {
            return (Class<T>) cls;
        } else if (type instanceof ParameterizedType param) {
            return (Class<T>) param.getRawType();
        } else {
            throw new IllegalStateException("Cannot determine raw class for type " + type);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TypeToken<?> type))
            return false;

        return this.getRawType().equals(type.getRawType());
    }
}
