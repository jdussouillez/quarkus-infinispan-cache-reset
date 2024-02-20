package com.github.jdussouillez;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(
    includeClasses = {
        MathInfo.class
    },
    schemaPackageName = "math"
)
public interface MathCacheSchema extends GeneratedSchema {
}
