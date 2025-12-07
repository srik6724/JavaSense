package com.example;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jgrapht.nio.Attribute;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.AttributeType;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.graphml.GraphMLExporter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.example.Graph;
import com.example.Rule;

class RelEdge extends DefaultEdge {
    private final String relation; // e.g., "Friends" or "owns"
    private final int value;       // e.g., 1

    public RelEdge(String relation, int value) {
        this.relation = relation;
        this.value = value;
    }

    public String getRelation() {
        return relation;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return relation + "=" + value;
    }
}

public class Interpretation {

    private static List<Fact> facts = new ArrayList<>(); 
    private static List<Rule> rules = new ArrayList<>(); 
    private static Set<String> popularPeople = new HashSet<>();

    private static void createGraphMLFromData() {
        org.jgrapht.Graph<String, RelEdge> g = new DefaultDirectedGraph<>(RelEdge.class);
        g.addVertex("John");
        g.addVertex("Mary"); 
        g.addVertex("Justin"); 
        g.addVertex("Jack");
        g.addVertex("Dog");
        g.addVertex("Cat");
        g.addEdge("Justin", "Mary", new RelEdge("Friends", 1));
        g.addEdge("John", "Mary", new RelEdge("Friends", 1));
        g.addEdge("John", "Justin", new RelEdge("Friends", 1));

        // Pet edges (equivalent to owns=1)
        g.addEdge("Mary", "Cat", new RelEdge("owns", 1));
        g.addEdge("Justin", "Cat", new RelEdge("owns", 1));
        g.addEdge("Justin", "Dog", new RelEdge("owns", 1));
        g.addEdge("John", "Dog", new RelEdge("owns", 1));

        // Example: print all edges & attributes
        for (RelEdge e : g.edgeSet()) {
            String src = g.getEdgeSource(e);
            String dst = g.getEdgeTarget(e);
            System.out.println(src + " -> " + dst + " [" + e + "]");
        }

        GraphMLExporter<String, RelEdge> exporter = new GraphMLExporter<>();

// Use the vertex label as the ID in GraphML
exporter.setVertexIdProvider(v -> v);

// Give each edge a unique ID (not strictly necessary for your use case)
exporter.setEdgeIdProvider(e -> UUID.randomUUID().toString());

// (Optional but closer to your desired output):
// declare that these edge attributes exist, so <key> elements are written.
exporter.registerAttribute("owns",
        GraphMLExporter.AttributeCategory.EDGE,
        AttributeType.LONG);

exporter.registerAttribute("Friends",
        GraphMLExporter.AttributeCategory.EDGE,
        AttributeType.LONG);

// Now provide the actual attribute values for each edge
exporter.setEdgeAttributeProvider(edge -> {
    Map<String, Attribute> attrs = new LinkedHashMap<>();
    // This will generate: <data key="Friends">1</data> or <data key="owns">1</data>
    attrs.put(edge.getRelation(), DefaultAttribute.createAttribute(edge.getValue()));
    return attrs;
});

// Finally, write to a .graphml file
try (Writer writer = Files.newBufferedWriter(Paths.get("example.graphml"))) {
    exporter.exportGraph(g, writer);
    System.out.println("GraphML written to example.graphml");
} catch (Exception e) {
    e.printStackTrace();
}
    }

    public static Graph loadKnowledgeBase(String graphml) {
    System.out.println("This is the knowledge base.");
    List<Element> nodes = new ArrayList<>();
    List<Element> edges = new ArrayList<>();
    Map<String, String> keyIdToName = new HashMap<>();

    try {
        Path p = Paths.get(graphml).toAbsolutePath();
        File inputFile = new File(p.toString());

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);  // Important for GraphML
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputFile);
        doc.getDocumentElement().normalize();

        // 1) Build keyId -> attr.name map  (key0 -> "owns", key1 -> "Friends")
        NodeList keyList = doc.getElementsByTagNameNS("*", "key");
        for (int i = 0; i < keyList.getLength(); i++) {
            Element keyEl = (Element) keyList.item(i);
            String id = keyEl.getAttribute("id");             // "key0"
            String attrName = keyEl.getAttribute("attr.name"); // "owns" / "Friends"
            String forAttr = keyEl.getAttribute("for");        // "edge", "node", etc.
            if (!id.isEmpty() && !attrName.isEmpty() && "edge".equals(forAttr)) {
                keyIdToName.put(id, attrName);
            }
        }

        // 2) Nodes
        NodeList nodeList = doc.getElementsByTagNameNS("*", "node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            nodes.add((Element) nodeList.item(i));
        }

        // 3) Edges
        NodeList edgeList = doc.getElementsByTagNameNS("*", "edge");
        for (int i = 0; i < edgeList.getLength(); i++) {
            edges.add((Element) edgeList.item(i));
        }

        return new Graph(nodes, edges, keyIdToName);
    } catch (Exception e) {
        e.printStackTrace();
    }
    return null;
}


    /*public static void loadFactsFromFile() {
        System.out.println("These are the facts.");
        String text = ""; 
        int startTime = 0; 
        int endTime = 0; 
        try (BufferedReader br = new BufferedReader(new FileReader("facts.txt"))) {
            String line;

            while ((line = br.readLine()) != null) {
                System.out.println(line);  // Process the line
                if(line.startsWith("Text")) {
                    line = line.replace("Text: ", ""); 
                    text = line;
                }
                else if(line.startsWith("Start")) {
                    line = line.replace("Start: ", ""); 
                    startTime = Integer.parseInt(line);
                }
                else if(line.startsWith("End")) {
                    line = line.replace("End: " , ""); 
                    endTime = Integer.parseInt(line); 
                }
                else if(line.startsWith("-")) {
                    Fact fact = new Fact(text, startTime, endTime); 
                    facts.add(fact); 
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        for(Fact f: facts) {
            System.out.println(f.getText());
            System.out.println(f.getStartTime());
            System.out.println(f.getEndTime()); 
        }
    }*/

    public static void loadRulesFromFile() {
        System.out.println("These are the rules.");
        try (BufferedReader br = new BufferedReader(new FileReader("rules.txt"))) {
            String line;

            while ((line = br.readLine()) != null) {
                System.out.println(line);  // Process the line
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*public static void startReasoningProcess() {
        System.out.println("Starting reasoning process here."); 
        Graph graph = loadKnowledgeBase();
        // Mary is popular
        List<Element> nodes = graph.getNodes(); 
        List<Element> edges = graph.getEdges(); 

        for(Element node: nodes) {
              String id = node.getAttribute("id");
                String label = id; // fallback label
    NodeList dataTags = node.getElementsByTagNameNS("*", "data");
    if (dataTags.getLength() > 0) {
        label = dataTags.item(0).getTextContent();
    }

    System.out.println("id: " + id);
    System.out.println("Label: " + label);
        }


        for(Element edge: edges) {
            String source = edge.getAttribute("source");
    String target = edge.getAttribute("target");
                NodeList dataTags = edge.getElementsByTagNameNS("*", "data");
    for (int j = 0; j < dataTags.getLength(); j++) {
        Element data = (Element) dataTags.item(j);
        String key = data.getAttribute("key");         // e.g. "owns" or "Friends"
        if(key.equals("Friends")) {
        }
        else if(key.equals("owns")) {
        }
        String value = data.getTextContent();          // e.g. "1"
        System.out.println("Source: " + source + ", Target: " + target + ", Label: " + key + ", Value: " + value);
    }
        }
        

    boolean changed = true;
int iteration = 0;

while (changed) {
    iteration++;
    System.out.println("\n--- Iteration " + iteration + " ---");
    changed = applyPopularityRule(graph, facts);
}
        
        List<String> popularPeople = new ArrayList<>(); 
        popularPeople.add("Mary"); 
        System.out.println(" Fact: Mary is popular initially."); 
        System.out.println("A person is popular if they have a friend who is popular and who owns the same per as them."); 
        System.out.println("Start at timestep 1 and check for people who are a friends with Mary.");
        Map<String, String> friendships = new HashMap<>(); 
        Map<String, String> pets = new HashMap<>();
        for(Element edge: edges) {
            String source = edge.getAttribute("source");
            String target = edge.getAttribute("target");
            NodeList dataTags = edge.getElementsByTagNameNS("*", "data");
            for(int j = 0; j < dataTags.getLength(); j++) {
                Element data = (Element) dataTags.item(j);
                String key = data.getAttribute("key"); 
                if(key.equals("Friends")) {
                    friendships.put(source, target);
                }
                else if(key.equals("owns")) {
                    pets.put(source, target);
                }
            }
        }
        
        for(int i = 0; i < 3; i++) {
            for(Map.Entry<String, String> entry: friendships.entrySet()) {
            String key = entry.getKey(); 
            String value = entry.getValue();
            if(popularPeople.contains(value)) {
                String pet1 = pets.get(key); 
                System.out.println("Pet1: " + pet1);
                String pet2 = pets.get(value); 
                System.out.println("pet 2: " + pet2);
                if(pet1.equals(pet2)) {
                    System.out.println(key + " is popular.");
                    popularPeople.add(key);
                    friendships.remove(key); 
                    break;
                }
            }
        }
        }

    }*/

    public static void displayInterpretation() {
        System.out.println("Displaying interpretation.");
    }

    /*public static boolean applyPopularityRule(Graph graph, List<Fact> facts) {
    Set<String> popularPeople = new HashSet<>();
    for (Fact f : facts) {
        if (f.getText().endsWith("is popular.")) {
            popularPeople.add(f.getText().split(" ")[0]);
        }
    }

    Map<String, Set<String>> personToPets = new HashMap<>();
    Map<String, Set<String>> friendships = new HashMap<>();

    for (Element edge : graph.getEdges()) {
        String source = edge.getAttribute("source");
        String target = edge.getAttribute("target");

        NodeList dataList = edge.getElementsByTagNameNS("*", "data");
        for (int j = 0; j < dataList.getLength(); j++) {
            Element data = (Element) dataList.item(j);
            String label = data.getAttribute("key");

            if (label.equalsIgnoreCase("owns")) {
                personToPets.computeIfAbsent(source, k -> new HashSet<>()).add(target);
            } else if (label.equalsIgnoreCase("Friends")) {
                friendships.computeIfAbsent(source, k -> new HashSet<>()).add(target);
            }
        }
    }

    for (String person : friendships.keySet()) {
        Set<String> friends = friendships.getOrDefault(person, Set.of());
        Set<String> pets = personToPets.getOrDefault(person, Set.of());

        for (String friend : friends) {
            if (!popularPeople.contains(friend)) continue;

            Set<String> friendPets = personToPets.getOrDefault(friend, Set.of());

            for (String pet : pets) {
                if (friendPets.contains(pet)) { 
                    AnnotationFunction function = new AnnotationFunction();
                    String newFact = person + " is popular." + ", " + function.defaultAnnotation();
                    boolean exists = facts.stream()
                            .anyMatch(f -> f.getText().equals(newFact));
                    if (!exists) {
                        facts.add(new Fact(newFact, 0, 10));
                        System.out.println("Inferred: " + newFact);
                        return true; // 🚨 STOP after first inference
                    }
                }
            }
        }
    }

    return false;
}*/

    public static void main(String[]args) {
        createGraphMLFromData();
        //loadKnowledgeBase(); 
        //loadFactsFromFile();
        //loadRulesFromFile();
        //startReasoningProcess();
    }


}

