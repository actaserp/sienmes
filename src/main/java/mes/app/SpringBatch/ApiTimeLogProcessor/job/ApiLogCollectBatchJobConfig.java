package mes.app.SpringBatch.ApiTimeLogProcessor.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mes.app.Scheduler.LogProcessor.ApiLog.ApiExecutionLogEntry;
import mes.domain.services.SqlRunner;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ApiLogCollectBatchJobConfig {

    private final JobBuilderFactory jobBuilderFactory;

    private final StepBuilderFactory stepBuilderFactory;
    private final PlatformTransactionManager transactionManager;

    private final SqlRunner sqlRunner;

    private int CHUNK_SIZE = 10;

    @Bean
    public Job ApiTimeLogProcesserJob() {
        return jobBuilderFactory.get("ApiTimeLogProcesserJob")
                .start(readLogFileStep())
                .build();
    }

    @Bean
    public Step readLogFileStep(){
        return stepBuilderFactory.get("readLogFileStep")
                .<String, ApiExecutionLogEntry> chunk(CHUNK_SIZE)
                .reader(logFileItemReader())
                .processor(logFileItemProcessor())
                .writer(apiExecutionLogWriter())
                .transactionManager(transactionManager)
                .build();
    }

    //FlatFileItemReader : Spring Batch에서 파일을 한 줄씩 읽어서 지정된 타입 T로 반환해주는 ItemReader
    //파일의 각 줄(line)을 하나의 T 객체로 매핑해서 읽어주는 컴포넌트
    @Bean
    @StepScope
    public FlatFileItemReader<String> logFileItemReader() {

        LocalDateTime now = LocalDateTime.now().minusDays(1);
        String formatted = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String logPath = "logs/api-time/api_time." + formatted + ".log";


        return new FlatFileItemReaderBuilder<String>()
                .name("logFileItemReader")
                .resource(new FileSystemResource(logPath))
                .lineMapper((line, lineNumber) -> line)
                .saveState(true)
                .build();
    }

    @Bean
    public ItemProcessor<String, ApiExecutionLogEntry> logFileItemProcessor(){

        Pattern pattern = Pattern.compile(
                "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}) .*?\\[API 실행시간] (GET|POST|PUT|DELETE) (.*?) → ([\\d.]+)초"
        );

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

        return line -> {

            Matcher mathcer = pattern.matcher(line);

            if(mathcer.find()){
                ApiExecutionLogEntry entry = new ApiExecutionLogEntry();
                entry.setOccurrenceTime(LocalDateTime.parse(mathcer.group(1), formatter));
                entry.setMethod(mathcer.group(2));
                entry.setApiAddress(mathcer.group(3));
                entry.setDurationSecond(Double.parseDouble(mathcer.group(4)));
                return entry;
            }
            return null;
        };
    }

    @Bean
    public ItemWriter<ApiExecutionLogEntry> apiExecutionLogWriter(){


        String sql = """
                INSERT INTO api_log_summary (
                    summary_date, api_address, request_count, avg_duration, max_duration, min_duration
                ) VALUES (
                    :summaryDate, :apiAddress, :requestCount, :avgDuration, :maxDuration, :minDuration
                )
                """;

        return items -> {

            if(items.isEmpty()) return;

            Map<String, List<ApiExecutionLogEntry>> grouped = items.stream()
                    .collect(Collectors.groupingBy(ApiExecutionLogEntry::getApiAddress));

            List<SqlParameterSource> batchParams = new ArrayList<>();

            grouped.forEach((api, logs) -> {
                DoubleSummaryStatistics stats = logs.stream()
                        .collect(Collectors.summarizingDouble(ApiExecutionLogEntry::getDurationSecond));

                int count = (int) stats.getCount();
                double avg = stats.getAverage();
                double max = stats.getMax();
                double min = stats.getMin();

                /*MapSqlParameterSource param = new MapSqlParameterSource()
                        .addValue("OccurrenceTime", grouped.get(api))
                        .addValue("avg", )*/
            });

            for (ApiExecutionLogEntry item : items) {
                /*System.out.printf("✔ 로그 저장 예정: [%s] %s %s (%.3f초)%n",*/
                MapSqlParameterSource param = new MapSqlParameterSource()
                                .addValue("OccurrenceTime", item.getOccurrenceTime())
                                .addValue("method", item.getMethod())
                                .addValue("durationSecond", item.getDurationSecond())
                                .addValue("apiAddress", item.getApiAddress());
                batchParams.add(param);
            }
            log.info("[스프링 배치 쿼리 실행 - {}건]", batchParams.size());

            sqlRunner.batchUpdate(sql, batchParams.toArray(new SqlParameterSource[0]));
        };
    }
}
