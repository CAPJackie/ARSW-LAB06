/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.eci.arsw.collabpaint.persistence;

import edu.eci.arsw.collabpaint.model.Point;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 *
 * @author Juan David
 */
@Service
public class InMemoryPersistenceService implements PersistenceHandlerService{

    @Autowired
    SimpMessagingTemplate msgt;
    
    private ConcurrentHashMap<String, ArrayList<Point>> polygonPoints = new ConcurrentHashMap();
    
    @Override
    public void handleRequest(Point pt, String numdibujo) {
         System.out.println("Nuevo punto recibido en el servidor!:"+pt);
		if(polygonPoints.containsKey(numdibujo)){
                    polygonPoints.get(numdibujo).add(pt);
                    if(polygonPoints.get(numdibujo).size() > 3){
                        msgt.convertAndSend("/topic/newpolygon."+numdibujo, polygonPoints.get(numdibujo));
                        polygonPoints.clear();
                    }
                }
                else{
                    ArrayList<Point> puntos = new ArrayList();
                    puntos.add(pt);
                    polygonPoints.put(numdibujo, puntos);
                }
    }
    
}
