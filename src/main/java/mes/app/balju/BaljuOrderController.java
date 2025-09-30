package mes.app.balju;

import lombok.extern.slf4j.Slf4j;
import mes.app.MailService;
import mes.app.balju.service.BaljuOrderService;
import mes.config.Settings;
import mes.domain.entity.Balju;
import mes.domain.entity.BaljuHead;
import mes.domain.entity.Company;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import mes.domain.repository.BalJuHeadRepository;
import mes.domain.repository.BujuRepository;
import mes.domain.repository.MaterialRepository;
import mes.domain.services.CommonUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/balju/balju_order")
public class BaljuOrderController {

  @Autowired
  BaljuOrderService baljuOrderService;

  @Autowired
  BujuRepository bujuRepository;

  @Autowired
  BalJuHeadRepository balJuHeadRepository;

  @Autowired
  MailService mailService;

  @Autowired
  MaterialRepository materialRepository;

  @Autowired
  Settings settings;

  // 발주 목록 조회
  @GetMapping("/read")
  public AjaxResult getSujuList(
      @RequestParam(value = "date_kind", required = false) String date_kind,
      @RequestParam(value = "start", required = false) String start_date,
      @RequestParam(value = "end", required = false) String end_date,
      @RequestParam(value = "spjangcd") String spjangcd,
      HttpServletRequest request) {
    //log.info("발주 read--- date_kind:{}, start_date:{},end_date:{} , spjangcd:{} " ,date_kind,start_date , end_date, spjangcd);
    start_date = start_date + " 00:00:00";
    end_date = end_date + " 23:59:59";

    Timestamp start = Timestamp.valueOf(start_date);
    Timestamp end = Timestamp.valueOf(end_date);

    List<Map<String, Object>> items = this.baljuOrderService.getBaljuList(date_kind, start, end, spjangcd);

    AjaxResult result = new AjaxResult();
    result.data = items;

    return result;
  }

  // 발주 등록
  @PostMapping("/multi_save")
  @Transactional
  public AjaxResult saveBaljuMulti(@RequestBody Map<String, Object> payload, Authentication auth) {
//    log.info("발주등록 들어옴");
//    log.info("📦 payload keys: {}", payload.keySet());  // items가 포함되어야 함
//    log.info("🧾 items 내용: {}", payload.get("items"));
    User user = (User) auth.getPrincipal();

    // 기본 정보 추출
    String jumunDateStr = (String) payload.get("JumunDate");
    String dueDateStr = (String) payload.get("DueDate");
    Integer companyId = Integer.parseInt(payload.get("Company_id").toString());
    String CompanyName = (String) payload.get("CompanyName");
    String spjangcd = (String) payload.get("spjangcd");
    String isVat = (String) payload.get("invatyn");
    String specialNote = (String) payload.get("special_note");
    String sujuType = (String) payload.get("cboBaljuType");

    Date jumunDate = CommonUtil.trySqlDate(jumunDateStr);
    Date dueDate = CommonUtil.trySqlDate(dueDateStr);

    Integer headId = CommonUtil.tryIntNull(payload.get("bh_id")); // 발주 헤더 ID
//    log.info("Balju Info => JumunDate: {}, DueDate: {}, CompanyId: {}, CompanyName: {}, Spjangcd: {}, InVatYN: {}, SpecialNote: {}, SujuType: {}" ,
//        jumunDateStr, dueDateStr, companyId, CompanyName, spjangcd, isVat, specialNote, sujuType);

    BaljuHead head;

    if (headId != null) {
//      log.info("🔄 기존 발주 수정 - headId: {}", headId);
      head = balJuHeadRepository.findById(headId)
          .orElseThrow(() -> new RuntimeException("발주 헤더 없음"));
      head.setModified(new Timestamp(System.currentTimeMillis()));
      head.setModifierId(user.getId());
      head.setDeliveryDate(dueDate);
      head.setSujuType(sujuType);
    } else {
//      log.info("신규 발주 생성");
      head = new BaljuHead();
      head.setCreated(new Timestamp(System.currentTimeMillis()));
      head.setCreaterId(user.getId());
      head.set_status("manual");
      String jumunNumber = baljuOrderService.makeJumunNumber(jumunDate);
      head.setJumunNumber(jumunNumber);
      head.setSujuType(sujuType);
      head.setDeliveryDate(dueDate);
    }

    // 공통 필드 설정
    head.setSujuType(sujuType);
    head.setJumunDate(jumunDate);
    head.setCompanyId(companyId);
    head.setSpjangcd(spjangcd);
    head.setSpecialNote(specialNote);

    balJuHeadRepository.save(head);
    //log.info("✅ BaljuHead 저장 완료 - ID: {}", head.getId());

    // 하위 품목 저장
    List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
    double totalPriceSum = 0;

    if (headId != null) {
      Set<Integer> incomingIds = items.stream()
          .map(i -> CommonUtil.tryIntNull(i.get("baljuId")))
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());

      List<Balju> existingDetails = bujuRepository.findByBaljuHeadId(headId);
      for (Balju detail : existingDetails) {
        if (!incomingIds.contains(detail.getId())) {
//          log.info("🗑️ 삭제 대상 발주 상세 ID: {}", detail.getId());
          bujuRepository.delete(detail);
        }
      }
    }

    for (Map<String, Object> item : items) {
      Integer baljuId = CommonUtil.tryIntNull(item.get("baljuId"));

      Integer materialId = Integer.parseInt(item.get("Material_id").toString());
      Double qty = Double.parseDouble(item.get("quantity").toString());
      Double unitPrice = Double.parseDouble(item.get("unit_price").toString());
      Double supply_price = Double.parseDouble(item.get("supply_price").toString());
      Double vat = Double.parseDouble(item.get("vat").toString());

      Balju detail;

      if (baljuId != null) {
        detail = bujuRepository.findById(baljuId)
            .orElseThrow(() -> new RuntimeException("상세 항목 없음"));
        detail._modified = new Timestamp(System.currentTimeMillis());
        detail._modifier_id = user.getId();
      } else {
        detail = new Balju();
        detail._created = new Timestamp(System.currentTimeMillis());
        detail._creater_id = user.getId();
        detail.setBaljuHeadId(head.getId());
        detail.setJumunNumber(head.getJumunNumber());
        detail.setDueDate(dueDate);
      }
      String editedFlag = String.valueOf(item.get("totalEdited")).toUpperCase();
      boolean isManual = "TRUE".equals(editedFlag) || "Y".equals(editedFlag);

      if (isManual) {
        detail.setTotalAmount(Double.parseDouble(item.get("total_price").toString()));
        //log.info("✅ 수기입력 적용: total_price = {}", item.get("total_price"));
      } else {
        detail.setTotalAmount(supply_price + vat);
        //log.info("⚙️ 자동계산 적용: supply + vat = {}", supply_price + vat);
      }


      detail.setMaterialId(materialId);
      detail.setCompanyId(companyId);
      detail.setCompanyName(CompanyName);
      detail.setSujuQty(qty);
      detail.setUnitPrice(unitPrice);
      detail.setPrice(supply_price);
      detail.setVat(vat);
      detail.setDescription(CommonUtil.tryString(item.get("description")));
      detail.setSpjangcd(spjangcd);
      detail.setJumunDate(jumunDate);
      detail.setDueDate(dueDate);
      detail.setInVatYN("Y".equalsIgnoreCase(isVat) ? "Y" : "N");
      detail.setSujuType(sujuType);
      detail.setState("draft");
      detail.setSujuQty2(0.0d);
      detail.set_status("manual");

      totalPriceSum += detail.getTotalAmount();
      bujuRepository.save(detail);
    }

    head.setTotalPrice(totalPriceSum);
    balJuHeadRepository.save(head);

    AjaxResult result = new AjaxResult();
    result.data = Map.of("headId", head.getId(), "totalPrice", totalPriceSum);
    return result;
  }

  // 발주 상세정보 조회
  @GetMapping("/detail")
  public AjaxResult getBaljuDetail(
      @RequestParam("id") int id,
      HttpServletRequest request) {
//    log.info("상세 정보 들어옴 : id:{}", id);
    Map<String, Object> item = this.baljuOrderService.getBaljuDetail(id);

    AjaxResult result = new AjaxResult();
    result.data = item;

    return result;
  }

  // 발주 삭제
  @PostMapping("/delete")
  @Transactional
  public AjaxResult deleteSuju(
      @RequestParam("id") Integer id,
      @RequestParam("State") String State) {
    AjaxResult result = new AjaxResult();

    String state = (State == null) ? "" : State.trim();
    boolean isDraft =
        "draft".equalsIgnoreCase(state) || "미입고".equals(state);

    if (!isDraft) {
      result.success = false;
      result.message = "미입고 상태일 때만 삭제할 수 있습니다.";
      return result;
    }
    Optional<BaljuHead> optionalHead = balJuHeadRepository.findById(id);
    if (!optionalHead.isPresent()) {
      result.success = false;
      result.message = "해당 발주 정보가 존재하지 않습니다.";
      return result;
    }

    BaljuHead head = optionalHead.get();

    // 1. 기준 정보 추출
    String jumunNumber = head.getJumunNumber();
    Date jumunDate = head.getJumunDate();
    String spjangcd = head.getSpjangcd();

    // 2. 해당 기준으로 balju 삭제
    bujuRepository.deleteByJumunNumberAndJumunDateAndSpjangcd(jumunNumber, jumunDate, spjangcd);

    // 3. balju_head 삭제
    balJuHeadRepository.deleteById(id);

    result.success = true;
    return result;
  }


  //중지 처리
  @PostMapping("/balju_stop")
  public AjaxResult balju_stop(@RequestParam(value = "id", required = false) Integer id) {

    List<Map<String, Object>> items = this.baljuOrderService.balju_stop(id);
    AjaxResult result = new AjaxResult();
    result.data = items;
    return result;
  }

  //단가 찾기
  @GetMapping("/price")
  public AjaxResult BaljuPrice(@RequestParam("mat_pk") int materialId,
                               @RequestParam("JumunDate") String jumunDate,
                               @RequestParam("company_id") int companyId) {
    //log.info("발주단가 찾기 --- matPk:{}, ApplyStartDate:{},company_id:{} ",materialId,jumunDate , companyId);
    List<Map<String, Object>> items = this.baljuOrderService.getBaljuPrice(materialId, jumunDate, companyId);
    AjaxResult result = new AjaxResult();
    result.data = items;
    return result;
  }

  @PostMapping("/savePrice")
  public AjaxResult savePriceByMat(@RequestBody Map<String, Object> data) {
    AjaxResult result = new AjaxResult();

    try {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      User user = (User) auth.getPrincipal();
      data.put("user_id", user.getId());

      int saveCount = this.baljuOrderService.saveCompanyUnitPrice(data);

      if (saveCount > 0) {
        result.success = true;
      } else {
        result.success = false;
        result.message = "저장 실패: 중복된 데이터이거나 입력값이 올바르지 않습니다.";
      }
    } catch (Exception e) {
      result.success = false;
      result.message = "서버 오류: " + e.getMessage();
    }

    return result;
  }

  @GetMapping("/receiverEmail")
  public AjaxResult getReceiverEmail(@RequestParam("bhId") Integer bhId) {
    String email = this.baljuOrderService.getReceiverEmail(bhId);

    AjaxResult result = new AjaxResult();
    result.data = email;
    return result;
  }

  //엑셀 만들기 + 메일 전송
  @PostMapping("/sendBalJuMail")
  public AjaxResult getMailData(@RequestBody Map<String, Object> payload, Authentication auth) {
    AjaxResult result = new AjaxResult();

    try {
      List<String> recipients = (List<String>) payload.get("recipients");
      String title = (String) payload.get("title");
      String content = (String) payload.get("content");
      Integer bhId = (Integer) payload.get("bhId");
      // 1. 로그인 사용자 정보 추출
      User user = (User) auth.getPrincipal();
      String userid = user.getUsername();

      // 2. 발주서 데이터 및 발신자 정보 조회
      Map<String, Object> baljuData = baljuOrderService.getBaljuDetail(bhId);
      Map<String, Object> senderInfo = baljuOrderService.getSenderInfo(userid);

      Integer companyId = (Integer) baljuData.get("Company_id");
      Map<String, Object> receiverInfo = baljuOrderService.getReceiverInfo(companyId);

      // 3. 파일명 구성: "20250701-0011_동영전자_발주서.xlsx"
      String jumunNumber = (String) baljuData.get("JumunNumber"); // 주문번호
      String companyName = (String) baljuData.get("CompanyName"); // 구매처명
      String safeCompanyName = companyName.replaceAll("[\\\\/:*?\"<>|]", ""); // 파일명에 쓸 수 없는 문자 제거

      String fileName = String.format("%s_%s_발주서.xlsx", jumunNumber, safeCompanyName);

      // 4. 엑셀 템플릿 기반 파일 생성
      // 새 경로: C:/Temp/mes21/{파일명}에 직접 저장
      Path tempXlsx = Paths.get("C:/Temp/mes21/" + fileName);
      Files.createDirectories(tempXlsx.getParent()); // 상위 디렉터리 없으면 생성
      Files.deleteIfExists(tempXlsx);               // 중복 방지
      Files.createFile(tempXlsx);                   // 새 파일 생성


      try (FileInputStream fis = new FileInputStream("C:/Temp/mes21/문서/BaljuTemplate.xlsx");
           Workbook workbook = new XSSFWorkbook(fis);
           FileOutputStream fos = new FileOutputStream(tempXlsx.toFile())) {

        // 시트 열기 및 이름 변경
        Sheet sheet = workbook.getSheetAt(0);
        workbook.setSheetName(workbook.getSheetIndex(sheet), "발주서");

        // 데이터 채우기
        Map<String, Object> header = baljuData;
        List<Map<String, Object>> items = (List<Map<String, Object>>) header.get("items");
        // 수신자 (TO.)
        safeAddMergedRegion(sheet, 2, 2, 1, 2);  // B3:C3
        setCell(sheet, 2, 1, (String) receiverInfo.get("company_name"));
        safeAddMergedRegion(sheet, 4, 4, 1, 3);  // B5:D5
        setCell(sheet, 4, 1, (String) receiverInfo.get("tel"));
        safeAddMergedRegion(sheet, 5, 6, 1, 3);  // B6:D7
        setCell(sheet, 5, 1, (String) receiverInfo.get("address"));


        // 발신자 (FROM.)
        safeAddMergedRegion(sheet, 2, 2, 5, 6);  // F3:G3
        setCell(sheet, 2, 5, (String) senderInfo.get("spjangnm"));
        safeAddMergedRegion(sheet, 4, 4, 5, 6);  // F5:G5
        setCell(sheet, 4, 5, (String) senderInfo.get("tel1"));
        safeAddMergedRegion(sheet, 5, 6, 5, 7);  // F6:H7
        setCell(sheet, 5, 5, (String) senderInfo.get("adresa"));

        // 날짜 출력
        String rawDate = String.valueOf(baljuData.get("JumunDate"));
        LocalDate date = LocalDate.parse(rawDate);
        String formattedDate = date.format(DateTimeFormatter.ofPattern("yy.MM.dd"));
        setCell(sheet, 11, 3, formattedDate);  // D12 셀에 날짜만 넣기

        // 자재 행 삽입
        int startRow = 14;
        Row styleTemplateRow = sheet.getRow(startRow); // 14행 스타일 참조

        CellStyle[] cachedStyles = new CellStyle[7];         // 일반 행용 스타일
        CellStyle[] cachedLastRowStyles = new CellStyle[7];  // 마지막 행용 스타일

        for (int i = 0; i < items.size(); i++) {
          Map<String, Object> item = items.get(i);
          int currentRowIndex = startRow + i;

          Row row = sheet.getRow(currentRowIndex);
          if (row == null) row = sheet.createRow(currentRowIndex);

          for (int col = 1; col <= 6; col++) {
            Cell cell = row.getCell(col);
            if (cell == null) cell = row.createCell(col);

            if (styleTemplateRow != null && styleTemplateRow.getCell(col) != null) {
              CellStyle baseStyle = styleTemplateRow.getCell(col).getCellStyle();

              if (i == items.size() - 1 && col == 2) {
                if (cachedLastRowStyles[col] == null) {
                  CellStyle style = workbook.createCellStyle();
                  style.cloneStyleFrom(baseStyle);
                  style.setBorderBottom(BorderStyle.THICK); // 굵은 아래 테두리
                  style.setAlignment(HorizontalAlignment.CENTER); // 가운데 정렬
                  style.setVerticalAlignment(VerticalAlignment.CENTER);
                  cachedLastRowStyles[col] = style;
                }
                cell.setCellStyle(cachedLastRowStyles[col]);
              } else {
                // 일반 행: 기본 스타일
                if (cachedStyles[col] == null) {
                  CellStyle normalStyle = workbook.createCellStyle();
                  normalStyle.cloneStyleFrom(baseStyle);
                  cachedStyles[col] = normalStyle;
                }
                cell.setCellStyle(cachedStyles[col]);
              }
            }
          }

          // ✅ 병합: C열(2) ~ D열(3), 중복 방지 로직 적용
          CellRangeAddress mergedRegion = new CellRangeAddress(currentRowIndex, currentRowIndex, 2, 3);
          boolean alreadyMerged = false;
          for (int j = 0; j < sheet.getNumMergedRegions(); j++) {
            if (sheet.getMergedRegion(j).equals(mergedRegion)) {
              alreadyMerged = true;
              break;
            }
          }
          if (!alreadyMerged) {
            sheet.addMergedRegion(mergedRegion);
          }

          // 가운데 정렬 스타일 (자재명 셀에만)
          CellStyle centerStyle = workbook.createCellStyle();
          centerStyle.cloneStyleFrom(styleTemplateRow.getCell(2).getCellStyle());
          centerStyle.setAlignment(HorizontalAlignment.CENTER);
          centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
          row.getCell(2).setCellStyle(centerStyle);

          // 값 설정
          row.getCell(1).setCellValue(i + 1); // NO
          row.getCell(2).setCellValue((String) item.get("product_name")); // 자재명
          row.getCell(4).setCellValue(((Number) item.get("quantity")).doubleValue()); // 수량
          row.getCell(5).setCellValue(((Number) item.get("unit_price")).doubleValue()); // 단가
          row.getCell(6).setCellValue((String) item.get("description")); // 비고
        }

        // 특이사항 처리 시작
        // 1. 특이사항 행 위치 계산
        int lastItemRow = startRow + items.size();
        int baseSpecialNoteRow = 22;
        int specialNoteStartRow = Math.max(lastItemRow + 2, baseSpecialNoteRow);

        // 2. 병합 범위 계산 (B~G 열, 3행 병합)
        CellRangeAddress specialNoteRegion = new CellRangeAddress(
            specialNoteStartRow,
            specialNoteStartRow + 2,
            1,
            6
        );

        // 3. 기존 병합과 충돌하는 것 제거
        for (int i = sheet.getNumMergedRegions() - 1; i >= 0; i--) {
          if (sheet.getMergedRegion(i).intersects(specialNoteRegion)) {
            sheet.removeMergedRegion(i);
          }
        }

        // 4. 병합 적용
        sheet.addMergedRegion(specialNoteRegion);

        // 5. 셀 스타일 정의
        CellStyle borderStyle = workbook.createCellStyle();
        borderStyle.setWrapText(true);
        borderStyle.setVerticalAlignment(VerticalAlignment.TOP);
        borderStyle.setAlignment(HorizontalAlignment.LEFT);
        borderStyle.setVerticalAlignment(VerticalAlignment.CENTER); // ← 세로 가운데 정렬

        // 바깥쪽만 굵은 테두리 → 내부 셀도 같이 반복
        for (int rowIdx = specialNoteStartRow; rowIdx <= specialNoteStartRow + 2; rowIdx++) {
          Row row = sheet.getRow(rowIdx);
          if (row == null) row = sheet.createRow(rowIdx);

          for (int colIdx = 1; colIdx <= 6; colIdx++) {
            Cell cell = row.getCell(colIdx);
            if (cell == null) cell = row.createCell(colIdx);
            cell.setCellStyle(borderStyle);
          }
        }

        // 바깥쪽 테두리만 굵게 따로 지정
        for (int col = 1; col <= 6; col++) {
          // 위쪽
          Cell topCell = sheet.getRow(specialNoteStartRow).getCell(col);
          topCell.getCellStyle().setBorderTop(BorderStyle.THICK);

          // 아래쪽
          Cell bottomCell = sheet.getRow(specialNoteStartRow + 2).getCell(col);
          bottomCell.getCellStyle().setBorderBottom(BorderStyle.THICK);
        }

        // 왼쪽/오른쪽 테두리는 각 행 첫 번째, 마지막 열에서
        for (int rowIdx = specialNoteStartRow; rowIdx <= specialNoteStartRow + 2; rowIdx++) {
          Row row = sheet.getRow(rowIdx);
          row.getCell(1).getCellStyle().setBorderLeft(BorderStyle.THICK);  // B열
          row.getCell(6).getCellStyle().setBorderRight(BorderStyle.THICK); // G열
        }

        // 6. 병합 시작 셀에 값 설정
        Row noteRow = sheet.getRow(specialNoteStartRow);
        Cell noteCell = noteRow.getCell(1);
        noteCell.setCellValue("***특이사항 : " + header.get("special_note"));

        //파일 생성 후 저장
        workbook.write(fos);

        // 로그 출력
//      log.info("▶ 생성된 발주서 파일 경로: {}", tempXlsx.toAbsolutePath());
        if (Files.exists(tempXlsx)) {
//        log.info("✅ 발주서 파일이 성공적으로 생성되었습니다: {}", tempXlsx.toAbsolutePath());
        } else {
          log.warn("❌ 발주서 파일 생성 실패!");
        }

        //메일 전송
        mailService.sendMailWithAttachment(
            recipients,
            title,
            content,
            tempXlsx.toFile(),
            fileName
        );
//      log.info("✅ 메일 전송 완료: 수신자={}", recipients);
        // 임시 파일 삭제 예약
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
          try {
            Files.deleteIfExists(tempXlsx);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }, 5, TimeUnit.MINUTES);

      } catch (Exception e) {
        e.printStackTrace();
      }

      // 5. 결과 데이터 구성
      Map<String, Object> response = new HashMap<>();
      response.put("baljuData", baljuData);
      response.put("senderInfo", senderInfo);
      response.put("filePath", tempXlsx.toString());
      response.put("fileName", fileName);

      result.data = response;
      return result;

    } catch (Exception e) {
      log.error("❌ 메일 전송 중 서버에서 예외 발생: {}", e.getMessage(), e);
      result.success = false;
      result.message = "메일 전송 중 문제가 발생했습니다: " + e.getMessage();
      return result;
    }
  }

  public static void setCell(Sheet sheet, int rowIdx, int colIdx, String value) {
    Row row = sheet.getRow(rowIdx);
    if (row == null) row = sheet.createRow(rowIdx);
    Cell cell = row.getCell(colIdx);
    if (cell == null) cell = row.createCell(colIdx);
    cell.setCellValue(value);
  }

  private void safeAddMergedRegion(Sheet sheet, int firstRow, int lastRow, int firstCol, int lastCol) {
    CellRangeAddress newRegion = new CellRangeAddress(firstRow, lastRow, firstCol, lastCol);

    // 기존 병합 영역 중 겹치는 것 제거
    for (int i = sheet.getNumMergedRegions() - 1; i >= 0; i--) {
      CellRangeAddress existing = sheet.getMergedRegion(i);
      if (existing.intersects(newRegion)) {
        sheet.removeMergedRegion(i);
      }
    }

    sheet.addMergedRegion(newRegion);
  }

  /**
   * 엑셀 업로드
   * 파일명 :sien_balju_upload
   * 납기 예정일 : B2  # DeliveryDate   --- balju_head
   * 품목 코드: A4     # Material_id    --- balju
   * 품목명:  B4
   * 주문수량: C4      # SujuQty        --- balju
   * 단가: D4          # UnitPrice      --- balju
   * **/
  @PostMapping("/upload_save")
  public AjaxResult uploadSeve(
      @RequestParam("data_date") String data_date,
      @RequestParam("spjangcd") String spjangcd,
      @RequestParam("upload_file") MultipartFile upload_file,
      Authentication auth) {

    User user = (User) auth.getPrincipal();
    AjaxResult res = new AjaxResult();

    try {

      if (upload_file == null || upload_file.isEmpty()) {
        res.success = false;
        res.message = "업로드 파일이 없습니다.";
        res.code = "NO_FILE";
        return res;
      }

      // 1) 임시 저장
      DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
      String ts = LocalDateTime.now().format(dtf);
      String basePath = settings.getProperty("file_temp_upload_path"); // 예: c:\\temp\\mes21\\upload_temp\\
      String uploadFilename = basePath + ts + "_" + upload_file.getOriginalFilename();

      // 🔹 디렉터리 보장
      File dir = new File(basePath);
      if (!dir.exists()) { dir.mkdirs(); }

      File f = new File(uploadFilename);
      if (f.exists()) f.delete();
      try (FileOutputStream out = new FileOutputStream(uploadFilename)) {
        out.write(upload_file.getBytes());
      }

      // 2) 엑셀 파싱
      try (InputStream in = new FileInputStream(uploadFilename);
           Workbook wb = WorkbookFactory.create(in)) {

        Sheet sheet = wb.getSheetAt(0);

        // B2: 납기예정일
        // B2: 납기예정일 안전 파싱
        Row row2 = sheet.getRow(1);
        String deliveryDateIso = data_date; // 기본값

        if (row2 != null) {
          Cell dcell = row2.getCell(1); // B2
          if (dcell != null) {
            if (dcell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dcell)) {
              // ✅ 진짜 날짜 셀
              java.util.Date d = dcell.getDateCellValue();
              deliveryDateIso = new java.text.SimpleDateFormat("yyyy-MM-dd").format(d);
            } else if (dcell.getCellType() == CellType.STRING) {
              // ✅ 문자열 셀 → 유연 파싱
              String s = dcell.getStringCellValue().trim();
              Date parsed = tryParseIsoOrKoreanDate(s); // 아래 헬퍼 참고
              if (parsed != null) deliveryDateIso = CommonUtil.tryString(parsed);
            } else {
              // ✅ 기타 타입 → 표시문자열로 변환 후 유연 파싱
              DataFormatter fmt = new DataFormatter(java.util.Locale.KOREAN);
              String s = fmt.formatCellValue(dcell).trim();
              Date parsed = tryParseIsoOrKoreanDate(s);
              if (parsed != null) deliveryDateIso = CommonUtil.tryString(parsed);
            }
          }
        }


        // A4~: 데이터 읽고 → ID/금액 계산까지 해서 item 구성
        List<Map<String, Object>> items = new ArrayList<>();

        for (int r = 3; r <= sheet.getLastRowNum(); r++) {
          Row row = sheet.getRow(r);
          if (row == null) continue;

          String materialCode = getCellString(row.getCell(0)); // A: 품목 코드
          String materialName = getCellString(row.getCell(1)); // B: 품목명
          Double qty         = getCellNumeric(row.getCell(2)); // C: 수량
          Double unitPrice   = getCellNumeric(row.getCell(3)); // D: 단가

          // 빈 행 스킵
          if ((materialCode == null || materialCode.isEmpty()) &&
              (materialName == null || materialName.isEmpty()) &&
              qty == null && unitPrice == null ){
            continue;
          }

          // ✅ 2-1) 품목 코드 → Material_id
          Integer materialId = materialRepository.findIdByCodeAndSpjangcd(materialCode, spjangcd);
          if (materialId == null) {
            res.success = false;
            res.message = "미등록 품목코드: " + materialCode + " (행 " + (r+1) + ")";
            res.code = "NO_MATERIAL";
            return res;
          }

          // ✅ 2-2) 업체명 → Company (baljuOrderService에 위임)
          Company comp = baljuOrderService.findCompCode(materialCode, spjangcd);
          if (comp == null) {
            res.success = false;
            res.message = "업체 매핑 실패: " + materialCode + " (행 " + (r+1) + ")";
            res.code = "NO_COMPANY"; // 선택
            return res;
          }


          double q  = (qty != null ? qty : 0d);
          double up = (unitPrice != null ? unitPrice : 0d);
          double supply = q * up;
          double vat    = Math.floor(supply * 0.1); // 부가세 10% (정책에 맞게 조정 가능)

          Map<String, Object> item = new HashMap<>();
          item.put("Material_id", materialId);
          item.put("quantity", q);
          item.put("unit_price", up);
          item.put("supply_price", supply);
          item.put("vat", vat);

          item.put("Company_id", comp.getId());
          item.put("CompanyName", comp.getName());

          item.put("due_date", deliveryDateIso);  // 공통 납기일
          item.put("description", materialName);  // 비고로 품목명 저장 (선택)
          item.put("totalEdited", false);         // 자동계산 기본

          items.add(item);

        }

        // 3) payload 구성 → 업체별 발주 저장 (saveBaljuMultiGrouped가 Company_id로 그룹핑)
        Map<String, Object> payload = new HashMap<>();
        payload.put("JumunDate", data_date);
        payload.put("DueDate", deliveryDateIso);
        payload.put("spjangcd", spjangcd);
        payload.put("invatyn", "N");
        payload.put("cboBaljuType", "Regular"); //기본은 Regular(정기) 로 저장
        payload.put("special_note", "");
        payload.put("items", items);

        return saveBaljuMultiGrouped(payload, auth);
      }

    } catch (Exception e) {
      res.success = false;
      res.message = "엑셀 처리 중 오류: " + e.getMessage();
      return res;
    }
  }

  private AjaxResult saveBaljuMultiGrouped(Map<String, Object> payload, Authentication auth) {
    User user = (User) auth.getPrincipal();
    Set<String> priceKeys = new HashSet<>();

    // 공통 헤더 필드
    String jumunDateStr = (String) payload.get("JumunDate");
    String defaultDueStr = (String) payload.get("DueDate");   // B2 기본 납기
    String spjangcd      = (String) payload.get("spjangcd");
    String isVat         = (String) payload.get("invatyn");   // "Y"/"N"
    String specialNote   = (String) payload.get("special_note");
    String sujuType      = (String) payload.get("cboBaljuType"); // "Regular" 등

    Date jumunDate   = CommonUtil.trySqlDate(jumunDateStr);
    Date defaultDue  = CommonUtil.trySqlDate(defaultDueStr);

    List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
    if (items == null || items.isEmpty()) {
      AjaxResult r = new AjaxResult();
      r.success = false;
      r.code = "EMPTY_ITEMS";
      r.message = "업로드할 데이터가 없습니다.";
      return r;
    }

    // ✅ 업체별 그룹핑 (Company_id 우선, 없으면 CompanyName)
    Map<String, List<Map<String, Object>>> grouped = items.stream().collect(
        Collectors.groupingBy(it -> {
          Object cid = it.get("Company_id");
          if (cid != null) return "ID:" + cid.toString();
          String cname = (String) it.getOrDefault("CompanyName", "");
          return "NM:" + cname;
        })
    );

    List<Map<String, Object>> results = new ArrayList<>();

    for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
      List<Map<String, Object>> groupItems = entry.getValue();
      Map<String, Object> first = groupItems.get(0);

      Integer companyId   = CommonUtil.tryIntNull(first.get("Company_id"));
      String  companyName = CommonUtil.tryString(first.get("CompanyName"));

      // 🔹 헤더 생성
      BaljuHead head = new BaljuHead();
      head.setCreated(new Timestamp(System.currentTimeMillis()));
      head.setCreaterId(user.getId());
      head.set_status("manual");

      head.setJumunDate(jumunDate);
      head.setDeliveryDate(defaultDue);     // 기본 납기(행 단위로 덮어쓸 수 있음)
      head.setSujuType(sujuType);
      head.setSpjangcd(spjangcd);
      head.setSpecialNote(specialNote);
      head.setCompanyId(companyId);         // 회사 연결

      String jumunNumber = baljuOrderService.makeJumunNumber(jumunDate);
      head.setJumunNumber(jumunNumber);

      balJuHeadRepository.save(head);

      double totalPriceSum = 0d;

      // 🔹 상세 저장
      for (Map<String, Object> item : groupItems) {

        Integer materialId   = CommonUtil.tryIntNull(item.get("Material_id"));
        Double  qty          = CommonUtil.tryDouble(item.get("quantity"));
        Double  unitPrice    = CommonUtil.tryDouble(item.get("unit_price"));
        Double  supply_price = CommonUtil.tryDouble(item.get("supply_price"));
        Double  vat          = CommonUtil.tryDouble(item.get("vat"));

        // 행 단위 due_date(문자열) → Date
        String rowDueStr = CommonUtil.tryString(item.get("due_date"));
        Date   rowDue    = CommonUtil.trySqlDate(rowDueStr);
        Date   dueDate   = (rowDue != null) ? rowDue : defaultDue;

        String editedFlag = String.valueOf(item.get("totalEdited")).toUpperCase();
        boolean isManual  = "TRUE".equals(editedFlag) || "Y".equals(editedFlag);

        Balju detail = new Balju();
        detail.setBaljuHeadId(head.getId());
        detail.setJumunNumber(head.getJumunNumber());
        detail.setJumunDate(jumunDate);
        detail.setDueDate(dueDate);
        detail.set_audit(user);

        detail.setMaterialId(materialId);
        detail.setCompanyId(companyId);
        detail.setCompanyName(companyName);

        detail.setSujuQty(qty != null ? qty : 0d);
        detail.setUnitPrice(unitPrice != null ? unitPrice : 0d);
        detail.setPrice(supply_price != null ? supply_price : 0d);
        detail.setVat(vat != null ? vat : 0d);

        if (isManual && item.get("total_price") != null) {
          detail.setTotalAmount(CommonUtil.tryDouble(item.get("total_price")));
        } else {
          double auto = (supply_price != null ? supply_price : 0d) + (vat != null ? vat : 0d);
          detail.setTotalAmount(auto);
        }

        detail.setSpjangcd(spjangcd);
        detail.setInVatYN("Y".equalsIgnoreCase(isVat) ? "Y" : "N");
        detail.setSujuType(sujuType);
        detail.setState("draft");
        detail.setSujuQty2(0.0d);
        detail.set_status("manual");

        totalPriceSum += (detail.getTotalAmount() != null ? detail.getTotalAmount() : 0d);
        bujuRepository.save(detail);

        if (materialId != null && companyId != null && unitPrice != null) {
          String key = materialId + "-" + companyId + "-" + unitPrice.intValue();
          if (priceKeys.add(key)) {
            Map<String, Object> priceData = new HashMap<>();
            priceData.put("Material_id", materialId);
            priceData.put("Company_id",  companyId);
            priceData.put("UnitPrice",   unitPrice.intValue());

            String hhmmss = java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            String applyStart = (jumunDate != null
                ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(jumunDate)
                : java.time.LocalDate.now().toString()) + "T" + hhmmss;

            priceData.put("ApplyStartDate", applyStart);
            priceData.put("type", "01");
            priceData.put("user_id", user.getId());

            baljuOrderService.saveCompanyUnitPrice(priceData);
          }
        }

      }

      // 🔹 헤더 합계/수정자 반영
      head.setTotalPrice(totalPriceSum);
      head.setModified(new Timestamp(System.currentTimeMillis()));
      head.setModifierId(user.getId());
      balJuHeadRepository.save(head);

      results.add(Map.of(
          "companyId", companyId,
          "companyName", companyName,
          "headId", head.getId(),
          "jumunNumber", head.getJumunNumber(),
          "totalPrice", totalPriceSum
      ));
    }

    AjaxResult ok = new AjaxResult();
    ok.success = true;
    ok.data = Map.of("count", results.size(), "results", results);
    return ok;
  }

  /** 안전 문자열 추출 */
  private static String getCellString(Cell cell) {
    if (cell == null) return null;
    try {
      if (cell.getCellType() == CellType.STRING) {
        return Optional.ofNullable(cell.getStringCellValue()).orElse("").trim();
      } else if (cell.getCellType() == CellType.NUMERIC) {
        // 코드가 숫자로 들어온 경우 소수점 제거
        return new BigDecimal(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
      } else if (cell.getCellType() == CellType.BOOLEAN) {
        return String.valueOf(cell.getBooleanCellValue());
      }
    } catch (Exception ignore) {}
    return null;
  }

  /** 안전 숫자 추출 */
  private static Double getCellNumeric(Cell cell) {
    if (cell == null) return null;
    try {
      if (cell.getCellType() == CellType.NUMERIC) {
        return cell.getNumericCellValue();
      } else if (cell.getCellType() == CellType.STRING) {
        String s = cell.getStringCellValue();
        if (s == null || s.trim().isEmpty()) return null;
        return Double.parseDouble(s.replace(",", "").trim());
      }
    } catch (Exception ignore) {}
    return null;
  }

  private static Date tryParseIsoOrKoreanDate(String s) {
    if (s == null || s.isBlank()) return null;
    s = s.trim();

    java.time.format.DateTimeFormatter[] fmts = new java.time.format.DateTimeFormatter[] {
        // ISO
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd"),
        java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"),

        // 한글 월 표기
        java.time.format.DateTimeFormatter.ofPattern("d-M월-uuuu", java.util.Locale.KOREAN),
        java.time.format.DateTimeFormatter.ofPattern("dd-M월-uuuu", java.util.Locale.KOREAN),
        java.time.format.DateTimeFormatter.ofPattern("yyyy년 M월 d일", java.util.Locale.KOREAN),

        // 기타 보편
        java.time.format.DateTimeFormatter.ofPattern("M/d/uuuu"),
        java.time.format.DateTimeFormatter.ofPattern("d/M/uuuu")
    };

    for (var f : fmts) {
      try {
        java.time.LocalDate ld = java.time.LocalDate.parse(s, f);
        return java.sql.Date.valueOf(ld);
      } catch (Exception ignore) {}
    }
    return null;
  }


}
