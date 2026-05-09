package dev.blitical.jigsawDB.drivers.types.definition;

import dev.blitical.jigsawDB.drivers.DriverType;
import dev.blitical.jigsawDB.drivers.types.TypeDefinition;
import dev.blitical.jigsawDB.drivers.types.TypeSpec;

public class BinaryTypeDefinition extends TypeDefinition {
    public BinaryTypeDefinition(
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
