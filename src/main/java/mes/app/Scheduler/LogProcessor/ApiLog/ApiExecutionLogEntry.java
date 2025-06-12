package mes.app.Scheduler.LogProcessor.ApiLog;

import lombok.Getter;
import lombok.Setter;
import mes.app.Scheduler.LogProcessor.LogEntry;

@Getter
@Setter
public class ApiExecutionLogEntry implements LogEntry {

    private String occurrenceTime;
    private String method;
    private String apiAddress;
    private Double durationSecond;

    @Override
    public String getType() {
        return "API_EXEC_TIME";
    }

}
