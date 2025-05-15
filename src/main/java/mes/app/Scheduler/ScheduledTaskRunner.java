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

    @Scheduled(cron = "0 0 * * * *")
    //@Scheduled(cron = "0 */30 * * * *") //30분주기
    public void runScheduledTasks() {

        log.info("[스케줄러 시작] 계좌수집/세금계산서 작업 시작 - Thread: {}", Thread.currentThread().getName());

        //schedulerExecutor.execute(() -> safeRun(accountSyncService::run, "계좌수집"));
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
