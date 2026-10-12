package com.roqhrcl.common.outbox;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

@AutoConfiguration
@ConditionalOnClass(name = "jakarta.persistence.EntityManager")
@AutoConfigurationPackage(basePackageClasses = OutboxEvent.class)
public class OutboxAutoConfiguration {
}
