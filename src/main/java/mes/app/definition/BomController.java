package mes.app.definition;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import mes.app.definition.service.BomUploadService;
import mes.app.definition.service.material.UnitPriceService;
import mes.app.sales.service.SujuUploadService;
import mes.config.Settings;
import mes.domain.entity.*;
import mes.domain.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.security.core.Authentication;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mes.app.definition.service.BomService;
import mes.domain.model.AjaxResult;
import mes.domain.services.SqlRunner;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.transaction.Transactional;

@Slf4j
@RestController
@RequestMapping("/api/definition/bom")
public class BomController {
	
	@Autowired
	SqlRunner sqlRunner;	
	
	@Autowired
	BomService bomService;

	@Autowired
	Settings settings;

	@Autowired
	BomUploadService bomUploadService;

	@Autowired
	CompanyRepository companyRepository;

	@Autowired
	ProjectRepository projectRepository;

	@Autowired
	MaterialRepository materialRepository;

	@Autowired
	DepartRepository departRepository;

	@Autowired
	UnitRepository unitRepository;

	@Autowired
	UnitPriceService unitPriceService;
    @Autowired
    private BomRepository bomRepository;

	@RequestMapping("/read")
	public AjaxResult getMaterialList(
			@RequestParam(value="mat_type", required=false) String mat_type,
			@RequestParam(value="mat_group", required=false) Integer mat_group,
			@RequestParam(value="bom_type", required=false) String bom_type,
			@RequestParam(value="mat_name", required=false) String mat_name,
			@RequestParam(value="not_past_flag", required=false) String not_past_flag,
			@RequestParam(value ="spjangcd") String spjangcd
			) {
		
		AjaxResult result = new AjaxResult();  
        result.data = this.bomService.getBomMaterialList(mat_type,mat_group,bom_type, mat_name, not_past_flag,spjangcd);
		return result;
	}	
		

	
	@PostMapping("/save")
	public AjaxResult saveBom(
			@RequestParam(value="id", required = false) Integer id,
			@RequestParam(value="Name") String name,
			@RequestParam(value="Material_id") int materialId,
			@RequestParam(value="StartDate") String startDate,
			@RequestParam(value="EndDate") String endDate,
			@RequestParam(value="BOMType") String bomType,
			@RequestParam(value="Version") String version,
			@RequestParam(value="OutputAmount") float outputAmount,
			@RequestParam(value ="spjangcd") String spjangcd,
			Authentication auth	
			) {				
		
		User user = (User)auth.getPrincipal();
		
		AjaxResult result = new AjaxResult();
		
		startDate = startDate + " 00:00:00";
		endDate = endDate + " 23:59:59";
		
		Timestamp startTs = Timestamp.valueOf(startDate);
		Timestamp endTs = Timestamp.valueOf(endDate);
		
		boolean isSameVersion = this.bomService.checkSameVersion(id, materialId, bomType, version);
		
		if (isSameVersion==true) {
			result.success = false;
			result.message="중복된 BOM버전이 존재합니다.";
			return result;
		}
		
		boolean isDuplicated = this.bomService.checkDuplicatePeriod(id, materialId, bomType, startDate, endDate);
		if (isDuplicated) {
			result.success = false;
			result.message="기간이 겹치는 동일 제품의 \\n BOM이 존재합니다.";
			return result;			
		}
		
		Bom bom = null;
		if (id!=null) {
			bom = this.bomService.getBom(id);
		}else {
			bom = new Bom();
			if (StringUtils.hasText(version)==false) {
				version = "1.0";
			}
		}		
		
		bom.setName(name);
		bom.setMaterialId(materialId);
		bom.setOutputAmount(outputAmount);
		bom.setBomType(bomType);
		bom.setVersion(version);
		bom.setStartDate(startTs);
		bom.setEndDate(endTs);
		bom.set_audit(user);
		bom.setSpjangcd(spjangcd);


		this.bomService.saveBom(bom);		
		result.data = bom.getId();
		
		return result;
		
	}	
	
	@RequestMapping("/detail")
	public AjaxResult getBomDetail(
			@RequestParam(value="id") int id
			) {
		AjaxResult result = new AjaxResult();		
        result.data = this.bomService.getBomDetail(id);		
		return result;		
	}
	
	@RequestMapping("/bom_delete")
	public AjaxResult deleteBom(
			@RequestParam(value="id") int id
			) {
		
		MapSqlParameterSource paramMap = new MapSqlParameterSource();		
		AjaxResult result = new AjaxResult();		
		String sql = "delete from bom where id=:id ";
		paramMap.addValue("id", id);		
		int iRowEffected = this.sqlRunner.execute(sql, paramMap);
		result.data = iRowEffected;
		return result;
	}
	
	@RequestMapping("/material_save")
	public AjaxResult bomComponentSave(
			@RequestParam(value="id" , required = false) Integer id,
			@RequestParam(value="BOM_id") int bom_id,
			@RequestParam(value="Material_id") int materialId,
			@RequestParam(value="Amount") float amt,
			@RequestParam(value="_order",required = false) Integer _order,
			@RequestParam(value="Description",required = false) String description,
			Authentication auth			
			) {
		
		User user = (User)auth.getPrincipal();
		AjaxResult result = new AjaxResult();
		
		BomComponent bomComponent = null;
		
		if (id !=null) {
			// 기존 데이터를 가져온다
			bomComponent = this.bomService.getBomComponent(id);			
		}else {
			//동일한 데이터가 있는지 검사해서 중복이 있으면 리턴
			//신규데이터를 등록한다
			boolean exists = this.bomService.checkDuplicateBomComponent(bom_id, materialId);
			if(exists) {
				result.success=false;
				result.message = "이미 존재하는 품목입니다.";
				return result;
			}			
			bomComponent = new BomComponent();
		}
		
		bomComponent.setBomId(bom_id);
		bomComponent.setMaterialId(materialId);
		bomComponent.setAmount(amt);
		bomComponent.set_order(_order);
		bomComponent.setDescription(description);		
		bomComponent.set_audit(user);
		
		bomComponent = this.bomService.saveBomComponent(bomComponent);
		result.data = bomComponent.getId();
		return result;		
	}
	
	@RequestMapping("/material_detail")
    public AjaxResult bomComponentDetail(
    		@RequestParam(value="id") int id    		
    		) {
    	AjaxResult result = new AjaxResult();    	
    	result.data = this.bomService.getBomComponentDetail(id);    	
    	return result;
    }	
	

	@PostMapping("/material_delete")
	public AjaxResult deleteBomComponent(
			@RequestParam(value="id") int id
			) {
		AjaxResult result = new AjaxResult();		
		result.data = this.bomService.deleteBomComponent(id);		
		return result;		
	}
		
	
	@RequestMapping("/bom_comp_list")
	public AjaxResult getBomCompList(
			@RequestParam(value="id") Integer id
			) {
		AjaxResult result = new AjaxResult();
		String sql = """
	            select bc.id
	            , fn_code_name('mat_type', mg."MaterialType") as mat_type
	            , mg."Name" as group_name
	            , m."Name" as mat_name
	            , m."Code" as mat_code
	            , bc."Amount"
	            , bc."Material_id" as mat_id
	            , m."Unit_id"
	            , u."Name" as unit
	            , bc."Description"
	            , bc."_order" 
	            from bom_comp bc
	            left join material m on bc."Material_id"=m.id
	            left join unit u on u.id = m."Unit_id" 
	            left join mat_grp mg on m."MaterialGroup_id" =mg.id
	            where bc."BOM_id" = :bom_id
	            order by bc."_order"
	    """;		
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("bom_id", id);
		result.data = this.sqlRunner.getRows(sql, paramMap);
		
		return result;		
	}
	
	@RequestMapping("/material_tree_list")
	public AjaxResult getComponentTreeList(
			@RequestParam(value="id") Integer id
			) {		
		AjaxResult result = new AjaxResult();		
		result.data = this.bomService.getBomComponentTreeList(id);		
		return result;		
	}	
	
	
	@PostMapping("/bom_replicate")
	public AjaxResult bomReplicate(
			@RequestParam(value="id") int bom_id,
			Authentication auth
			) {		
		
		User user = (User)auth.getPrincipal();				
		return this.bomService.bomReplicate(bom_id, user);
	}	
	
	@PostMapping("/bom_revision")
	public AjaxResult bomRevision(
			@RequestParam(value="id") int bom_id,
			Authentication auth
			) {		
		User user = (User)auth.getPrincipal();
		return this.bomService.bomRevision(bom_id, user);
	}

	// BOM 엑셀 업로드
	@Transactional
	@PostMapping("/upload_save")
	public AjaxResult saveBomBulkData(
			@RequestParam(value="data_date") String data_date,
			@RequestParam(value="spjangcd") String spjangcd,
			@RequestParam(value="upload_file") MultipartFile upload_file,
			MultipartHttpServletRequest multipartRequest,
			Authentication auth) throws IOException {

		User user = (User)auth.getPrincipal();
		AjaxResult result = new AjaxResult();

		// 1. 파일 저장
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
		String formattedDate = dtf.format(LocalDateTime.now());
		String upload_filename = settings.getProperty("file_temp_upload_path") + formattedDate + "_" + upload_file.getOriginalFilename();

		File file = new File(upload_filename);
		if (file.exists()) file.delete();
		try (FileOutputStream destination = new FileOutputStream(upload_filename)) {
			destination.write(upload_file.getBytes());
		}

		// 2. 엑셀 전체 rows 읽기
		List<List<String>> all_rows = this.bomUploadService.excel_read(upload_filename);

		// 3. 제품명 추출 (2행, 13열~)
		List<String> productNames = new ArrayList<>();
		List<String> productRow = all_rows.get(1); // 2번째 행 (index 1)
		int productStartCol = 12; // 13번째 열 (M열, index 12)
		for (int col = productStartCol; col < productRow.size(); col++) {
			String name = productRow.get(col);
			if (name == null || name.trim().isEmpty()) break;
			productNames.add(name.trim());
		}

		// 4. 자재명 추출 (12행~, J열)
		List<String> materialNames = new ArrayList<>();
		int materialStartRow = 11; // 12번째 행 (index 11)
		int materialNameCol = 9;   // J열 (index 9)
		for (int rowIdx = materialStartRow; rowIdx < all_rows.size(); rowIdx++) {
			List<String> row = all_rows.get(rowIdx);
			if (row.size() <= materialNameCol) break;
			String matName = row.get(materialNameCol);
			if (matName == null || matName.trim().isEmpty()) break;
			materialNames.add(matName.trim());
		}

		// 5. 교점 데이터 추출 (필요자재수량)
		List<BomDetail> bomDetails = new ArrayList<>();
		for (int mIdx = 0; mIdx < materialNames.size(); mIdx++) {
			List<String> row = all_rows.get(materialStartRow + mIdx);
			for (int pIdx = 0; pIdx < productNames.size(); pIdx++) {
				int cellIdx = productStartCol + pIdx;
				if (row.size() <= cellIdx) continue;
				String qtyStr = row.get(cellIdx);
				if (qtyStr == null || qtyStr.trim().isEmpty()) continue;
				try {
					double qty = Double.parseDouble(qtyStr.trim());
					if (qty > 0) {
						BomDetail detail = new BomDetail(productNames.get(pIdx), materialNames.get(mIdx), qty);
						bomDetails.add(detail);
					}
				} catch (Exception ignore) {}
			}
		}

		// 6. 추출된 데이터를 원하는 방식으로 저장 (예시)
		for (BomDetail detail : bomDetails) {
			// 1. 제품, 자재, 수량 정보로 엔티티/테이블 저장 처리
			// 예: bomRepository.save(...), sujuList.add(...) 등
			// ... 사용자 기존 로직 삽입 ...
		}

		result.success = true;
		result.data = bomDetails; // 디버깅용. 실제 서비스 시 삭제 가능
		return result;
	}

	// DTO 예시
	public class BomDetail {
		private String productName;
		private String materialName;
		private double quantity;

		public BomDetail(String s, String s1, double qty) {
		}
		// 생성자/Getter/Setter...
	}

}