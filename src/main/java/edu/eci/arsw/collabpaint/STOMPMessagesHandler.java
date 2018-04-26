package edu.eci.arsw.collabpaint;

import edu.eci.arsw.collabpaint.model.Point;
import edu.eci.arsw.collabpaint.util.JedisUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
    
    private CopyOnWriteArrayList<Point> polygon = new CopyOnWriteArrayList<>();

    @MessageMapping("/newpoint.{numdibujo}")
    public void handlePointEvent(Point pt, @DestinationVariable String numdibujo) throws Exception {
        Jedis jedis = JedisUtil.getPool().getResource();
        jedis.getClient().setTimeoutInfinite();

        jedis.watch("X", "Y");

        Transaction tx = jedis.multi();
        tx.rpush("X", String.valueOf(pt.getX()));
        tx.rpush("Y", String.valueOf(pt.getY()));

        System.out.println("Nuevo punto recibido en el servidor!:" + pt);

        String luaScript = "local xVal,yVal; \n"
                + "if (redis.call('LLEN','X')==4) then \n"
                + "	xVal=redis.call('LRANGE','X',0,-1);\n"
                + "	yVal=redis.call('LRANGE','Y',0,-1);\n"
                + "	redis.call('DEL','X');\n"
                + "	redis.call('DEL','Y');\n"
                + "	return {xVal,yVal};\n"
                + "else\n"
                + "	return {};\n"
                + "end";

        Response<Object> luares = tx.eval(luaScript.getBytes(), 0, "0".getBytes());

        List<Object> resp = tx.exec();

        if (resp.size() == 2) {
            ArrayList<Object> coordX = (ArrayList) (((ArrayList) luares.get()).get(0));
            ArrayList<Object> coordY = (ArrayList) (((ArrayList) luares.get()).get(1));
            for (int i = 0; i < 4; i++) {
                Point pol = new Point(Integer.parseInt(new String((byte[]) coordX.get(i))), Integer.parseInt(new String((byte[]) coordY.get(i))));
                polygon.add(pol);
            }          
            msgt.convertAndSend("/topic/newpolygon." + numdibujo, polygon);
            polygon.clear();
        }
        jedis.close();
    }
}
