package edu.eci.arsw.collabpaint;

import edu.eci.arsw.collabpaint.model.Point;
import edu.eci.arsw.collabpaint.util.JedisUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Response;
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

        jedis.watch("X", "Y");
        Transaction tx = jedis.multi();
        tx.rpush("X", String.valueOf(pt.getX()));
        tx.rpush("Y", String.valueOf(pt.getY()));

        
        //System.out.println(jedis.lrange("Y", 0, -1));

        System.out.println("Nuevo punto recibido en el servidor!:" + pt);

        String luaScript = "local xVal,yVal; \n"
                + "if (redis.call('LLEN','X')>=4) then \n"
                + "	xVal=redis.call('LRANGE',KEYS[1],0,-1) 1 X;\n"
                + "	yVal=redis.call('LRANGE',KEYS[1],0,-1) 1 Y;\n"
                + "	redis.call('DEL','X');\n"
                + "	redis.call('DEL','Y');\n"
                + "	return {xVal,yVal};\n"
                + "else\n"
                + "	return {};\n"
                + "end";

        Response<Object> luares = tx.eval(luaScript.getBytes(), 0, "0".getBytes());

        List<Object> resp = tx.exec();

        if (((ArrayList) luares.get()).size() == 2) {
            System.out.println(new String((byte[]) ((ArrayList) (((ArrayList) luares.get()).get(0))).get(0)));
        }
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
