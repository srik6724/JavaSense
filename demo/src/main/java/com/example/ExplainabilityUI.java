package com.example;

import java.util.*;

/**
 * Explainability and Provenance Visualization Utilities
 *
 * <p>Provides human-readable explanations and visualizations of how facts were derived.
 * Critical for regulatory compliance, debugging, and building trust in AI systems.</p>
 *
 * <h2>Use Cases:</h2>
 * <ul>
 *   <li><b>Regulatory Compliance:</b> Explain decisions for audits (GDPR, finance regulations)</li>
 *   <li><b>Debugging:</b> Understand why a rule fired or didn't fire</li>
 *   <li><b>Trust:</b> Show users why the system made a decision</li>
 *   <li><b>Education:</b> Teach users how the reasoning engine works</li>
 * </ul>
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * ExplainabilityUI ui = new ExplainabilityUI(provenance);
 *
 * // Why was this account flagged?
 * String explanation = ui.explainWhy(Atom.parse("fraudDetected(A123)"), 5);
 * System.out.println(explanation);
 *
 * // Generate HTML report
 * String html = ui.generateHTMLReport(Atom.parse("fraudDetected(A123)"), 5);
 * Files.writeString(Path.of("fraud_report.html"), html);
 * }</pre>
 */
public class ExplainabilityUI {

    private final Provenance provenance;

    public ExplainabilityUI(Provenance provenance) {
        this.provenance = provenance;
    }

    /**
     * Generates a human-readable explanation of why a fact is true.
     *
     * @param atom the fact to explain
     * @param time the timestep
     * @return multi-line explanation
     */
    public String explainWhy(Atom atom, int time) {
        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(70)).append("\n");
        sb.append("Why is ").append(atom).append(" true at t=").append(time).append("?\n");
        sb.append("=".repeat(70)).append("\n\n");

        DerivationInfo info = provenance.getDerivation(atom, time);

        if (info == null) {
            sb.append("This is a BASE FACT (not derived, directly stated).\n");
        } else {
            sb.append("DERIVED by rule: ").append(info.getRuleName()).append("\n\n");
            sb.append("Variable bindings:\n");
            for (Map.Entry<String, String> entry : info.getSubstitution().entrySet()) {
                sb.append("  ").append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
            }
            sb.append("\nBecause the following facts were true:\n");

            int i = 1;
            for (Provenance.AtomTimeKey source : info.getSourceFacts()) {
                sb.append("  ").append(i++).append(". ");
                sb.append(source.getAtom()).append(" at t=").append(source.getTime());

                // Recursively explain sources
                DerivationInfo sourceInfo = provenance.getDerivation(source.getAtom(), source.getTime());
                if (sourceInfo != null) {
                    sb.append(" [derived by ").append(sourceInfo.getRuleName()).append("]");
                } else {
                    sb.append(" [base fact]");
                }
                sb.append("\n");
            }

            // Show full derivation tree
            sb.append("\n").append("Full Derivation Tree:\n");
            sb.append(provenance.getDerivationTree(atom, time).toTreeString());
        }

        return sb.toString();
    }

    /**
     * Generates a "Why Not?" explanation - why a fact is NOT true.
     *
     * @param atom the fact to explain
     * @param time the timestep
     * @param interpretation the reasoning result
     * @return explanation
     */
    public String explainWhyNot(Atom atom, int time, ReasoningInterpretation interpretation) {
        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(70)).append("\n");
        sb.append("Why is ").append(atom).append(" NOT true at t=").append(time).append("?\n");
        sb.append("=".repeat(70)).append("\n\n");

        // Check if it exists
        Set<Atom> factsAtTime = interpretation.getFactsAt(time);
        if (factsAtTime.contains(atom)) {
            sb.append("ERROR: This fact IS true! Use explainWhy() instead.\n");
            return sb.toString();
        }

        sb.append("Analyzing rules that could derive ").append(atom.getPredicate()).append("...\n\n");

        // TODO: Find rules that have this predicate in the head
        sb.append("This fact is not true because:\n");
        sb.append("  - No rule derived it\n");
        sb.append("  - It was not stated as a base fact\n\n");

        sb.append("To make this fact true, you could:\n");
        sb.append("  1. Add it as a base fact\n");
        sb.append("  2. Add a rule that derives it\n");
        sb.append("  3. Check if required preconditions are missing\n");

        return sb.toString();
    }

    /**
     * Generates an HTML report with interactive derivation tree.
     *
     * @param atom the fact to explain
     * @param time the timestep
     * @return HTML string
     */
    public String generateHTMLReport(Atom atom, int time) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n<html>\n<head>\n");
        html.append("<title>Derivation Report: ").append(atom).append("</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }\n");
        html.append(".header { background: #2c3e50; color: white; padding: 20px; border-radius: 5px; }\n");
        html.append(".fact { background: white; padding: 15px; margin: 10px 0; border-left: 4px solid #3498db; }\n");
        html.append(".rule { color: #e74c3c; font-weight: bold; }\n");
        html.append(".base-fact { color: #27ae60; }\n");
        html.append(".tree { font-family: monospace; white-space: pre; background: #ecf0f1; padding: 15px; border-radius: 5px; }\n");
        html.append(".binding { background: #f39c12; color: white; padding: 2px 6px; border-radius: 3px; }\n");
        html.append("</style>\n");
        html.append("</head>\n<body>\n");

        html.append("<div class='header'>\n");
        html.append("<h1>Derivation Report</h1>\n");
        html.append("<h2>").append(atom).append(" at t=").append(time).append("</h2>\n");
        html.append("</div>\n");

        DerivationInfo info = provenance.getDerivation(atom, time);

        if (info == null) {
            html.append("<div class='fact'>\n");
            html.append("<p class='base-fact'>✓ This is a BASE FACT (directly stated)</p>\n");
            html.append("</div>\n");
        } else {
            html.append("<div class='fact'>\n");
            html.append("<p>Derived by rule: <span class='rule'>").append(info.getRuleName()).append("</span></p>\n");

            if (!info.getSubstitution().isEmpty()) {
                html.append("<p><b>Variable Bindings:</b></p>\n<ul>\n");
                for (Map.Entry<String, String> entry : info.getSubstitution().entrySet()) {
                    html.append("<li><span class='binding'>").append(entry.getKey()).append("</span> = ")
                        .append(entry.getValue()).append("</li>\n");
                }
                html.append("</ul>\n");
            }

            html.append("<p><b>Because these facts were true:</b></p>\n<ul>\n");
            for (Provenance.AtomTimeKey source : info.getSourceFacts()) {
                html.append("<li>").append(source.getAtom()).append(" at t=").append(source.getTime());

                DerivationInfo sourceInfo = provenance.getDerivation(source.getAtom(), source.getTime());
                if (sourceInfo != null) {
                    html.append(" <span style='color: #7f8c8d;'>[via ").append(sourceInfo.getRuleName()).append("]</span>");
                } else {
                    html.append(" <span class='base-fact'>[base fact]</span>");
                }
                html.append("</li>\n");
            }
            html.append("</ul>\n");
            html.append("</div>\n");

            // Full tree
            html.append("<div class='fact'>\n");
            html.append("<h3>Full Derivation Tree</h3>\n");
            html.append("<div class='tree'>");
            html.append(escapeHtml(provenance.getDerivationTree(atom, time).toTreeString()));
            html.append("</div>\n");
            html.append("</div>\n");
        }

        html.append("</body>\n</html>");

        return html.toString();
    }

    /**
     * Generates a JSON representation for web UIs.
     *
     * @param atom the fact to explain
     * @param time the timestep
     * @return JSON string
     */
    public String generateJSON(Atom atom, int time) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"fact\": \"").append(escapeJson(atom.toString())).append("\",\n");
        json.append("  \"time\": ").append(time).append(",\n");

        DerivationInfo info = provenance.getDerivation(atom, time);

        if (info == null) {
            json.append("  \"type\": \"base_fact\",\n");
            json.append("  \"derivedBy\": null\n");
        } else {
            json.append("  \"type\": \"derived\",\n");
            json.append("  \"derivedBy\": \"").append(escapeJson(info.getRuleName())).append("\",\n");

            json.append("  \"substitution\": {\n");
            List<String> bindings = new ArrayList<>();
            for (Map.Entry<String, String> entry : info.getSubstitution().entrySet()) {
                bindings.add("    \"" + escapeJson(entry.getKey()) + "\": \"" + escapeJson(entry.getValue()) + "\"");
            }
            json.append(String.join(",\n", bindings)).append("\n  },\n");

            json.append("  \"sources\": [\n");
            List<String> sources = new ArrayList<>();
            for (Provenance.AtomTimeKey source : info.getSourceFacts()) {
                sources.add("    {\"atom\": \"" + escapeJson(source.getAtom().toString()) +
                           "\", \"time\": " + source.getTime() + "}");
            }
            json.append(String.join(",\n", sources)).append("\n  ]\n");
        }

        json.append("}");
        return json.toString();
    }

    /**
     * Generates a side-by-side comparison of two facts.
     *
     * @param atom1 first fact
     * @param time1 first timestep
     * @param atom2 second fact
     * @param time2 second timestep
     * @return comparison string
     */
    public String compareDerivations(Atom atom1, int time1, Atom atom2, int time2) {
        StringBuilder sb = new StringBuilder();

        sb.append("=".repeat(70)).append("\n");
        sb.append("Comparing Derivations\n");
        sb.append("=".repeat(70)).append("\n\n");

        DerivationTree tree1 = provenance.getDerivationTree(atom1, time1);
        DerivationTree tree2 = provenance.getDerivationTree(atom2, time2);

        sb.append("Fact 1: ").append(atom1).append(" at t=").append(time1).append("\n");
        sb.append("  Depth: ").append(tree1.getDepth()).append("\n");
        sb.append("  Base facts: ").append(tree1.getBaseFacts().size()).append("\n\n");

        sb.append("Fact 2: ").append(atom2).append(" at t=").append(time2).append("\n");
        sb.append("  Depth: ").append(tree2.getDepth()).append("\n");
        sb.append("  Base facts: ").append(tree2.getBaseFacts().size()).append("\n\n");

        // Find common base facts
        Set<Provenance.AtomTimeKey> baseFacts1 = tree1.getBaseFacts();
        Set<Provenance.AtomTimeKey> baseFacts2 = tree2.getBaseFacts();

        Set<Provenance.AtomTimeKey> common = new HashSet<>(baseFacts1);
        common.retainAll(baseFacts2);

        if (!common.isEmpty()) {
            sb.append("Common Base Facts:\n");
            for (Provenance.AtomTimeKey bf : common) {
                sb.append("  - ").append(bf).append("\n");
            }
        } else {
            sb.append("No common base facts.\n");
        }

        return sb.toString();
    }

    /**
     * Generates a summary of all derivations by rule.
     *
     * @return summary string
     */
    public String generateRuleSummary() {
        Map<String, Integer> stats = provenance.getDerivationStats();

        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(70)).append("\n");
        sb.append("Rule Usage Summary\n");
        sb.append("=".repeat(70)).append("\n\n");

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(stats.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        int totalDerivations = sorted.stream().mapToInt(Map.Entry::getValue).sum();
        sb.append("Total Derivations: ").append(totalDerivations).append("\n\n");

        sb.append(String.format("%-40s %10s %10s\n", "Rule Name", "Count", "% of Total"));
        sb.append("-".repeat(70)).append("\n");

        for (Map.Entry<String, Integer> entry : sorted) {
            double percentage = (entry.getValue() * 100.0) / totalDerivations;
            sb.append(String.format("%-40s %10d %9.1f%%\n",
                    entry.getKey(), entry.getValue(), percentage));
        }

        return sb.toString();
    }

    // Helper methods

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
