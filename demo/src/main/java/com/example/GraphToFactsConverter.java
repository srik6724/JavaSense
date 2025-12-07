package com.example;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GraphToFactsConverter {

    public static List<TimedFact> fromGraph(Graph kb, int maxTime) {
        List<TimedFact> facts = new ArrayList<>();

        List<Element> edges = kb.getEdges();
        Map<String, String> keyIdToName = kb.getKeyIdToName();

        for (Element edge : edges) {
            String source = edge.getAttribute("source"); // e.g. "John"
            String target = edge.getAttribute("target"); // e.g. "Mary"

            NodeList dataTags = edge.getElementsByTagNameNS("*", "data");
            for (int j = 0; j < dataTags.getLength(); j++) {
                Element data = (Element) dataTags.item(j);
                String keyId = data.getAttribute("key");          // "key0" / "key1"
                String predName = keyIdToName.getOrDefault(keyId, keyId); // "owns"/"Friends"

                Atom atom = new Atom(predName, List.of(source, target));
                // Edge is true over all timesteps [0, maxTime]
                facts.add(new TimedFact(atom, "edge_fact", 0, maxTime));
            }
        }

        return facts;
    }
}

