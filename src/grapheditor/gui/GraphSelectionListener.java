package grapheditor.gui;

import graphs.graph.Edge;
import graphs.graph.GraphVertex;

public interface GraphSelectionListener {

    public void edgeSelected(GraphDrawPanel source, Edge edge);

    public void vertexSelected(GraphDrawPanel source, GraphVertex vertex);

}
