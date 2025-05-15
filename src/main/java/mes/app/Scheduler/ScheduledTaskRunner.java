package mes.app.Scheduler;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mes.app.Scheduler.SchedulerService.AccountSyncService;
import mes.app.Scheduler.SchedulerService.InvoiceSyncService;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
@Slf4j
@RequiredArgsConstructor
public class ScheduledTaskRunner {

    private final Executor schedulerExecutor;
    private final AccountSyncService accountSyncService;
    private final InvoiceSyncService invoiceSyncService;

    @Scheduled(cron = "0 */30 * * * *")
    public void runScheduledTasks() {
        schedulerExecutor.execute(() -> safeRun(accountSyncService::run, "계좌수집"));
        schedulerExecutor.execute(() -> safeRun(invoiceSyncService::run, "세금계산서"));

    }

    private void safeRun(Runnable task, String taskName){
        try{
            task.run();
        }catch (Exception e){
            log.error("스케줄러 작업 실패: {} - {}", taskName, e.getMessage(), e);
        }
    }
}
