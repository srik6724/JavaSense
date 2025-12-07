package com.example;

import org.w3c.dom.Element;
import java.util.List;
import java.util.Map;

public class Graph {
    private final List<Element> nodes;
    private final List<Element> edges;
    private final Map<String, String> keyIdToName;

    public Graph(List<Element> nodes,
                 List<Element> edges,
                 Map<String, String> keyIdToName) {
        this.nodes = nodes;
        this.edges = edges;
        this.keyIdToName = keyIdToName;
    }

    public List<Element> getNodes() { return nodes; }
    public List<Element> getEdges() { return edges; }
    public Map<String, String> getKeyIdToName() { return keyIdToName; }
}
