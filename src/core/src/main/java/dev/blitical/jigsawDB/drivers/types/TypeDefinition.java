package dev.blitical.jigsawDB.drivers.types;

import dev.blitical.jigsawDB.drivers.DriverType;

import java.util.Set;

public class TypeDefinition {
    private final TypeSpec type;
    private final Integer length;
    private final Integer precision;
    private final Integer scale;
    private final Set<DriverType> allowedDrivers;

    public TypeDefinition(
            TypeSpec type,
            Integer length,
            Integer precision,
            Integer scale,
            DriverType... allowedDrivers
    ) {
        this.type = type;
        this.length = length;
        this.precision = precision;
        this.scale = scale;
        this.allowedDrivers = Set.of(allowedDrivers);
    }

    public TypeSpec type() {
        return type;
    }

    public Integer length() {
        return length;
    }

    public Integer precision() {
        return precision;
    }

    public Integer scale() {
        return scale;
    }

    public Set<DriverType> allowedDrivers() {
        return allowedDrivers;
    }

    public boolean allowed(DriverType type) {
        return allowedDrivers.contains(type);
    }
}
