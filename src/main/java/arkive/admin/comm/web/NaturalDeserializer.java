package arkive.admin.comm.web;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
/**
 * 
 * @Class Name : NaturalDeserializer.java
 * @Description : JSON String을 Map Object 또는 Map[] 으로 만들어줌
 * @author hg00075
 * @since 2011. 9. 8.
 * @version 1.0
 * @see  http://stackoverflow.com/questions/2779251/convert-json-to-hashmap-using-gson-in-java 
 *
 * @Modification Information
 * <pre>
 *    수정일         수정자              수정내용
 *  ===========    =========    ===========================
 *  2011. 9. 8.      hg00075      최초 생성
 * </pre>
 */
public class NaturalDeserializer implements JsonDeserializer<Object> {
	
	protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    public Object deserialize(JsonElement json, Type typeOfT,
            JsonDeserializationContext context) {
        if (json.isJsonNull())
            return null;
        else if (json.isJsonPrimitive())
            return handlePrimitive(json.getAsJsonPrimitive());
        else if (json.isJsonArray())
            return handleArray(json.getAsJsonArray(), context);
        else
            return handleObject(json.getAsJsonObject(), context);
    }

    private Object handlePrimitive(JsonPrimitive json) {
        if (json.isBoolean())
            return json.getAsBoolean();
        else if (json.isString())
            return json.getAsString();
        else {
            BigDecimal bigDec = json.getAsBigDecimal();
            // Find out if it is an int type
            try {
                bigDec.toBigIntegerExact();
                try {
                    return bigDec.intValueExact();
                } catch (ArithmeticException e) {
                	logger.error("에러");
                }
                return bigDec.longValue();
            } catch (ArithmeticException e) {
            	logger.error("에러");
            }
            // Just return it as a double
            return bigDec.doubleValue();
        }
    }

    private Object handleArray(JsonArray json, JsonDeserializationContext context) {
        Object[] array = new Object[json.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = context.deserialize(json.get(i), Object.class);
        }
        return array;
    }

    private Map handleObject(JsonObject json, JsonDeserializationContext context) {
        Map<String, Object> map = new HashMap<String, Object>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            map.put(entry.getKey(),
                    context.deserialize(entry.getValue(), Map.class));
        }
        return map;
    }
}
