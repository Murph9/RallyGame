package world.osm;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.jme3.math.Vector3f;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class OsmReader {

    public class Road {
        final String subRoadType;
        final Vector3f a;
        final Vector3f b;

        public Road(String subRoadType, Vector3f a, Vector3f b) {
            this.subRoadType = subRoadType;
            this.a = a;
            this.b = b;
        }
    }

    private Vector3f middle;
    private List<Road> lines;

    public void load(InputStream fileStream) throws SAXException, IOException, ParserConfigurationException {
        this.lines = new LinkedList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        Document document = documentBuilder.parse(fileStream);

        Map<String, Vector3f> points = new HashMap<>();

        NodeList childNodes = document.getDocumentElement().getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);

            if ("bounds".equals(childNode.getNodeName())) {
                Element childElement = (Element) childNode;

                double minLat = Double.parseDouble(childElement.getAttribute("minlat"));
                double maxLat = Double.parseDouble(childElement.getAttribute("maxlat"));
                double minLon = Double.parseDouble(childElement.getAttribute("minlon"));
                double maxLon = Double.parseDouble(childElement.getAttribute("maxlon"));

                this.middle = new Vector3f((float)((maxLat + minLat) / 2d), 0f, (float)((maxLon + minLon) / 2d));
            }

            if (childNode.getNodeName().equals("node")) {
                Element childElement = (Element) childNode;
                Vector3f p = new Vector3f(Float.parseFloat(childElement.getAttribute("lat")), 0f,
                        Float.parseFloat(childElement.getAttribute("lon")));
                points.put(childElement.getAttribute("id"), p);
            }

            if (childNode.getNodeName().equals("way")) {
                NodeList wayChildren = childNode.getChildNodes();
                
                //first check that it is a road
                boolean isRoad = false;
                String roadType = null;
                for (int k = 0; k < wayChildren.getLength(); k++) {
                    Node wayChild = wayChildren.item(k);
                    if (wayChild.getNodeName().equals("tag")) {
                        String value = ((Element)wayChild).getAttribute("k");
                        if (value != null && value.equals("highway")) {
                            isRoad = true;
                            roadType = ((Element) wayChild).getAttribute("v");
                        }
                    }
                }
                if (!isRoad)
                    continue;

                Vector3f p = null;
                for (int j = 0; j < wayChildren.getLength(); j++) {
                    Node wayChild = wayChildren.item(j);
                    if (wayChild.getNodeName().equals("nd")) {
                        Element wayChildElement = (Element) wayChild;
                        String pointId = wayChildElement.getAttribute("ref");
                        Vector3f curP = points.get(pointId);
                        if (p != null) {
                            lines.add(new Road(roadType, p, curP));
                        }
                        p = curP;
                    }
                }
            }
        }
    }

    public List<Road> getWithScale(double scale) {

        return lines.stream().map(x -> new Road(x.subRoadType, convertToLocal(x.a, middle, scale),
                convertToLocal(x.b, middle, scale))).collect(Collectors.toList());
    }

    private Vector3f convertToLocal(Vector3f p, Vector3f offset, double scale) {
        return new Vector3f(subThenMult(p.x, offset.x, scale), subThenMult(p.y, offset.y, scale),
                subThenMult(p.z, offset.z, scale));
    }

    private float subThenMult(float input, float sub, double scale) {
        return (float)((input - sub) * scale);
    }

}
