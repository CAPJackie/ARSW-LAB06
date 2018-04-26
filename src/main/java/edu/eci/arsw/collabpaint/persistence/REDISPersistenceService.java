/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.eci.arsw.collabpaint.persistence;

import edu.eci.arsw.collabpaint.model.Point;
import edu.eci.arsw.collabpaint.util.JedisUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

/**
 *
 * @author Juan David
 */
//@Service
public class REDISPersistenceService implements PersistenceHandlerService{

    @Autowired
    SimpMessagingTemplate msgt;
    
    private CopyOnWriteArrayList<Point> polygon = new CopyOnWriteArrayList<>();
    
    private static final String luaScript = "local xVal,yVal; \n"
                + "if (redis.call('LLEN','X')==4) then \n"
                + "	xVal=redis.call('LRANGE','X',0,-1);\n"
                + "	yVal=redis.call('LRANGE','Y',0,-1);\n"
                + "	redis.call('DEL','X');\n"
                + "	redis.call('DEL','Y');\n"
                + "	return {xVal,yVal};\n"
                + "else\n"
                + "	return {};\n"
                + "end";
    
    @Override
    public void handleRequest(Point pt, String numdibujo) {
        Jedis jedis = JedisUtil.getPool().getResource();
        jedis.getClient().setTimeoutInfinite();

        jedis.watch("X", "Y");

        Transaction tx = jedis.multi();
        tx.rpush("X", String.valueOf(pt.getX()));
        tx.rpush("Y", String.valueOf(pt.getY()));

        System.out.println("Nuevo punto recibido en el servidor!:" + pt);

        Response<Object> luares = tx.eval(luaScript.getBytes(), 0, "0".getBytes());

        List<Object> resp = tx.exec();

        if (((ArrayList) luares.get()).size() == 2) {
            ArrayList<Object> xValues = (ArrayList) (((ArrayList) luares.get()).get(0));
            ArrayList<Object> yValues = (ArrayList) (((ArrayList) luares.get()).get(1));
            for (int i = 0; i < 4; i++) {
                Point pol = new Point(Integer.parseInt(new String((byte[]) xValues.get(i))), Integer.parseInt(new String((byte[]) yValues.get(i))));
                polygon.add(pol);
            }          
            msgt.convertAndSend("/topic/newpolygon." + numdibujo, polygon);
            polygon.clear();
        }
        jedis.close();
    
    }
    
}
