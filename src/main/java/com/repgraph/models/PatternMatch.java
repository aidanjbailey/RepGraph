package com.repgraph.models;

import java.util.ArrayList;

//finds graphs that match the pattern and marks first subgraph found
public class PatternMatch {

    //Node whose subgraph will be matched
    private Node node;
    //keep track of the nodes in the graph that match the pattern so they can be marked
    private ArrayList<Node> matchedNodes;

    /**
     * Constructor which takes in a node representing a subgraph pattern i.e. is connected
     * to every other node in the subgraph pattern and assigns it the class PatternMatch node
     * 
     * @param n the node representing the subgraph pattern
     */
    public PatternMatch(Node n) {
        node = n;
        matchedNodes = new ArrayList<>();
    }

    /**
     * Takes in a graph and checks whether it contains subgraphs matching the subgraph pattern.
     * If the graph contains at least one set of nodes and edges which match the subgraph, true
     * is returned and the nodes and edges matching the pattern are marked. Else false is returned
     * 
     * @param g the graph to be checked for the subgraph pattern
     * @return true if the graph contains at least one match, false otherwise
     */
    public boolean graphMatch(Graph g) {
        //reset labels for graph before marking matches
        g.resetGraphMatches();
        boolean matched = false;
        String patternLabel = node.getLabel();
        for (Node other : g.getNodes()) {
            if (patternLabel.equals(other.getLabel())) {
                matchedNodes.add(other);
                //check whether the subgraph of pattern node and part of the other nodes subgraph match
                boolean currentMatched = nodeMatch(node, other);
                if (currentMatched) {
                    markLabels();
                    //if this is the first match, mark that their is a match in the graph before keeping searching
                    //remove if only searching for first occurrence
                    if(!matched) {
                        matched = true;
                    }
                }
                matchedNodes.clear();
            }
        }
        return matched;
    }

    //recrusive method which checks if the subgraph pattern is a subgraph of the subgraph generated by a node in
    //the graph
    private boolean nodeMatch(Node pattern, Node test) {
        //if the labels of the nodes match and the pattern node has no elements, the pattern is a subgraph of graph
        //generated by the test node and thus a match
        if (pattern.getNumberEdges() == 0) return true;
        //if the pattern has more edges than the test, there is no way the sub-graphs could match
        if (pattern.getNumberEdges() > test.getNumberEdges()) return false;
        //iterate through all edges of the pattern node and check them against the edges of the test node
        for (Edge patternEdge : pattern.getEdges()) {
            boolean patternFound = false;
            Node patternDest = patternEdge.getDest();
            for (Edge testEdge : test.getEdges()) {
                Node testDest = testEdge.getDest();
                //if the labels of the destination nodes and edges match, test the subgraphs
                if (patternEdge.getLabel().equals(testEdge.getLabel()) && patternDest.getLabel().equals(testDest.getLabel())) {
                    matchedNodes.add(testDest);
                    patternFound = nodeMatch(patternDest, testDest);
                    if (patternFound) {
                        break;
                    }
                    //if pattern has not been found in the subgraph generated by the testDest, remove it from
                    // the matchedNodes array (where it will be the last element)
                    matchedNodes.remove(matchedNodes.size() - 1);
                }
            }
            //if there is no match for the pattern edge, we can stop looking
            if (!patternFound) return false;
        }
        return true;
    }

    /**
     * Returns the matchedNodes array
     * @return the matchedNodes array
     */
    public ArrayList<Node> getMatchedNodes() {
        return matchedNodes;
    }

    //Marks the node and edge labels of a graph that match the subgraph pattern using the nodes
    //stored in the matchedNodes array
    private void markLabels() {
        if (!matchedNodes.isEmpty()) {
            //mark the first node label as a match
            matchedNodes.get(0).setLableMatch(true);
            for (int i = 1; i < matchedNodes.size(); i++) {
                Node nodeToLabel = matchedNodes.get(i);
                nodeToLabel.setLableMatch(true);
                //iterate back through list to find edge that matches to the node in the list
                for (int j = i - 1; j >= 0; j--) {
                    for (Edge e : matchedNodes.get(j).getEdges()) {
                        if (e.getDest() == nodeToLabel) {
                            e.setEdgeMatch(true);
                        }
                    }
                }
            }
        }
    }
}
