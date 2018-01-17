package grapheditor.gui;

import graphs.graph.Edge;
import graphs.graph.Graph;
import graphs.graph.GraphVertex;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import javax.swing.JPanel;

public class GraphDrawPanel extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    public enum GraphOperation {

        ADD_VERTEX, SELECT_VERTEX, MOVE_VERTEX, DELETE_VERTEX, ADD_EDGE, SELECT_EDGE, DELETE_EDGE;
    }

    private final double HIT_PRECISION = 7; // How close you must click to a vertex or edge in order to select it. Higher values mean you can be further away. Note that this makes it harder to select the right vertex when several are very close.
    private final int VERTEX_SIZE = 5; // Radius in pixels of the vertices
    private Graph graph; // The current graph
    private GraphVertex selectedVertex = null; // The currently selected vertex
    private Edge selectedEdge = null; // The currently selected edge. Invariant: (selectedVertex == null) || (selectedEdge == null), meaning that you can't select both a vertex and and edge.
    private double zoomfactor = 1;
    private int panX = 0;
    private int panY = 0;
    private int mouseX = 0;
    private int mouseY = 0;
    private Collection<GraphSelectionListener> listeners;
    private EnumSet<GraphOperation> enabledOperations = EnumSet.allOf(GraphOperation.class);

    public GraphDrawPanel() {
        initialize();
    }

    private void initialize() {
        setFocusable(true);
        setOpaque(true);
        setBackground(Color.white);

        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        addKeyListener(this);

        graph = new Graph();
        listeners = new ArrayList<GraphSelectionListener>();
    }

    public void addGraphSelectionListener(GraphSelectionListener listener) {
        listeners.add(listener);
    }

    public void removeGraphSelectionListener(GraphSelectionListener listener) {
        listeners.remove(listener);
    }

    public void enableOperation(GraphOperation operation) {
        enabledOperations.add(operation);
    }

    public void disableOperation(GraphOperation operation) {
        enabledOperations.remove(operation);
    }

    public boolean isOperationEnabled(GraphOperation operation) {
        return enabledOperations.contains(operation);
    }

    public void setEnabledOperations(Set<GraphOperation> operations) {
        if (operations.isEmpty()) {
            enabledOperations = EnumSet.noneOf(GraphOperation.class);
        } else {
            enabledOperations = EnumSet.copyOf(operations);
        }
    }

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
        setSelectedVertex(null);
        zoomToGraph();
    }

    public Edge getSelectedEdge() {
        return selectedEdge;
    }

    public GraphVertex getSelectedVertex() {
        return selectedVertex;
    }

    public void zoomToGraph() {
        if (!graph.getVertices().isEmpty()) {
            int margin = 20;

            double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY,
                    maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;

            for (GraphVertex vertex : graph.getVertices()) {
                minX = Math.min(minX, vertex.getX());
                minY = Math.min(minY, vertex.getY());
                maxX = Math.max(maxX, vertex.getX());
                maxY = Math.max(maxY, vertex.getY());
            }

            double zoomfactorX = (maxX - minX) / (getWidth() - 2 * margin);
            double zoomfactorY = (maxY - minY) / (getHeight() - 2 * margin);

            if (zoomfactorY > zoomfactorX) {
                zoomfactor = zoomfactorY;
                panX = (int) Math.round((maxX + minX) / (2 * zoomfactor)) - getWidth() / 2;
                panY = (int) Math.round(maxY / zoomfactor) - getHeight() + margin;
            } else {
                zoomfactor = zoomfactorX;
                panX = (int) Math.round(minX / zoomfactor) - margin;
                panY = (int) Math.round((maxY + minY) / (2 * zoomfactor)) - getHeight() / 2;
            }
        }

        repaint();
    }

    private double xScreenToWorld(int x) {
        return (x + panX) * zoomfactor;
    }

    private double yScreenToWorld(int y) {
        return (getHeight() - y + panY) * zoomfactor;
    }

    private int xWorldToScreen(double x) {
        return (int) Math.round((x / zoomfactor) - panX);
    }

    private int yWorldToScreen(double y) {
        return getHeight() - (int) Math.round((y / zoomfactor) - panY);
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (Edge e : graph.getEdges()) {
            if (e.isVisible()) {
                if (e == selectedEdge) {
                    g.setColor(Color.RED);
                } else {
                    g.setColor(Color.BLACK);
                }

                GraphVertex vA = e.getVA();
                GraphVertex vB = e.getVB();
                g.drawLine(xWorldToScreen(vA.getX()), yWorldToScreen(vA.getY()), xWorldToScreen(vB.getX()), yWorldToScreen(vB.getY()));
            }
        }

        for (GraphVertex v : graph.getVertices()) {
            if (v.isVisible()) {
                g.setColor(Color.blue);
                g.fillOval(xWorldToScreen(v.getX()) - VERTEX_SIZE, yWorldToScreen(v.getY()) - VERTEX_SIZE, 2 * VERTEX_SIZE, 2 * VERTEX_SIZE);

                if (v == selectedVertex) {
                    g.setColor(Color.RED);
                    ((Graphics2D) g).setStroke(new BasicStroke(2));
                } else {
                    g.setColor(Color.BLACK);
                    ((Graphics2D) g).setStroke(new BasicStroke());
                }
                g.drawOval(xWorldToScreen(v.getX()) - VERTEX_SIZE, yWorldToScreen(v.getY()) - VERTEX_SIZE, 2 * VERTEX_SIZE, 2 * VERTEX_SIZE);
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (e.isControlDown() && enabledOperations.contains(GraphOperation.ADD_EDGE)) {
                if (selectedVertex != null) {
                    double wX = xScreenToWorld(e.getX());
                    double wY = yScreenToWorld(e.getY());

                    GraphVertex v = graph.getVertexAt(wX, wY, zoomfactor * HIT_PRECISION);

                    if (v == null && enabledOperations.contains(GraphOperation.ADD_VERTEX)) {
                        // Add a vertex, connect it to the selected vertex and select the new vertex
                        GraphVertex newVertex = new GraphVertex(wX, wY);
                        graph.addVertex(newVertex);
                        graph.addEdge(newVertex, selectedVertex);
                        setSelectedVertex(newVertex);
                    } else if (v != null) {
                        Edge edge = new Edge(selectedVertex, v);

                        if (selectedVertex != v && !graph.getEdges().contains(edge)) {
                            graph.addEdge(selectedVertex, v);
                        }
                    }

                    repaint();
                }
            } else {
                double wX = xScreenToWorld(e.getX());
                double wY = yScreenToWorld(e.getY());

                GraphVertex v = (enabledOperations.contains(GraphOperation.SELECT_VERTEX) ? graph.getVertexAt(wX, wY, zoomfactor * HIT_PRECISION) : null);

                if (v != null) {
                    setSelectedVertex(v);
                } else {
                    Edge edge = (enabledOperations.contains(GraphOperation.SELECT_EDGE) ? graph.getEdgeAt(wX, wY, zoomfactor * HIT_PRECISION) : null);

                    if (edge != null) {
                        setSelectedEdge(edge);
                    } else if (enabledOperations.contains(GraphOperation.ADD_VERTEX)) {
                        GraphVertex newVertex = new GraphVertex(wX, wY);
                        graph.addVertex(newVertex);
                        setSelectedVertex(newVertex);
                    }
                }

                repaint();
            }
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            // start panning, store the current mouse position
            mouseX = e.getX();
            mouseY = e.getY();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if ((e.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) == MouseEvent.BUTTON3_DOWN_MASK) {
            // pan
            panX += mouseX - e.getX();
            panY += e.getY() - mouseY;

            mouseX = e.getX();
            mouseY = e.getY();

            repaint();
        } else if (selectedVertex != null && enabledOperations.contains(GraphOperation.MOVE_VERTEX)) {
            selectedVertex.setX(xScreenToWorld(e.getX()));
            selectedVertex.setY(yScreenToWorld(e.getY()));

            repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        double factor = (e.getWheelRotation() < 0 ? 10.0 / 11.0 : 11.0 / 10.0);

        zoomfactor *= factor;

        int centerX = e.getX();
        int centerY = getHeight() - e.getY();
        panX = (int) Math.round((centerX + panX) / factor - centerX);
        panY = (int) Math.round((centerY + panY) / factor - centerY);

        repaint();
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DELETE) {
            if (selectedVertex != null && enabledOperations.contains(GraphOperation.DELETE_VERTEX)) {
                graph.removeVertex(selectedVertex);
                deselectVertex();
            } else if (selectedEdge != null && enabledOperations.contains(GraphOperation.DELETE_EDGE)) {
                graph.removeEdge(selectedEdge);
                deselectEdge();
            }

            repaint();
        } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            zoomToGraph();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    private void setSelectedVertex(GraphVertex v) {
        deselectEdge();

        if (v != selectedVertex) {
            selectedVertex = v;

            for (GraphSelectionListener list : listeners) {
                list.vertexSelected(this, v);
            }

            requestFocus();
        }
    }

    private void setSelectedEdge(Edge e) {
        deselectVertex();

        if (e != selectedEdge) {
            selectedEdge = e;

            for (GraphSelectionListener list : listeners) {
                list.edgeSelected(this, e);
            }

            requestFocus();
        }
    }

    private void deselectVertex() {
        // Deselect the current selected vertex
        if (selectedVertex != null) {
            selectedVertex = null;

            for (GraphSelectionListener list : listeners) {
                list.edgeSelected(this, null);
            }
        }
    }

    private void deselectEdge() {
        // Deselect the current selected edge
        if (selectedEdge != null) {
            selectedEdge = null;

            for (GraphSelectionListener list : listeners) {
                list.edgeSelected(this, null);
            }
        }
    }
}
