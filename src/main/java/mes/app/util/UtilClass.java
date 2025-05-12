package mes.app.util;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.List;

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

    /**
     * 세션에서 사업장 코드를 통해 사업자 번호를 추출하는 메서드
     * ***/
    public static Map<String, Object> getSpjanInfoFromSession(String spjangcd, HttpSession httpSession){
        List<Map<String, Object>> spjangList = (List<Map<String, Object>>) httpSession.getAttribute("spjangList");

        if(spjangList == null) return null;

        for(Map<String, Object> item : spjangList){
            if(spjangcd.equals(item.get("spjancd"))){
                return item;
            }
        }
        return null;
    }
    /***
     * 세션에서 사업자번호 추출하는 메서드
     * **/
    public static String getsaupnumInfoFromSession(String spjangcd, HttpSession httpSession){
        List<Map<String, Object>> spjangList = (List<Map<String, Object>>) httpSession.getAttribute("spjangList");

        if(spjangList == null) return null;

        for(Map<String, Object> item : spjangList){
            if(spjangcd.equals(item.get("spjangcd"))){
                return String.valueOf(item.get("saupnum"));
            }
        }
        return null;
    }

    /**
     * 객체를 안전하게 문자열로 변환한다.
     * - null일 경우 빈 문자열("") 반환
     * - null이 아니면 toString() 결과 반환
     */
    public static String getStringSafe(Object obj) {
        return obj == null ? "" : obj.toString().trim();
    }

}
