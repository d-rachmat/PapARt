/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.jiii.prezi;

import java.util.HashMap;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;
import processing.core.PGraphics;

/**
 *
 * @author jiii
 */
public class TextField implements Drawable {

    private float x, y;
    private String text = null;
    private String annotations = null;

    public TextField(Element elem) {

        NodeList children = elem.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {

            Node nNode = children.item(i);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                Element e = (Element) nNode;
                if (e.getTagName().equalsIgnoreCase("x")) {

                    x = Float.parseFloat(e.getChildNodes().item(0).getNodeValue());
                }
                if (e.getTagName().equalsIgnoreCase("y")) {
                    y = Float.parseFloat(e.getChildNodes().item(0).getNodeValue());
                }
                if (e.getTagName().equalsIgnoreCase("text")) {
                    text = e.getChildNodes().item(0).getNodeValue();
                }
                if (e.getTagName().equalsIgnoreCase("annotations")) {

                    if (e.getChildNodes().getLength() > 0) {
                        annotations = e.getChildNodes().item(0).getNodeValue();
                    }
                }

            }
        }

        System.out.println("TextField loaded " + x + " " + y);
    }

    @Override
    public void drawSelf(PGraphics graphics) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
