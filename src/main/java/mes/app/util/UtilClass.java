package mes.app.util;

import java.util.Map;

public class UtilClass {

    public static Integer getInt(Map<String, Object> map, String key) {
        if (map == null || key == null) return null;

        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }

        return null;
    }

    public static Integer parseInteger(Object obj){
        if(obj == null) return null;
        if (obj instanceof Integer) return (Integer) obj;

        try{
            return Integer.parseInt(obj.toString());
        }catch (NumberFormatException e){
            return null;
        }
    }

}
