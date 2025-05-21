package mes.app.transaction.service;


import mes.Encryption.EncryptionUtil;
import mes.app.util.UtilClass;
import mes.domain.dto.BankTransitDto;
import mes.domain.entity.TB_ACCOUNT;
import mes.domain.entity.TB_BANKTRANSIT;
import mes.domain.model.AjaxResult;
import mes.domain.repository.TB_ACCOUNTRepository;
import mes.domain.repository.TB_BANKTRANSITRepository;
import mes.domain.services.SqlRunner;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import static mes.app.util.UtilClass.getStringSafe;

@Service
public class TransactionInputService {


    private final TB_ACCOUNTRepository accountRepository;
    private final SqlRunner sqlRunner;
    private final TB_BANKTRANSITRepository tB_BANKTRANSITRepository;

    public TransactionInputService(TB_ACCOUNTRepository accountRepository, SqlRunner sqlRunner,
                                   TB_BANKTRANSITRepository tB_BANKTRANSITRepository) {
        this.accountRepository = accountRepository;

        this.sqlRunner = sqlRunner;
        this.tB_BANKTRANSITRepository = tB_BANKTRANSITRepository;
    }


    public List<Map<String, Object>> getAccountList(String spjangcd){

        MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("spjangcd", spjangcd);

        String sql = """
                SELECT
                 a.accid as accountid,
                 b.banknm as bankname,
                 b.bankpopcd as managementnum,
                 b.bankid as bankid,
                 accnum as accountNumber,
                 accname as accountName,
                 onlineid as onlineBankId,
                 viewid as viewid,
                 viewpw as viewpw,
                 accpw as paymentPw,
                 accbirth as birth,
                 case when popyn = '1' then true
                 else false
                 end as popyn,
                 case
                     when popsort = '1' then '개인'
                     when popsort = '0' then '법인'
                     else null
                 end as accounttype
                 FROM tb_account a
                left join tb_xbank b on b.bankid = a.bankid
                where a.spjangcd = :spjangcd
                 ORDER BY accid ASC
                """;

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, parameterSource);

        return items;
    }

    public List<Map<String, Object>> getTransactionHistory(String searchfrdate, String searchtodate, String TradeType, Integer parsedAccountId, Integer parsedCompanyId){

        MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("searchfrdate", searchfrdate);
        parameterSource.addValue("searchtodate", searchtodate);
        parameterSource.addValue("accid", parsedAccountId);
        parameterSource.addValue("ioflag", TradeType);
        parameterSource.addValue("cltcd", parsedCompanyId);

        String sql = """
                SELECT to_char(to_date(trdate, 'YYYYMMDD'), 'YYYY-MM-DD') as trade_date
                ,to_char(to_timestamp(trdt, 'YYYYMMDDHH24MISS'), 'HH24:MI') as transactionHour
                ,case
                    when b.ioflag = '0' then '입금'
                    else '출금'
                end as "inoutFlag"
                ,ioid as id
                ,b.trid as transactionTypeId
                ,accin as input_money
                ,accout as output_money
                ,CASE
                 WHEN b.feeamt IS NOT NULL THEN true
                 ELSE false
                END AS commission
                ,b.feeamt as feeamt
                ,d.mijamt as mijamt
                ,remark1 as remark
                ,c."Code" as code
                ,t.tradenm as trade_type
                ,banknm as bankname
                ,b.accnum as account
                ,s."Value" as depositAndWithdrawalType
                ,s."Code" as iotype
                ,c."Name" as "clientName"
                ,c.id as cltcd
                ,b.eumnum as bill
                ,b.etcremark as etc
                ,b.memo as memo
                ,b.accid as accountId
                ,b.eumtodt as expiration
                FROM public.tb_banktransit b
                left join tb_trade t on t.trid = b.trid
                left join sys_code s on s."Code" = b.iotype
                left join company c on c.id = b.cltcd
                left join tb_account d on d.accid = b.accid
                where trdate between :searchfrdate and :searchtodate
                """;

        if(TradeType != null && !TradeType.isEmpty()){
            sql += """
                    AND ioflag = :ioflag
                    """;
        }

        if(parsedAccountId != null){
            sql += """
                    AND accid = :accid
                    """;
        }

        if(parsedCompanyId != null){
            sql += """
                    AND b.cltcd = :cltcd
                    """;
        }

        sql += """
                ORDER BY trdt desc
                """;

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, parameterSource);

        return items;
    }

    public List<Map<String, Object>> getCltCdRelationRemarkList(){

        MapSqlParameterSource parameterSource = new MapSqlParameterSource();

        String sql = """
                select distinct on (remark1)
                remark1,
                cltcd,
                trdt
                from tb_banktransit
                order by remark1, cltcd, trdt desc;
                """;

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, parameterSource);

        return items;
    }

    @Transactional
    public void saveBankTransit(BankTransitDto dto){


        Integer accountId = dto.getAccountId();
        TB_ACCOUNT acc = accountRepository.findById(accountId).orElseGet(() -> null);

        dto.setAccountNumber(acc.getAccnum());

        TB_BANKTRANSIT byId;

        if (dto.getBankTransitId() != null) {
            byId = tB_BANKTRANSITRepository.findById(dto.getBankTransitId()).orElseGet(TB_BANKTRANSIT::new);
        } else {
            byId = new TB_BANKTRANSIT();
        }


        TB_BANKTRANSIT banktransit = BankTransitDto.toEntity(dto, byId);
        tB_BANKTRANSITRepository.save(banktransit);

    }

    @Transactional
    public void deleteBanktransit(List<Integer> parsedidList) {

        tB_BANKTRANSITRepository.deleteAllByIdInBatch(parsedidList);
    }

    @Transactional
    public AjaxResult editBankTransit(Object list) {

        AjaxResult result = new AjaxResult();

        List<Map<String, Object>> parsedList = (List<Map<String, Object>>) list;

        List<Integer> ids = ((List<Map<String, Object>>) list).stream()
                .map(item -> (Integer) item.get("id"))
                .toList();

        List<TB_BANKTRANSIT> entities = tB_BANKTRANSITRepository.findAllById(ids);
        Map<Integer, TB_BANKTRANSIT> entityMap = entities.stream()
                .collect(Collectors.toMap(TB_BANKTRANSIT::getIoid, e-> e));

        for(Map<String, Object> item : parsedList){
            Integer id = (Integer) item.get("id");
            TB_BANKTRANSIT entity = entityMap.get(id);

                Object remark = item.get("remark");
                Object cltcd = item.get("cltcd");
                Object tradeType = item.get("trade_type");
                Object memo = item.get("memo");
                Boolean commission = (Boolean) item.get("commission");

                entity.setRemark1(remark != null ? remark.toString() : null);
                entity.setCltcd(cltcd != null ? UtilClass.parseInteger(cltcd) : null);
                entity.setTrid(UtilClass.parseInteger(tradeType));
                entity.setMemo(UtilClass.getStringSafe(memo));

                if (commission) {
                    String feemat = UtilClass.getStringSafe(item.get("feemat"));
                    Integer parseFeeMat = UtilClass.parseInteger(feemat);
                    Integer mijamt = UtilClass.parseInteger(item.get("mijamt"));

                    if (mijamt == null || mijamt == 0) {
                        result.success = false;
                        result.message = "계좌에 설정된 수수료가 없는 항목이 있습니다.";
                        return result;
                    }

                    entity.setFeeamt(StringUtils.isEmpty(feemat) ? mijamt : parseFeeMat);
                } else {
                    entity.setFeeamt(null);
                }
        }
        result.success = true;
        result.message = "수정되었습니다.";
        return  result;
    }

    public List<Map<String, Object>> searchDetail(Integer cltcd, String searchfrdate, String searchtodate) {

        MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("cltcd", cltcd);
        parameterSource.addValue("searchfrdate", searchfrdate);
        parameterSource.addValue("searchtodate", searchtodate);


        String sql = """
                SELECT
                ioid as id
                ,c."Name" as "clientName" --거래처명
                ,to_char(to_date(trdate, 'YYYYMMDD'), 'YYYY-MM-DD') as trade_date --일자
                ,case when b.ioflag = '0' then '입금' else '출금' end as ioflag -- 구분
                ,accin as input_money -- 금액
                ,accout as output_money -- 금액
                ,b.memo as memo
                ,b.accnum as "accountNumber"
                ,b.banknm as "bankName"
                ,t.tradenm as trade_type
                ,s."Value" as depositAndWithdrawalType
                FROM public.tb_banktransit b
                left join tb_trade t on t.trid = b.trid
                left join sys_code s on s."Code" = b.iotype
                left join company c on c.id = b.cltcd
                where trdate between :searchfrdate and :searchtodate
                AND b.cltcd = :cltcd
                ORDER BY trdt desc
                """;

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, parameterSource);

        return items;
    }

    //음... 단건이라서 그냥 루프 돌림. 하지만 등록계좌가 엄청 많아진다면 일괄조회후 캐싱해서 써야겠지?
    @Transactional
    public void editAccountList(List<Map<String, Object>> list) throws Exception {

        for(Map<String, Object> item : list){
            Integer accountid = UtilClass.parseInteger(item.get("accountid"));

            TB_ACCOUNT acc = accountRepository.findById(accountid).orElseGet(null);

            if(acc != null){
                String accpw = getStringSafe(item.get("paymentpw"));
                if(!accpw.contains("⋆")){
                    acc.setAccpw(EncryptionUtil.encrypt(accpw));
                }

                String viewpw = getStringSafe(item.get("viewpw"));
                if(!viewpw.contains("⋆")){
                    acc.setViewpw(EncryptionUtil.encrypt(viewpw));
                }

                acc.setAccname(getStringSafe(item.get("accountname")));
                acc.setOnlineid(getStringSafe(item.get("onlinebankid")));

                acc.setPopsort(getStringSafe(item.get("accounttype")));
                acc.setAccbirth(getStringSafe(item.get("birth")));

                acc.setViewid(getStringSafe(item.get("viewid")));
                accountRepository.save(acc);
            }
        }

    }
}
