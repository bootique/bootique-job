package io.bootique.job.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.bootique.config.PolymorphicConfiguration;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = SingleJob.class)
public interface JobDefinition extends PolymorphicConfiguration {

}
