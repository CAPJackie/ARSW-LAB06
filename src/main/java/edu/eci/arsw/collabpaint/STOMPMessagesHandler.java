package edu.eci.arsw.collabpaint;

import edu.eci.arsw.collabpaint.model.Point;
import edu.eci.arsw.collabpaint.util.JedisUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

@Controller
public class STOMPMessagesHandler {

    @Autowired
    SimpMessagingTemplate msgt;

    ConcurrentHashMap<String, ArrayList<Point>> polygonPoints = new ConcurrentHashMap();

    @MessageMapping("/newpoint.{numdibujo}")
    public void handlePointEvent(Point pt, @DestinationVariable String numdibujo) throws Exception {
            Jedis jedis = JedisUtil.getPool().getResource();
            jedis.getClient().setTimeoutInfinite();  
            
            
            
            Transaction tx = jedis.multi();
            tx.watch("X", "Y");
            tx.rpush("X", String.valueOf(pt.getX()));
            tx.rpush("Y", String.valueOf(pt.getY()));
            List<Object> res = tx.exec();
            
            
            
            
            
            System.out.println("TAMANO: " + res.size()); 
            System.out.println("Nuevo punto recibido en el servidor!:" + pt);
            
            jedis.close();

           

        /*if (polygonPoints.containsKey(numdibujo)) {
            polygonPoints.get(numdibujo).add(pt);
            if (polygonPoints.get(numdibujo).size() > 3) {
                msgt.convertAndSend("/topic/newpolygon." + numdibujo, polygonPoints.get(numdibujo));
                polygonPoints.clear();
            }
        } else {
            ArrayList<Point> puntos = new ArrayList();
            puntos.add(pt);
            polygonPoints.put(numdibujo, puntos);
        }*/
    }
}
