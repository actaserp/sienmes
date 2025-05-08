package mes.app.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

    public static boolean isValidDate(String yyymmdd){
        try{
            LocalDate.parse(yyymmdd, DateTimeFormatter.ofPattern("yyyyMMdd"));

            return true;
        }catch(DateTimeParseException e){
            return false;
        }
    }

    /**
     * @Return : yyyyMMddHHmmss
     */
    public static String combineDateAndHourReturnyyyyMMddHHmmss(String date, String time){
        try{

            if(time == null || time.isBlank()){
                time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            }

            String combined = date + " " + time;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime dateTime = LocalDateTime.parse(combined, formatter);

            DateTimeFormatter output = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            return dateTime.format(output);

        }catch (DateTimeParseException e){
            throw new IllegalArgumentException("날짜/시간 형식이 올바르지 않습니다.");
        }
    }



}
