package io.bootique.job.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.bootique.annotation.BQConfig;
import io.bootique.config.PolymorphicConfiguration;

/**
 * @since 0.13
 */
@BQConfig("Job of a given type.")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = SingleJobDefinition.class)
public interface JobDefinition extends PolymorphicConfiguration {

}
