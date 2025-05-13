package mes.Encryption.aop;

import lombok.extern.slf4j.Slf4j;
import mes.app.util.UtilClass;
import mes.domain.model.AjaxResult;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Aspect
@Component
@Slf4j
public class DecryptAspect {

    @Around("@annotation(decryptField)")
    public Object decryptResponse(ProceedingJoinPoint joinPoint, DecryptField decryptField) throws Throwable {
        Object result = joinPoint.proceed();

        if (result instanceof List<?>) {
            decryptList((List<?>) result, decryptField);

        } else if (result instanceof AjaxResult ajaxResult) {
            Object data = ajaxResult.data;

            // 1. data가 직접 List일 때
            if (data instanceof List<?>) {
                decryptList((List<?>) data, decryptField);
            }

            // 2. data가 Map이고, "list"라는 키를 포함할 때
            else if (data instanceof Map<?, ?> dataMap) {
                Object listObj = dataMap.get("list");

                if (listObj instanceof List<?>) {
                    decryptList((List<?>) listObj, decryptField);
                }
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private void decryptList(List<?> list, DecryptField decryptField) {
        if (!list.isEmpty() && list.get(0) instanceof Map<?, ?>) {
            try {
                UtilClass.decryptEachItem((List<Map<String, Object>>) list, decryptField.column(), decryptField.mask());
            } catch (Exception e) {
                log.error("AOP 복호화 실패 - column: {}, 이유: {}", decryptField.column(), e.getMessage());
            }
        }
    }
}
