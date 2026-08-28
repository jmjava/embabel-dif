package com.embabel.dif.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record SemanticSnapshot(List<SemanticProperty> properties) {
    public SemanticSnapshot {
        properties = List.copyOf(properties);
    }

    public Map<String, String> asMap() {
        var map = new LinkedHashMap<String, String>();
        for (var property : properties) {
            map.put(property.path(), property.value());
        }
        return Map.copyOf(map);
    }

    public Optional<String> value(String path) {
        return Optional.ofNullable(asMap().get(path));
    }
}
