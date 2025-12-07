package com.example;

import java.util.List;

public class MainAdvanced {
    public static void main(String[]args) {
        Graph kb = Interpretation.loadKnowledgeBase("advanced_graph.graphml");
        JavaSense.addRule(new Rule(
    "popular(x) <-1 popular(y), Friends(x,y)",
    "popular_pet_rule"
));
JavaSense.addRule(new Rule(
    "cool_car(x) <-1 owns_car(x,y), Car_4(y)",
    "cool_car_rule"
));
JavaSense.addRule(new Rule(
    "cool_pet(x) <-1 owns_pet(x,y), Pet_2(y)",
    "cool_pet_rule"
));
JavaSense.addRule(new Rule(
    "trendy(x) <- cool_car(x), cool_pet(x)",
    "trendy_rule"
));
JavaSense.addRule(new Rule(
    "car_friend(x,y) <- owns_car(x,z), owns_car(y,z)",
    "car_friend_rule"
));
JavaSense.addRule(new Rule(
    "same_color_car(x,y) <- owns_car(x,c1), owns_car(y,c2)",
    "same_car_color_rule"
));

JavaSense.addFact(
    new Fact("popular(customer_0)", "popular-fact", 0, 5)
);
int timesteps = 6;
ReasoningInterpretation interp = JavaSense.reason(kb, timesteps);
displayNodePredicates(interp, timesteps);
    }

    private static void displayNodePredicates(ReasoningInterpretation interp, int timesteps) {
    List<String> nodePreds = List.of("trendy", "cool_car", "cool_pet", "popular");

    for (int t = 0; t <= timesteps; t++) {
        System.out.println("\nTIMESTEP - " + t);
        System.out.println("component      predicate");

        int row = 0;
        for (Atom a : interp.getFactsAt(t)) {
            if (!nodePreds.contains(a.getPredicate())) continue;
            String component = a.getArgs().get(0); // customer_0, customer_1, ...
            System.out.printf("%d    %-12s  %s [1.0,1.0]%n", row++, component, a.getPredicate());
        }
    }
}

}
