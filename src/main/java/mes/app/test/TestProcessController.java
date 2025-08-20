package mes.app.test;

import mes.app.test.service.TestMethodService;
import mes.app.test.service.TestProcessService;
import mes.domain.dto.ProcessTestDto;
import mes.domain.entity.ProcessTest;
import mes.domain.entity.TestMethod;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import mes.domain.repository.JobResRepository;
import mes.domain.repository.ProcessTestRepository;
import mes.domain.repository.TestMethodRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/test/test_process")
public class TestProcessController {
    @Autowired
    TestProcessService testMethodService;

    @Autowired
    TestMethodRepository testMethodRepository;

    @Autowired
    JobResRepository jobResRepository;

    @Autowired
    ProcessTestRepository processTestRepository;

    @GetMapping("/read")
    public AjaxResult read(@RequestParam(value = "date_from", required = false) String dateFrom,
                           @RequestParam(value = "date_to", required = false) String dateTo,
                           @RequestParam(value = "shift_code", required = false) String shiftCode,
                           @RequestParam(value = "workcenter_pk", required = false) String workcenterPk,
                           @RequestParam(value = "mat_type", required = false) String mat_type,
                           @RequestParam(value = "is_include_comp", required = false) String isIncludeComp,
                           @RequestParam("spjangcd") String spjangcd) {
        AjaxResult result = new AjaxResult();
        result.data = this.testMethodService.getProdResult(dateFrom, dateTo, shiftCode, workcenterPk, mat_type, isIncludeComp, spjangcd);
        return result;
    }

    @GetMapping("/readProcess")
    public AjaxResult detail(@RequestParam(value="id", required=false) int id){
        AjaxResult result = new AjaxResult();

        // 1. process_test 조회 (job_res_id 기준)
        Optional<ProcessTest> processData = processTestRepository.findByJobResId(id);

        if (processData.isPresent()) {
            // 조회결과가 있으면 process_test 데이터 사용
            result.data = this.testMethodService.getTestMethodDetail(id);
        } else {
            // 없으면 job_res 데이터로 fallback
            result.data = this.testMethodService.findJobResData(id);
        }

        return result;
    }

    @PostMapping("/save")
    public AjaxResult save(ProcessTestDto dto, Authentication auth) {
        AjaxResult result = new AjaxResult();
        try {
            // DTO → Entity 변환
            ProcessTest entity = new ProcessTest();
            entity.setProcessTestId(dto.getProcess_test_id());
            entity.setJobResId(dto.getJob_res_id());
            entity.setMatIncoming(dto.getMat_incoming());
            entity.setMatWeighing(dto.getMat_weighing());
            entity.setTankCleaning(dto.getTank_cleaning());
            entity.setInputMixing(dto.getInput_mixing());
            entity.setHeatSterilization(dto.getHeat_sterilization());
            entity.setFiltration(dto.getFiltration());
            entity.setQualityCheck(dto.getQuality_check());
            entity.setMetalDetect(dto.getMetal_detect());
            entity.setPackaging(dto.getPackaging());
            entity.setProductInspect(dto.getProduct_inspect());
            entity.setStorageShipping(dto.getStorage_shipping());
            entity.setRemark(dto.getRemark());
            entity.setQcFirstBrix(dto.getQc_first_brix());
            entity.setQcFirstSalt(dto.getQc_first_salt());
            entity.setQcSecondBrix(dto.getQc_second_brix());
            entity.setQcSecondSalt(dto.getQc_second_salt());
            entity.setQcFinalBrix(dto.getQc_final_brix());
            entity.setQcFinalSalt(dto.getQc_final_salt());
            entity.setMatName(dto.getMat_name());
            entity.setWorkcenterName(dto.getWorkcenter_name());
            entity.setFoodType(dto.getFood_type());
            String productionDate = (dto.getProduction_date()).replaceAll("-","");
            entity.setProductionDate(productionDate);
            entity.setValidate(dto.getValidate());
            entity.setStorageMethod(dto.getStorage_method());
            entity.setPackagingSpec(dto.getPackaging_spec());
            entity.setMixingAmount(dto.getMixing_amount());
            entity.setPackagingMat(dto.getPackaging_mat());
            entity.setLodcell(dto.getLodcell());

            result.data = processTestRepository.save(entity);
            result.success = true;
        } catch (Exception e) {
            result.success = false;
            result.message = e.getMessage();
        }
        return result;
    }




    @PostMapping("/delete")
    public AjaxResult deleteTestMethod(@RequestParam(value="id", required=true) int id) {
        AjaxResult result = new AjaxResult();
        this.processTestRepository.deleteById(id);
        return result;
    }
    // 엑셀파일 조회 및 파일 보기 메서드
    @GetMapping("/readExcelFile")
    public void readExcelFile(@RequestParam(value = "process_test_id", required = false) Integer processTestId,
                             HttpServletResponse response,
                             Authentication auth) throws Exception {
        try {
            User user = (User) auth.getPrincipal();
            String username = user.getUsername();
            String spjangcd = "ZZ";

            Optional<ProcessTest> excelData = processTestRepository.findById(processTestId);
            if (!excelData.isPresent()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"데이터가 없습니다\"}");
                response.flushBuffer();
                return;
            }

            // 1. UUID 기반 임시 파일명 생성
            String uuid = UUID.randomUUID().toString();
            Path tempXlsx = Files.createTempFile(uuid, ".xlsx");
            Path tempPdf = Path.of(tempXlsx.toString().replace(".xlsx", ".pdf"));

            try (FileInputStream fis = new FileInputStream("C:/Temp/mes21/문서/ProcessTest.xlsx");
                 Workbook workbook = new XSSFWorkbook(fis);
                 FileOutputStream fos = new FileOutputStream(tempXlsx.toFile())) {

                Sheet sheet = workbook.getSheetAt(0);
                // 공정검사 정보 setCell
                ProcessTest data = excelData.get();

                // 제품 정보 영역
                setCell(sheet, 2, 0, "제품명 : " + (data.getMatName() != null ? data.getMatName() : "")); // A3
                setCell(sheet, 3, 2, data.getWorkcenterName());  // C4
                setCell(sheet, 3, 5, data.getFoodType());        // F4
                setCell(sheet, 3, 9, formatDate(data.getProductionDate())); // J4 (yyyy-MM-dd)
                setCell(sheet, 3, 13, "제조일로부터 " + data.getValidate() + "일"); // N4
                setCell(sheet, 4, 2, data.getStorageMethod());   // C5
                setCell(sheet, 4, 5, data.getPackagingSpec());   // F5
                setCell(sheet, 4, 9, data.getMixingAmount()); // J5
                setCell(sheet, 4, 13, data.getPackagingMat());   // N5

                // 공정 검사 체크리스트 (값이 1이면 √)
                setCell(sheet, 8, 15, checkMark(data.getMatIncoming()));      // P9
                setCell(sheet, 9, 15, checkMark(data.getMatWeighing()));      // P10
                setCell(sheet, 10, 15, checkMark(data.getTankCleaning()));    // P11
                setCell(sheet, 11, 15, checkMark(data.getInputMixing()));     // P12
                setCell(sheet, 12, 15, checkMark(data.getHeatSterilization()));// P13
                setCell(sheet, 13, 15, checkMark(data.getFiltration()));      // P14
                setCell(sheet, 14, 15, checkMark(data.getQualityCheck()));    // P15
                setCell(sheet, 20, 15, checkMark(data.getMetalDetect()));     // P21
                setCell(sheet, 22, 15, checkMark(data.getPackaging()));       // P23

                // 로드셀
                setCell(sheet, 11, 8, "총 로드셀 : " + data.getLodcell() + " kg"); // I12

                // QC 항목
                setCell(sheet, 17, 9, data.getQcFirstBrix());   // J18
                setCell(sheet, 17, 10, data.getQcSecondBrix()); // K18
                setCell(sheet, 17, 12, data.getQcFinalBrix());  // M18
                setCell(sheet, 18, 9, data.getQcFirstSalt());   // J19
                setCell(sheet, 18, 10, data.getQcSecondSalt()); // K19
                setCell(sheet, 18, 12, data.getQcFinalSalt());  // M19

                // 검사 판정
                String inspectResult = "검사판정 :   적합 "
                        + ( "1".equals(data.getProductInspect()) ? "√" : "" )
                        + "   부적합 "
                        + ( !"1".equals(data.getProductInspect()) ? "√" : "" );
                setCell(sheet, 23, 8, inspectResult); // I24

                // 특이사항
                setCell(sheet, 25, 0, "특이사항 : " + (data.getRemark() != null ? data.getRemark() : "")); // A26


                workbook.write(fos);
            }

            // 3. LibreOffice로 PDF 변환
            ProcessBuilder pb = new ProcessBuilder(
                    "C:/Program Files/LibreOffice/program/soffice.exe",
                    "--headless",
                    "--convert-to", "pdf",
                    "--outdir", tempPdf.getParent().toString(),
                    tempXlsx.toAbsolutePath().toString()
            );
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("LibreOffice 변환 실패 (exitCode=" + exitCode + ")");
            }
            if (!Files.exists(tempPdf) || Files.size(tempPdf) == 0) {
                throw new RuntimeException("PDF 파일 생성 실패: " + tempPdf);
            }

            // --- PDF 응답 전송 (단일 파일) ---
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "inline; filename=file.pdf");
            Files.copy(tempPdf, response.getOutputStream());
            response.flushBuffer();

            // --- 임시 파일 삭제 예약 ---
            Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                try { Files.deleteIfExists(tempXlsx); } catch (Exception ignore) {}
                try { Files.deleteIfExists(tempPdf);  } catch (Exception ignore) {}
            }, 5, TimeUnit.MINUTES);

        } catch (Exception e) {
            // 예외 발생 시 명확한 메시지 반환
            System.out.println(">>> 예외 발생: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"서버 에러: " + e.getMessage() + "\"}");
            response.flushBuffer();
        }
    }
    private void setCell(Sheet sheet, int rowIdx, int colIdx, String value) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) row = sheet.createRow(rowIdx);
        Cell cell = row.getCell(colIdx);
        if (cell == null) cell = row.createCell(colIdx);
        cell.setCellValue(value);
    }
    // 날짜 포맷 변환 (YYYYMMDD → YYYY-MM-DD)
    private String formatDate(String yyyymmdd) {
        if (yyyymmdd == null || yyyymmdd.length() != 8) return "";
        return yyyymmdd.substring(0,4) + "-" + yyyymmdd.substring(4,6) + "-" + yyyymmdd.substring(6,8);
    }

    // 값이 1일 경우 "√", 그 외는 ""
    private String checkMark(String value) {
        if (value == null) return "";
        return "1".equals(value) ? "√" : "";
    }
}
