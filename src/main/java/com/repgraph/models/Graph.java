package com.repgraph.models;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Graph {

    private String id;
    private String input;
    private String source;
    private ArrayList<Node> nodes;
    private ArrayList<Token> tokens;
    private int top;
    private Map<String, String> colorMap;

    /**
     * Constructor which takes in a Json object representation of a DMRS graph and
     * extracts the id, input, source, tokens, nodes and edges
     * 
     * @param g the Json object representation of a DMRS graph
     */
    public Graph(JSONObject g) {
        tokens = new ArrayList<>();
        nodes = new ArrayList<>();

        id = (String) g.get("id");
        input = (String) g.get("input");
        source = (String) g.get("source");

        JSONArray tokenArr = (JSONArray) g.get("tokens");
        for (int i = 0; i < tokenArr.size(); i++) {
            JSONObject jToken = (JSONObject) tokenArr.get(i);
            tokens.add(new Token(jToken));
        }

        JSONArray nodeArr = (JSONArray) g.get("nodes");
        for (int i = 0; i < nodeArr.size(); i++) {
            JSONObject jNode = (JSONObject) nodeArr.get(i);
            nodes.add(new Node(jNode, tokens));
        }

        JSONArray edgeArr = (JSONArray) g.get("edges");
        for (int i = 0; i < edgeArr.size(); i++) {
            JSONObject jEdge = (JSONObject) edgeArr.get(i);
            // add edge to source node
            Node s = nodes.get((int) (long) jEdge.get("source"));
            s.addEdge(jEdge, nodes);
            // add source to dest adjacency list
            Node dest = nodes.get((int) (long) jEdge.get("target"));
            dest.addAdjacent(s);
        }

        JSONArray topArr = (JSONArray) g.get("tops");
        // if graph has top, store index, else set index to -1. If doesn't have top,
        // topArr might not exist or might be empty
        top = topArr != null && !topArr.isEmpty() ? (int) (long) topArr.get(0) : -1;

        colorMap = Map.of(
            "blue","#40c5e6", // for abstract nodes
            "lightGreen", "#40e661", // for surface nodes and span match
            "red", "#e66140", // for tokens, subsetNode
            "pink", "#e640c5", // for top 
            "default", "#bababa", // grey
            "brown", "#a93316", // cut vertices
            "darkGreen", "#107c26" // label match
        );
    }

    /**
     * Constructor for testing - takes in a list of nodes and assigns them to the
     * graph
     * 
     * @param nodes the list of nodes to be assigned to the graph
     */
    public Graph(List<Node> nodes) {
        this.nodes = new ArrayList<>(nodes);
    }

    /**
     * Constructor for testting which takes in a list of nodes and assigns them to the graph
     * @param nodes the list of nodes to be assigned to the graph
     */
    public Graph(ArrayList<Node> nodes){
        this.nodes = nodes;
    }

    /**
     * Compare two graphs and record the difference and similarities between them. The edges and nodes of the two graphs are compared
     * @param other the other graph to be used in the comparison
     */
    public void compareGraph(Graph other){
        ArrayList<Node> otherNodes = other.getNodes();
        //reset the match variables for both the graphs
        resetGraphMatches();
        other.resetGraphMatches();
        //only need to check nodes if the node array is instantiated in the other graph and contains elements
        if (other.getNodeCount() > 0) {
            //Can have multiple nodes with same node label, so node could be equal to multiple other nodes
            for (Node n : nodes) {
                for (Node o : otherNodes) {
                    if (n.equals(o)) {
                        n.compareNode(o);
                    }
                }
            }
        }
    }

    /**
     * Reset the match labels in the nodes and edges of the graph
     */
    public void resetGraphMatches(){
        //set all values for match variable in this graph's nodes and edges to false
        for (Node n : nodes) {
            n.setLableMatch(false);
            n.setSpanMatch(false);
            for (Edge e : n.getEdges()) {
                e.setEdgeMatch(false);
            }
        }
    }

    /**
     * Method which marks the nodes and edges of the subgraph generated by the given rootNode,
     * where rootNode is a node in the graph. The subgraph is marked by the label match variable
     * in the nodes and the edge match variable in the edges
     *
     * @param rootNode the node in the graph for which the subgraph must be marked
     */
    public void markSubgraph(Node rootNode) {
        resetGraphMatches();
        markNode(rootNode);
    }

    //method which recursively marks the nodes and edges of the subgraph generated by n
    private void markNode(Node n) {
        //use label match variable in node and edge match variable in edge to mark subgraph
        n.setLableMatch(true);
        for (Edge e : n.getEdges()) {
            e.setEdgeMatch(true);
            //If destination node has not already been processed and marked as part of the subgraph
            if (!(e.getDest().getLableMatch())) {
                markNode(e.getDest());
            }
        }
    }

    /**
     * Returns the array containing all the nodes in the graph
     * @return the ArrayList containing all the nodes in the graph
     */
    public ArrayList<Node> getNodes() {
        return nodes;
    }

    /**
     * Method to override toString() for the Graph class
     * @return a string representation of a graph comprised of its tokens, nodes and top node
     * Returns the id for the graph
     * 
     * @return the id of the graph
     */
    public String getId() {
        return id;
    }

    /**
     * Method to override toString() for the Graph class
     * @return a string representation of a graph comprised of its tokens, nodes and top node
     * Returns the id for the graph
     * 
     * @return the id of the graph
     */
    public int getTop() {
        return top;
    }

    /**
     * Return the sentence the graph represents
     * 
     * @return the sentence the graph represents
     */
    public String getSentence() {
        return input;
    }

    /**
     * Return the number of nodes in the graph i.e the size of the node array
     * 
     * @return the number of nodes in the graph
     */
    public int getNodeCount() {
        return nodes.size();
    }

     /**
     * Return the number of tokens in the graph i.e the size of the token array
     * 
     * @return the number of tokens in the graph
     */
    public int getTokenCount() {
        return tokens.size();
    }

    /**
     * Finds and returns a node given the node's ID as input
     * @param nodeID id of the node to be found
     * @return the node with the given id
     */
    public Node findNodeById(String nodeID){
        for (Node n : nodes) {
            String currentID = "n" + String.valueOf(n.getID());
            if (currentID.equals(nodeID)){
                return n;
            }
        }
        return null;
    }

    /**
     * Given a nodeID, marks the subgraph and returns json visualisation string to be utilized by sigma.js on the frontend. 
     * @param analysisType always "subgraph", used by the getVisualisationJson() method.
     * @param rootNode the id of the selected node - the node for which the subgraph is to be displayed
     * @return json visualisation string
     */
    public String getSubgraphVisualisation(String analysisType, String rootNode){
        String subgraphJsonString = "";
        markSubgraph(findNodeById(rootNode));
        subgraphJsonString = this.getVisualisationJson(true, analysisType);
        return subgraphJsonString;
    }

    /**
     * Returns json visualisation string to be utilized by sigma.js on the frontend
     * 
     * @param tokenView boolean stipulating whether the token view has been selected
     * @return json visualisation string
     */
    public String getVisualisationJson(boolean tokenView, String analysisType) {
        /* Variables */
        JSONObject jsonObject = new JSONObject(); // JSON Object which will contain the nodes array and the edges array
        JSONArray nodeArray = new JSONArray(); // JSON Array containing the nodes in the graph
        JSONArray edgeArray = new JSONArray(); // JSON Array containing the edges in the graph
        int maxX = 10; // Largest x co-ordinate (for tokens)
        
        /* Get layered graph. */
        NodePosition layeredGraph = new NodePosition(this);
        ArrayList<ArrayList<Node>> layers = layeredGraph.getLayers();
        int numLayers = layers.size();
       
        /* Get relevant token information and add token nodes and edges to the array */
        if (tokenView) {
            tokensToJsonString(nodeArray, numLayers, maxX, analysisType);
            tokenEdgesToJsonString(edgeArray);
        }

        /* Get relevant node information and add nodes to node array */
        nodesToJsonString(nodeArray,layers,maxX,analysisType);
        /* Get relevant edge information and add edges to edge array */ 
        edgesToJsonString(edgeArray,analysisType);

        // Add nodes and edges to graph array.
        jsonObject.put("nodes", nodeArray);
        jsonObject.put("edges", edgeArray);

        return jsonObject.toJSONString();
    }

    /**
     * Configures token nodes and adds them to node array.
     * @param nodeArray the node array for the tokens to be added to
     * @param numLayers the number of layers that nodes are organised into
     * @param maxX the largest X-coordinate
     * @param analysisType which analysis tool this is being used for (subgraph, graphComparison etc.)
     */
    private void tokensToJsonString(JSONArray nodeArray, int numLayers, int maxX, String analysisType){
        String color = colorMap.get("red");
        if (!analysisType.equals("none")) {
            color = colorMap.get("default");
        }
        
        double tokenX = 0; // Starting x co-ordinate for tokens
        double tokenY = (numLayers/2) + 0.5; // Starting y co-ordinate for tokens
        for (int i=0; i < getTokenCount(); i++){
            JSONObject nodeArrObject = new JSONObject();
            // Dynamically adjust space between tokens
            if (i > 0){
                int lenOfPrevToken = tokens.get(i-1).getForm().length();
                for (int j=0; j<lenOfPrevToken; j+=2){
                    tokenX += 0.2;
                }
            }
            // Start new row of tokens
            if (tokenX > maxX) {
                tokenX = 0;
                tokenY += 0.2;
            }
            // Add token attributes to JSON object
            nodeArrObject.put("id", "t" + tokens.get(i).getIndex());
            nodeArrObject.put("label", tokens.get(i).getForm());
            nodeArrObject.put("x",tokenX);
            nodeArrObject.put("y",tokenY);
            nodeArrObject.put("size","0.5");
            nodeArrObject.put("color", color);
            
            nodeArray.add(nodeArrObject);
        }
    }

      /**
      * Configures token edges and adds them to edge array.
      * @param edgeArray the edge array for the tokens to be added to
      */
    private void tokenEdgesToJsonString(JSONArray edgeArray){
        int tokenCounter = 0; // Counter for token ids
        for (int i=0; i<getNodeCount(); i++){
            for (int j=0; j<nodes.get(i).getNumTokens(); j++){
                JSONObject tokenEdgeArrObject = new JSONObject();
                tokenEdgeArrObject.put("id", "te" + tokenCounter);
                tokenEdgeArrObject.put("source", "n" + nodes.get(i).getID());
                tokenEdgeArrObject.put("target", "t"+ nodes.get(i).getTokens().get(j).getIndex());
                if(nodes.get(i).getTokens().get(j).getCarg() != null){
                    tokenEdgeArrObject.put("label", nodes.get(i).getTokens().get(j).getLemma() +"/"+ nodes.get(i).getTokens().get(j).getCarg());
                }
                tokenEdgeArrObject.put("label", nodes.get(i).getTokens().get(j).getLemma());
                tokenCounter++;
                edgeArray.add(tokenEdgeArrObject);
            }
        }
    }

    /**
     * Configures nodes and adds them to node array
     * @param nodeArray the node array for the tokens to be added to
     * @param layers the layers that nodes are organised into
     * @param maxX the largest X-coordinate
     * @param analysisType which analysis tool this is being used for (subgraph, graphComparison etc.)
     */
    private void nodesToJsonString(JSONArray nodeArray, ArrayList<ArrayList<Node>> layers, int maxX, String analysisType){
        String color = colorMap.get("default");
        
        for (int i=0; i < layers.size(); i++){ // num layers
            int numNodesinLayer = layers.get(i).size();
            double pX =  (double) maxX/numNodesinLayer; // length/x
            for (int j=0; j < layers.get(i).size(); j++){ // num nodes in layer, so for each node, j is nodes index in layer
                int size = 1;
                // always want top node to be bigger
                if (layers.get(i).get(j).getID() == nodes.get(getTop()).getID()) {
                    size = 2;
                }
                if (analysisType.equals("none")) {
                    if (layers.get(i).get(j).isAbstractNode()){
                        color = colorMap.get("blue");
                    }
                    else {
                        color = colorMap.get("lightGreen");
                    }
                    if (layers.get(i).get(j).getID() == nodes.get(getTop()).getID()) {
                        color = colorMap.get("pink");
                    }
                }
                else if (analysisType.equals("subgraph") || analysisType.equals("longestPath") || analysisType.equals("cutVertices")){
                    if (layers.get(i).get(j).getLableMatch()){ 
                        color = colorMap.get("red");
                    }
                    else {
                        color = colorMap.get("default");
                    }
                }                
                else if (analysisType.equals("graphComparison") || analysisType.equals("graphCompareOriginal")){
                    if (layers.get(i).get(j).getLableMatch() && layers.get(i).get(j).getSpanMatch()){
                        color = colorMap.get("red");
                    }
                    else if (layers.get(i).get(j).getLableMatch()){
                        color = colorMap.get("darkGreen");
                    }
                    else if (layers.get(i).get(j).getSpanMatch()){
                        color = colorMap.get("lightGreen");
                    }
                    else {
                        color = colorMap.get("default");
                    }
                }
                // want cut vertices to also show longest path
                if (analysisType.equals("cutVertices")){
                    if (layers.get(i).get(j).getSpanMatch()){
                        color = colorMap.get("brown");
                    }
                }
                double positionX = pX*j; // X coord of Node
                double positionY = i*0.5;
                JSONObject nodeArrObject = new JSONObject();
                
                
                nodeArrObject.put("id", "n" + layers.get(i).get(j).getID());
                nodeArrObject.put("label", layers.get(i).get(j).getLabel());
                nodeArrObject.put("x", positionX);
                nodeArrObject.put("y", positionY);
                nodeArrObject.put("size",size);
                nodeArrObject.put("color",color);
                nodeArray.add(nodeArrObject);
            }
        }
    }

    /**
     * Configures the edges and adds them to the edge array
     * @param edgeArray the edge array to add edges to
     * @param analysisType which analysis tool this is being used for (subgraph, graphComparison etc.)
     */
    private void edgesToJsonString(JSONArray edgeArray, String analysisType){
        String color = colorMap.get("default");
        int edgeCounter = 0; // Counter for edge ids
        for (int i=0; i < getNodeCount(); i++){
            int numEdges = nodes.get(i).getNumberEdges();
            for (int j=0; j < numEdges; j++){
                if (analysisType.equals("subgraph") || analysisType.equals("longestPath") || analysisType.equals("cutVertices")){
                    if (nodes.get(i).getEdges().get(j).getEdgeMatch()){
                        color = colorMap.get("red");
                    }
                    else {
                        color = colorMap.get("default");
                    }
                }
                if (analysisType.equals("graphComparison") || analysisType.equals("graphCompareOriginal")){
                    if (nodes.get(i).getEdges().get(j).getEdgeMatch()){
                        color = colorMap.get("lightGreen");
                    }
                    else {
                        color = colorMap.get("default");
                    }
                }
                JSONObject edgeArrObject = new JSONObject();
                edgeArrObject.put("id", "e" + edgeCounter);
                edgeArrObject.put("source", "n" + nodes.get(i).getID());
                edgeArrObject.put("target", "n" + nodes.get(i).getEdges().get(j).getDest().getID());
                edgeArrObject.put("label", nodes.get(i).getEdges().get(j).getFullLabel());
                edgeArrObject.put("color", color);
                edgeArray.add(edgeArrObject);
                edgeCounter++;
            }
        }
    }

    /**
     * Method to override toString() for the Graph class
     * 
     * @return a string representation of a graph comprised of its tokens, nodes and
     *         top node
     */
    public String toString() {
        StringBuilder tokenBuilder = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            tokenBuilder.append(" " + tokens.get(i).toString() + "\n");
        }
        StringBuilder nodeBuilder = new StringBuilder();
        for (int i = 0; i < nodes.size(); i++) {
            nodeBuilder.append(" " + nodes.get(i).toString() + "\n");
        }
        return " top Index: " + top + " tokens: " + tokenBuilder.toString() + " nodes: " + nodeBuilder.toString()
                + "\n";
    }
}
