package dev.blitical.jigsawDB.drivers.types.definition;

import dev.blitical.jigsawDB.drivers.DriverType;
import dev.blitical.jigsawDB.drivers.types.TypeSpec;

public class PrimaryGenericTypeDefinition extends GenericTypeDefinition {
    public PrimaryGenericTypeDefinition(
            TypeSpec type,
            Integer length,
            Integer precision,
            Integer scale,
            DriverType... allowedDrivers
    ) {
        super(
                type,
                length,
                precision,
                scale,
                allowedDrivers
        );
    }
}
