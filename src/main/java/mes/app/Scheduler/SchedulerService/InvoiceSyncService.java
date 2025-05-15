package mes.app.Scheduler.SchedulerService;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InvoiceSyncService {


    public void run() {
        for(int i=0; i<100; i++){
            System.out.println("[" + Thread.currentThread().getName() + "] 세금계산서 서비스 실행중...");

        }
    }
}
