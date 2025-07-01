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

    @Autowired
    private BomComponentRepository bomComponentRepository;

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

	@Transactional
	@PostMapping("/upload_save")
	public AjaxResult saveBomBulkData(
			@RequestParam("data_date") String data_date,
			@RequestParam("spjangcd") String spjangcd,
			@RequestParam("upload_file") MultipartFile upload_file,
			Authentication auth) throws IOException {

		User user = (User)auth.getPrincipal();
		Integer userId = user.getId();
		AjaxResult result = new AjaxResult();

		// 파일 저장
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
		String formattedDate = dtf.format(LocalDateTime.now());
		String upload_filename = settings.getProperty("c:\\temp\\mes21\\upload_temp\\") + formattedDate + "_" + upload_file.getOriginalFilename();
		File file = new File(upload_filename);
		if (file.exists()) file.delete();
		try (FileOutputStream destination = new FileOutputStream(upload_filename)) {
			destination.write(upload_file.getBytes());
		}

		// 1. 엑셀 읽기
		List<List<String>> all_rows = this.bomUploadService.excel_read(upload_filename);

		// 2. 제품명 추출
		List<String> productNames = new ArrayList<>();
		List<String> productRow = all_rows.get(0); // 1번째 행 (index 0)
		int productStartCol = 12; // 13번째 열 (M열, index 12)
		for (int col = productStartCol; col < productRow.size(); col++) {
			String name = productRow.get(col);
			if (name == null || name.trim().isEmpty()) break;
			productNames.add(name.trim());
		}

		// 3. 자재명 추출 (2행~, J열)
		List<String> materialNames = new ArrayList<>();
		int materialStartRow = 1; // 2번째 행 (index 1)
		int materialNameCol = 9;   // J열 (index 9)
		String lastMaterialName = null;
		for (int rowIdx = materialStartRow; rowIdx < all_rows.size(); rowIdx++) {
			List<String> row = all_rows.get(rowIdx);
			if (row.size() <= materialNameCol) break;
			String matName = row.get(materialNameCol);

			// 줄바꿈 처리
			if (matName != null) {
				matName = matName.replaceAll("[\\r\\n]+", " ").trim();
			}
			if (matName == null || matName.trim().isEmpty()) {
				matName = lastMaterialName;
			} else {
				lastMaterialName = matName.trim();
			}
			materialNames.add(matName.trim());
		}

		// 4. 제품/자재 id 등록
		Map<String, Integer> productNameToId = new HashMap<>();
		for (String pname : productNames) {
			productNameToId.put(pname, getOrCreateProductId(pname, spjangcd));
		}
		Map<String, Integer> materialNameToId = new HashMap<>();
		for (String mname : materialNames) {
			materialNameToId.put(mname, getOrCreateMaterialId(mname, spjangcd));
		}

		// 5. BOM + BOM_COMP 생성
		List<Bom> bomList = new ArrayList<>();
		List<BomComponent> bomCompList = new ArrayList<>();
		LocalDateTime now = LocalDateTime.now();
		Timestamp startDate = Timestamp.valueOf(now);
//		Timestamp endDate = Timestamp.valueOf(now.plusYears(1));
		Timestamp endDate = Timestamp.valueOf("2100-12-31 00:00:00");

		for (int pIdx = 0; pIdx < productNames.size(); pIdx++) {
			String productName = productNames.get(pIdx);
			Integer productId = productNameToId.get(productName);

			Bom existingBom = bomRepository.findByMaterialIdAndBomTypeAndVersion(
					productId, "manufacturing", "1.0");

			if (existingBom != null) {
				// 이미 존재 → 필요시 값만 update, 아니면 skip
				// existingBom.setOutputAmount(1F); // 예시
				// bomRepository.save(existingBom); // or skip
//				bomList.add(existingBom);
			} else {
				// 신규
				Bom bom = new Bom();
				bom.setName(productName);
				bom.setMaterialId(productId);
				bom.setBomType("manufacturing");
				bom.setVersion("1.0");
				bom.setStartDate(startDate);
				bom.setOutputAmount(1F);
				bom.setEndDate(endDate);
				bom.setSpjangcd(spjangcd);
				bom.set_creater_id(userId);
				bom.set_created(startDate);
				bomList.add(bom);
			}
		}
		bomRepository.saveAll(bomList); // PK(id) 자동생성

		// BOM id 매핑
		List<Bom> savedBoms = bomRepository.findAllByStartDate(startDate); // 또는 위에서 save한 리스트 사용

		// BOM_COMP 등록
		for (int pIdx = 0; pIdx < productNames.size(); pIdx++) {
			Bom bom = bomList.get(pIdx);
			String productName = productNames.get(pIdx);
			Integer productId = productNameToId.get(productName);

			for (int mIdx = 0; mIdx < materialNames.size(); mIdx++) {
				List<String> row = all_rows.get(materialStartRow + mIdx);
				int cellIdx = productStartCol + pIdx;
				if (row.size() <= cellIdx) continue;
				String qtyStr = row.get(cellIdx);
				if (qtyStr == null || qtyStr.trim().isEmpty()) continue;
				try {
					double qty = Double.parseDouble(qtyStr.trim());
					if (qty > 0) {
						String materialName = materialNames.get(mIdx);
						Integer materialId = materialNameToId.get(materialName);
						BomComponent bomComp = new BomComponent();
						bomComp.setBomId(bom.getId());
						bomComp.setMaterialId(materialId);
						bomComp.setAmount((float) qty);
						bomComp.set_creater_id(userId);
						bomComp.set_created(startDate);
						bomComp.set_order(1);
						bomComp.setSpjangcd(spjangcd);
						bomCompList.add(bomComp);
					}
				} catch (Exception ignore) {}
			}
		}
		bomComponentRepository.saveAll(bomCompList);

		// 업로드 파일 삭제 (BOM 처리 이후)
		File uploadedFile = new File(upload_filename);
		if (uploadedFile.exists()) {
			boolean deleted = uploadedFile.delete();
			if (!deleted) {
				// 필요하다면 로그 추가
				System.err.println("업로드 파일 삭제 실패: " + upload_filename);
			}
		}

		result.success = true;
		result.data = bomList;
		return result;
	}

	@Transactional
	public Integer getOrCreateProductId(String productName, String spjangcd) {
		Material prod = materialRepository.findByName(productName);

		if (prod != null) return prod.getId();

		// 신규 등록: 제품 (materialGroupId=46, Code 자동)
		Material newProd = new Material();
		newProd.setName(productName);
		newProd.setMaterialGroupId(46);
		newProd.setCode(getNextMaterialCode()); // '4000' + N
		newProd.set_created(Timestamp.valueOf(LocalDateTime.now()));
		newProd.setFactory_id(1);
		newProd.setUnitId(3);
		newProd.setLotUseYn("0");
		newProd.setMtyn("1");
		newProd.setUseyn("0");
		newProd.setSpjangcd(spjangcd);
		newProd.setWorkCenterId(39);
		newProd.setStoreHouseId(4);
		newProd.setMaterialGroupId(46);
		newProd.setFactory_id(1);
		newProd = materialRepository.save(newProd);
		return newProd.getId();
	}

	@Transactional
	public Integer getOrCreateMaterialId(String materialName, String spjangcd) {
		Material mat = materialRepository.findByName(materialName);
		if (mat != null) return mat.getId();

		// 신규 등록: 자재 (materialGroupId=50, Code 자동)
		Material newMat = new Material();
		newMat.setName(materialName);
		newMat.setMaterialGroupId(50);
		newMat.setCode(getNextMaterialCode()); // '4000' + N
		newMat.set_created(Timestamp.valueOf(LocalDateTime.now()));
		newMat.setFactory_id(1);
		newMat.setUnitId(3);
		newMat.setSpjangcd(spjangcd);
		newMat.setLotUseYn("0");
		newMat.setMtyn("1");
		newMat.setUseyn("0");
		newMat.setWorkCenterId(39);
		newMat.setStoreHouseId(3);
		newMat.setMaterialGroupId(50);
		newMat.setFactory_id(1);
		newMat = materialRepository.save(newMat);
		return newMat.getId();
	}

	/** material.code의 다음 '4000'+N 값을 생성하는 메서드 (실제 구현 필요!) */
	public String getNextMaterialCode() {
		String maxCode = materialRepository.findMaxCodeBy4000Prefix();
		int nextNumber = 4000;
		if (maxCode != null && !maxCode.isEmpty()) {
			try {
				int codeNum = Integer.parseInt(maxCode);
				nextNumber = codeNum + 1;
			} catch (NumberFormatException ignore) {}
		}

		// 최종 insert 직전 중복 체크
		while (materialRepository.existsByCode(String.valueOf(nextNumber))) {
			nextNumber++;
		}
		return String.valueOf(nextNumber);
	}



}