/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.jms.activemq;

import java.util.List;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.apache.commons.pool2.PooledObject;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;

/**
 * Configuration for ActiveMQ {@link ConnectionFactory}.
 *
 * @author Greg Turnquist
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Aurélien Leboulanger
 * @since 1.1.0
 */
@Configuration
@ConditionalOnMissingBean(ConnectionFactory.class)
class ActiveMQConnectionFactoryConfiguration {

	@Configuration
	@ConditionalOnClass(CachingConnectionFactory.class)
	@ConditionalOnProperty(prefix = "spring.activemq.pool", name = "enabled", havingValue = "false", matchIfMissing = true)
	static class SimpleConnectionFactoryConfiguration {

		private final JmsProperties jmsProperties;

		private final ActiveMQProperties properties;

		private final List<ActiveMQConnectionFactoryCustomizer> connectionFactoryCustomizers;

		SimpleConnectionFactoryConfiguration(JmsProperties jmsProperties,
				ActiveMQProperties properties,
				ObjectProvider<List<ActiveMQConnectionFactoryCustomizer>> connectionFactoryCustomizers) {
			this.jmsProperties = jmsProperties;
			this.properties = properties;
			this.connectionFactoryCustomizers = connectionFactoryCustomizers
					.getIfAvailable();
		}

		@Bean
		@ConditionalOnProperty(prefix = "spring.jms.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
		public CachingConnectionFactory cachingJmsConnectionFactory() {
			JmsProperties.Cache cacheProperties = this.jmsProperties.getCache();
			CachingConnectionFactory connectionFactory = new CachingConnectionFactory(
					createConnectionFactory());
			connectionFactory.setCacheConsumers(cacheProperties.isConsumers());
			connectionFactory.setCacheProducers(cacheProperties.isProducers());
			connectionFactory.setSessionCacheSize(cacheProperties.getSessionCacheSize());
			return connectionFactory;
		}

		@Bean
		@ConditionalOnProperty(prefix = "spring.jms.cache", name = "enabled", havingValue = "false")
		public ActiveMQConnectionFactory jmsConnectionFactory() {
			return createConnectionFactory();
		}

		private ActiveMQConnectionFactory createConnectionFactory() {
			return new ActiveMQConnectionFactoryFactory(this.properties,
					this.connectionFactoryCustomizers)
							.createConnectionFactory(ActiveMQConnectionFactory.class);
		}

	}

	@Configuration
	@ConditionalOnClass({ PooledConnectionFactory.class, PooledObject.class })
	static class PooledConnectionFactoryConfiguration {

		@Bean(destroyMethod = "stop")
		@ConditionalOnProperty(prefix = "spring.activemq.pool", name = "enabled", havingValue = "true", matchIfMissing = false)
		public PooledConnectionFactory pooledJmsConnectionFactory(
				ActiveMQProperties properties,
				ObjectProvider<List<ActiveMQConnectionFactoryCustomizer>> factoryCustomizers) {
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactoryFactory(
					properties, factoryCustomizers.getIfAvailable())
							.createConnectionFactory(ActiveMQConnectionFactory.class);
			return new PooledConnectionFactoryFactory(properties.getPool())
					.createPooledConnectionFactory(connectionFactory);

		}

	}

}
