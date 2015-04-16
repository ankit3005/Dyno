package org.opendaylight.controller.dyno.dynoCFENorthbound;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

public class DynoCFENorthboundRSApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(DynoCFENorthbound.class);
        return classes;
    }
}
