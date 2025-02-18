/*
 * Copyright (c) 2018 CA. All rights reserved.
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.ca.apim.gateway.cagatewayconfig.bundle.loader;

import com.ca.apim.gateway.cagatewayconfig.beans.Bundle;
import com.ca.apim.gateway.cagatewayconfig.beans.InboundJmsDestinationDetail;
import com.ca.apim.gateway.cagatewayconfig.beans.InboundJmsDestinationDetail.AcknowledgeType;
import com.ca.apim.gateway.cagatewayconfig.beans.InboundJmsDestinationDetail.ContentTypeSource;
import com.ca.apim.gateway.cagatewayconfig.beans.InboundJmsDestinationDetail.ServiceResolutionSettings;
import com.ca.apim.gateway.cagatewayconfig.beans.JmsDestination;
import com.ca.apim.gateway.cagatewayconfig.beans.JmsDestinationDetail.ReplyType;
import com.ca.apim.gateway.cagatewayconfig.beans.OutboundJmsDestinationDetail;
import com.ca.apim.gateway.cagatewayconfig.beans.OutboundJmsDestinationDetail.ConnectionPoolingSettings;
import com.ca.apim.gateway.cagatewayconfig.beans.OutboundJmsDestinationDetail.MessageFormat;
import com.ca.apim.gateway.cagatewayconfig.beans.OutboundJmsDestinationDetail.PoolingType;
import com.ca.apim.gateway.cagatewayconfig.beans.OutboundJmsDestinationDetail.SessionPoolingSettings;
import com.ca.apim.gateway.cagatewayconfig.util.entity.EntityTypes;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import javax.inject.Singleton;

import java.util.Map;
import java.util.stream.Collectors;

import static com.ca.apim.gateway.cagatewayconfig.beans.JmsDestination.*;
import static com.ca.apim.gateway.cagatewayconfig.util.gateway.BuilderUtils.mapPropertiesElements;
import static com.ca.apim.gateway.cagatewayconfig.util.gateway.BundleElementNames.*;
import static com.ca.apim.gateway.cagatewayconfig.util.properties.PropertyConstants.*;
import static com.ca.apim.gateway.cagatewayconfig.util.xml.DocumentUtils.getSingleChildElement;
import static com.ca.apim.gateway.cagatewayconfig.util.xml.DocumentUtils.getSingleChildElementTextContent;
import static org.apache.commons.lang3.BooleanUtils.toBoolean;
import static org.apache.commons.lang3.ObjectUtils.anyNotNull;

@Singleton
public class JmsDestinationLoader implements BundleEntityLoader {

    @Override
    public void load(Bundle bundle, Element element) {
        
        final Element jmsDestinationEle = getSingleChildElement(getSingleChildElement(element, RESOURCE), JMS_DESTINATION);
        final String id = jmsDestinationEle.getAttribute(ATTRIBUTE_ID);

        final Element jmsDestinationDetailEle = getSingleChildElement(jmsDestinationEle, JMS_DESTINATION_DETAIL);
        final String name = getSingleChildElementTextContent(jmsDestinationDetailEle, NAME);
        final boolean isInbound = toBoolean(getSingleChildElementTextContent(jmsDestinationDetailEle, INBOUND));
        final boolean isTemplate = toBoolean(getSingleChildElementTextContent(jmsDestinationDetailEle, TEMPLATE));
        final Map<String, Object> jmsDestinationDetailProps = mapPropertiesElements(getSingleChildElement(jmsDestinationDetailEle, PROPERTIES, false), PROPERTIES);
        
        final Element jmsConnectionEle = getSingleChildElement(jmsDestinationEle, JMS_CONNECTION);
        final String providerType = getSingleChildElementTextContent(jmsConnectionEle, JMS_PROVIDER_TYPE);

        final Map<String, Object> jmsConnectionProps = mapPropertiesElements(getSingleChildElement(jmsConnectionEle, PROPERTIES, false), PROPERTIES);
        final String initialContextFactoryClassName = (String) jmsConnectionProps.remove(JNDI_INITIAL_CONTEXT_FACTORY_CLASSNAME);
        final String jndiUrl = (String) jmsConnectionProps.remove(JNDI_PROVIDER_URL);

        final Map<String, Object> contextPropertiesTemplateProps = mapPropertiesElements(getSingleChildElement(jmsConnectionEle, CONTEXT_PROPERTIES_TEMPLATE, false), CONTEXT_PROPERTIES_TEMPLATE);
        final String jndiUsername = (String) contextPropertiesTemplateProps.remove(JNDI_USERNAME);
        final String jndiPassword = (String) contextPropertiesTemplateProps.remove(JNDI_PASSWORD);
        
        final Map<String, Object> jndiProperties = contextPropertiesTemplateProps.entrySet().stream()
                .filter(map -> 
                        !map.getKey().startsWith(JMS_PROPS_PREFIX) &&
                        !map.getKey().startsWith(TIBCO_JMS_PREFIX) &&
                        !SOAP_ACTION_MSG_PROP_NAME.equals(map.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        contextPropertiesTemplateProps.keySet().removeAll(jndiProperties.keySet());
        
        final String destinationType = (String) jmsDestinationDetailProps.remove(DESTINATION_TYPE);
        final String connectionFactoryName = (String) jmsConnectionProps.remove(CONNECTION_FACTORY_NAME);
        final String destinationName = getSingleChildElementTextContent(jmsDestinationDetailEle, JMS_DESTINATION_NAME);
        final String destinationUsername = (String) jmsDestinationDetailProps.remove(PROPERTY_USERNAME);
        final String destinationPassword = (String) jmsDestinationDetailProps.remove(PROPERTY_PASSWORD);
        
        Builder builder = new Builder()
                .id(id)
                .name(name)
                .providerType(providerType)
                .initialContextFactoryClassName(initialContextFactoryClassName)
                .jndiUrl(jndiUrl)
                .jndiUsername(jndiUsername)
                .jndiPassword(jndiPassword)
                .destinationType(JmsDestination.DestinationType.fromType(destinationType))
                .connectionFactoryName(connectionFactoryName)
                .destinationName(destinationName)
                .destinationUsername(destinationUsername)
                .destinationPassword(destinationPassword);

        if (!jndiProperties.isEmpty()) {
            builder.jndiProperties(jndiProperties);
        }
        
        if (isInbound) {
            builder.inboundDetail(this.loadInboundDetail(jmsDestinationDetailProps, contextPropertiesTemplateProps));
        } else {
            builder.outboundDetail(this.loadOutboundDetail(isTemplate, jmsDestinationDetailProps, contextPropertiesTemplateProps));
        }
        
        this.removeKeystoreGoidRefs(providerType, contextPropertiesTemplateProps);
        if (!contextPropertiesTemplateProps.isEmpty()) {
            // Any remaining items in contextPropertiesTemplateProps is copied to 
            // additional properties.
            // Items remaining in contextPropertiesTemplateProps should be settings for
            // none generic JMS providers.
            builder.additionalProperties(contextPropertiesTemplateProps);
        }
        
        bundle.getJmsDestinations().put(name, builder.build());
    }

    private InboundJmsDestinationDetail loadInboundDetail(
            Map<String, Object> jmsDestinationDetailProps,
            Map<String, Object> contextPropertiesTemplateProps) {
        final boolean isHardwiredService = toBoolean((String) contextPropertiesTemplateProps.remove(IS_HARDWIRED_SERVICE));
        String serviceRef = null;
        if (isHardwiredService) {
            serviceRef = (String) contextPropertiesTemplateProps.remove(HARDWIRED_SERVICE_ID);
        }
        final String soapActionMsgPropName = (String) contextPropertiesTemplateProps.remove(SOAP_ACTION_MSG_PROP_NAME);
        final ContentTypeSource contentTypeSource = ContentTypeSource.fromType((String) contextPropertiesTemplateProps.remove(CONTENT_TYPE_SOURCE));
        String contentType = (String) contextPropertiesTemplateProps.remove(CONTENT_TYPE_VALUE);
        if (StringUtils.isEmpty(contentType)) {
            contentType = null; // Empty string is default. Set it to null for default value.
        }

        ServiceResolutionSettings serviceResolutionSettings = null;
        if (anyNotNull(serviceRef, soapActionMsgPropName, contentTypeSource, contentType)) {
            serviceResolutionSettings = new ServiceResolutionSettings();
            serviceResolutionSettings.setServiceRef(serviceRef);
            serviceResolutionSettings.setSoapActionMessagePropertyName(soapActionMsgPropName);
            serviceResolutionSettings.setContentTypeSource(contentTypeSource);
            serviceResolutionSettings.setContentType(contentType);
        }
        
        final InboundJmsDestinationDetail inboundDetail = new InboundJmsDestinationDetail();
        inboundDetail.setAcknowledgeType(AcknowledgeType.fromType((String) jmsDestinationDetailProps.remove(INBOUND_ACKNOWLEDGEMENT_TYPE)));
        inboundDetail.setReplyType(ReplyType.fromType((String) jmsDestinationDetailProps.remove(REPLY_TYPE)));
        inboundDetail.setReplyToQueueName((String) jmsDestinationDetailProps.remove(REPLY_QUEUE_NAME));
        inboundDetail.setUseRequestCorrelationId((Boolean) jmsDestinationDetailProps.remove(USE_REQUEST_CORRELATION_ID));
        inboundDetail.setServiceResolutionSettings(serviceResolutionSettings);
        inboundDetail.setFailureQueueName((String) jmsDestinationDetailProps.remove(INBOUND_FAILURE_QUEUE_NAME));
        if (contextPropertiesTemplateProps.containsKey(DEDICATED_CONSUMER_CONNECTION_SIZE)) {
            int dedicatedConsumerConnectionSize = convertToInteger(contextPropertiesTemplateProps.remove(DEDICATED_CONSUMER_CONNECTION_SIZE));
            if (DEFAULT_DEDICATED_CONSUMER_CONNECTION_SIZE != dedicatedConsumerConnectionSize) {
                inboundDetail.setNumOfConsumerConnections(dedicatedConsumerConnectionSize);
            }
        }

        Long maxInboundMessageSize = (Long) jmsDestinationDetailProps.remove(INBOUND_MAX_SIZE);
        if (DEFAULT_MAX_INBOUND_MESSAGE_SIZE != maxInboundMessageSize) {
            inboundDetail.setMaxMessageSizeBytes(maxInboundMessageSize);
        }
        
        // Cleanup. Remove items from contextPropertiesTemplateProps that are not used.
        contextPropertiesTemplateProps.remove(IS_DEDICATED_CONSUMER_CONNECTION); // This is always true.
        
        return inboundDetail;
    }
    
    private OutboundJmsDestinationDetail loadOutboundDetail(
            boolean isTemplate,
            Map<String, Object> jmsDestinationDetailProps,
            Map<String, Object> contextPropertiesTemplateProps) {
        PoolingType poolingType;
        ConnectionPoolingSettings connectionPoolingSettings = null;
        SessionPoolingSettings sessionPoolingSettings = null;

        final boolean isConnectionPool = toBoolean((String) contextPropertiesTemplateProps.remove(CONNECTION_POOL_ENABLED));
        if (isConnectionPool) {
            poolingType = PoolingType.CONNECTION;
            
            Integer poolSize = convertToInteger(contextPropertiesTemplateProps.remove(CONNECTION_POOL_SIZE));
            Integer poolMinIdle = convertToInteger(contextPropertiesTemplateProps.remove(CONNECTION_POOL_MIN_IDLE));
            Integer poolMaxWait = convertToInteger(contextPropertiesTemplateProps.remove(CONNECTION_POOL_MAX_WAIT));
            if (anyNotNull(poolSize, poolMinIdle, poolMaxWait)) {
                connectionPoolingSettings = new ConnectionPoolingSettings(poolSize, poolMinIdle, poolMaxWait);
            }
        } else {
            poolingType = PoolingType.SESSION;

            Integer poolSize = convertToInteger(contextPropertiesTemplateProps.remove(SESSION_POOL_SIZE));
            Integer poolMaxIdle = convertToInteger(contextPropertiesTemplateProps.remove(SESSION_POOL_MAX_IDLE));
            Integer poolMaxWait = convertToInteger(contextPropertiesTemplateProps.remove(SESSION_POOL_MAX_WAIT));
            if (anyNotNull(poolSize, poolMaxIdle, poolMaxWait)) {
                sessionPoolingSettings = new SessionPoolingSettings(poolSize, poolMaxIdle, poolMaxWait);
            }
        }
        
        final OutboundJmsDestinationDetail outboundDetail = new OutboundJmsDestinationDetail();
        outboundDetail.setIsTemplate(isTemplate);
        outboundDetail.setReplyType(ReplyType.fromType((String) jmsDestinationDetailProps.remove(REPLY_TYPE)));
        outboundDetail.setReplyToQueueName((String) jmsDestinationDetailProps.remove(REPLY_QUEUE_NAME));
        outboundDetail.setUseRequestCorrelationId((Boolean) jmsDestinationDetailProps.remove(USE_REQUEST_CORRELATION_ID));
        outboundDetail.setMessageFormat(MessageFormat.fromFormat((String) jmsDestinationDetailProps.remove(OUTBOUND_MESSAGE_TYPE)));
        outboundDetail.setPoolingType(poolingType);
        outboundDetail.setSessionPoolingSettings(sessionPoolingSettings);
        outboundDetail.setConnectionPoolingSettings(connectionPoolingSettings);

        // Cleanup. Remove items from contextPropertiesTemplateProps that are not used.
        contextPropertiesTemplateProps.remove(IS_HARDWIRED_SERVICE); // Not relevant for outbound.
        contextPropertiesTemplateProps.remove(CONTENT_TYPE_SOURCE); // Not relevant for outbound.
        contextPropertiesTemplateProps.remove(CONTENT_TYPE_VALUE); // Not relevant for outbound.
        
        return outboundDetail;
    }
    
    private void removeKeystoreGoidRefs(String providerType, Map<String, Object> contextPropertiesTemplateProps) {
        if (PROVIDER_TYPE_WEBSPHERE_MQ_OVER_LDAP.equals(providerType)) {
            // Just keep key alias, remove the rest.
            contextPropertiesTemplateProps.remove(DESTINATION_CLIENT_AUTH_KEYSTORE_ID);
        } else if (PROVIDER_TYPE_TIBCO_EMS.equals(providerType)) {
            // Just keep key alias, remove the rest.
            contextPropertiesTemplateProps.remove(JNDI_CLIENT_AUT_KEYSTORE_ID);
            contextPropertiesTemplateProps.remove(JNDI_CLIENT_AUT_AUTH_IDENTITY);
            contextPropertiesTemplateProps.remove(JNDI_CLIENT_AUT_AUTH_PASSWORD);
            contextPropertiesTemplateProps.remove(DESTINATION_CLIENT_AUTH_KEYSTORE_ID);
            contextPropertiesTemplateProps.remove(DESTINATION_CLIENT_AUTH_IDENTITY);
            contextPropertiesTemplateProps.remove(DESTINATION_CLIENT_AUTH_PASSWORD);
        }
    }
    
    @Override
    public String getEntityType() {
        return EntityTypes.JMS_DESTINATION_TYPE;
    }
    
    @Nullable
    private static Integer convertToInteger(Object value) {
        if (value == null) {
            return null;
        }
        return Integer.valueOf((String) value);
    }
}
