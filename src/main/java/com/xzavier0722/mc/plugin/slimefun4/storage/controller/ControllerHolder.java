package com.xzavier0722.mc.plugin.slimefun4.storage.controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ControllerHolder<T extends ADataController> {
    private static final Map<Class<? extends ADataController>, ControllerHolder<?>> holders = new ConcurrentHashMap<>();

    private final Map<StorageType, T> controllers;

    public static <CT extends ADataController> CT getController(Class<CT> clazz, StorageType type) {
        final var holder = holders.get(clazz);
        return holder == null ? null : ((ControllerHolder<CT>) holder).get(type);
    }

    public static void clearControllers() {
        holders.clear();
    }

    public static <CT extends ADataController> CT createController(Class<CT> clazz, StorageType type) {
        try {
            var re = clazz.getDeclaredConstructor().newInstance();
            ((ControllerHolder<CT>) holders.computeIfAbsent(clazz, k -> new ControllerHolder<CT>())).put(type, re);
            return re;
        } catch (Throwable e) {
            throw new IllegalStateException("Exception thrown while creating controller: " + clazz.getSimpleName(), e);
        }
    }

    private ControllerHolder() {
        controllers = new ConcurrentHashMap<>();
    }

    private T get(StorageType type) {
        return controllers.get(type);
    }

    private void put(StorageType type, T controller) {
        controllers.put(type, controller);
    }

    private void remove(StorageType type) {
        controllers.remove(type);
    }
}
