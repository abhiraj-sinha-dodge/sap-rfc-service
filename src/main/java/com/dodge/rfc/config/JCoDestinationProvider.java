package com.dodge.rfc.config;

import com.sap.conn.jco.ext.DestinationDataEventListener;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom JCo destination provider that stores destinations in memory.
 * Destinations can be configured via environment variables or added dynamically.
 */
@Component
public class JCoDestinationProvider implements DestinationDataProvider {
    private static final Logger log = LoggerFactory.getLogger(JCoDestinationProvider.class);

    private final Map<String, Properties> destinations = new ConcurrentHashMap<>();
    private DestinationDataEventListener listener;

    @PostConstruct
    public void init() {
        // Register this provider with JCo
        try {
            Environment.registerDestinationDataProvider(this);
            log.info("JCo destination provider registered");
        } catch (IllegalStateException e) {
            // Already registered
            log.warn("JCo destination provider already registered");
        }

        // Load destinations from environment
        loadDestinationsFromEnv();
    }

    private void loadDestinationsFromEnv() {
        // Look for RFC_DEST_<name>_* environment variables
        Map<String, String> env = System.getenv();
        Map<String, Properties> destProps = new ConcurrentHashMap<>();

        for (Map.Entry<String, String> entry : env.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("RFC_DEST_")) {
                // Format: RFC_DEST_<destname>_<property>
                String rest = key.substring(9); // Remove "RFC_DEST_"
                int underscoreIdx = rest.indexOf('_');
                if (underscoreIdx > 0) {
                    String destName = rest.substring(0, underscoreIdx).toLowerCase();
                    String propName = rest.substring(underscoreIdx + 1).toLowerCase();

                    destProps.computeIfAbsent(destName, k -> new Properties());
                    Properties props = destProps.get(destName);

                    // Map env var names to JCo property names
                    String jcoProp = mapToJCoProp(propName);
                    if (jcoProp != null) {
                        props.setProperty(jcoProp, entry.getValue());
                    }
                }
            }
        }

        // Register all destinations
        for (Map.Entry<String, Properties> entry : destProps.entrySet()) {
            String destName = entry.getKey();
            Properties props = entry.getValue();
            if (props.containsKey(DestinationDataProvider.JCO_ASHOST) ||
                props.containsKey(DestinationDataProvider.JCO_MSHOST)) {
                destinations.put(destName, props);
                log.info("Loaded RFC destination: {}", destName);
            }
        }
    }

    private String mapToJCoProp(String envProp) {
        return switch (envProp.toUpperCase()) {
            case "ASHOST", "HOST" -> DestinationDataProvider.JCO_ASHOST;
            case "SYSNR" -> DestinationDataProvider.JCO_SYSNR;
            case "CLIENT" -> DestinationDataProvider.JCO_CLIENT;
            case "USER" -> DestinationDataProvider.JCO_USER;
            case "PASSWD", "PASSWORD" -> DestinationDataProvider.JCO_PASSWD;
            case "LANG" -> DestinationDataProvider.JCO_LANG;
            case "POOL_CAPACITY" -> DestinationDataProvider.JCO_POOL_CAPACITY;
            case "PEAK_LIMIT" -> DestinationDataProvider.JCO_PEAK_LIMIT;
            case "MSHOST" -> DestinationDataProvider.JCO_MSHOST;
            case "MSSERV" -> DestinationDataProvider.JCO_MSSERV;
            case "R3NAME" -> DestinationDataProvider.JCO_R3NAME;
            case "GROUP" -> DestinationDataProvider.JCO_GROUP;
            case "SAPROUTER" -> DestinationDataProvider.JCO_SAPROUTER;
            default -> null;
        };
    }

    /**
     * Add or update a destination programmatically.
     */
    public void addDestination(String name, Properties props) {
        destinations.put(name.toLowerCase(), props);
        if (listener != null) {
            listener.updated(name.toLowerCase());
        }
        log.info("Added/updated RFC destination: {}", name);
    }

    /**
     * Remove a destination.
     */
    public void removeDestination(String name) {
        destinations.remove(name.toLowerCase());
        if (listener != null) {
            listener.deleted(name.toLowerCase());
        }
        log.info("Removed RFC destination: {}", name);
    }

    public boolean hasDestination(String name) {
        return destinations.containsKey(name.toLowerCase());
    }

    @Override
    public Properties getDestinationProperties(String destinationName) {
        return destinations.get(destinationName.toLowerCase());
    }

    @Override
    public void setDestinationDataEventListener(DestinationDataEventListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean supportsEvents() {
        return true;
    }
}
