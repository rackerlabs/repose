package org.openrepose.filters.herp

import javax.jms.ConnectionFactory

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.jms.listener.SimpleMessageListenerContainer
import org.springframework.jms.listener.adapter.MessageListenerAdapter

/**
 * Created with IntelliJ IDEA.
 * User: adrian
 * Date: 1/23/15
 * Time: 4:01 PM
 */
@Configuration
@EnableAutoConfiguration
class HerpConfiguration {

  @Bean
  def publisher() :Publisher = {
    new Publisher()
  }

  @Bean
  def listenerAdapter(publisher :Publisher) :MessageListenerAdapter = {
    val messaqeListener = new MessageListenerAdapter(publisher)
    messaqeListener.setDefaultListenerMethod("sendEvent")
    messaqeListener
  }

  @Bean
  def listenerContainer(messageListener :MessageListenerAdapter,
                        connectionFactory :ConnectionFactory) :SimpleMessageListenerContainer = {
    val container = new SimpleMessageListenerContainer
    container.setMessageListener(messageListener)
    container.setConnectionFactory(connectionFactory)
    container.setDestinationName("uae-queue")
    container
  }
}
