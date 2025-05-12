package mes.app.transaction.service;


import mes.app.util.UtilClass;
import mes.domain.dto.BankTransitDto;
import mes.domain.entity.TB_BANKTRANSIT;
import mes.domain.repository.TB_ACCOUNTRepository;
import mes.domain.repository.TB_BANKTRANSITRepository;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

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
                 accnum as accountNumber,
                 accname as accountName,
                 onlineid as onlineBankId,
                 onlinepw as onlineBankPw,
                 accpw as paymentPw,
                 accbirth as birth,
                 case when popyn = '1' then true
                 else false
                 end as popyn,
                 case when popsort = '1' then '개인'
                 else '법인'
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
                ,ioid as id
                ,accin as input_money
                ,accout as output_money
                ,feeamt as commission
                ,remark1 as remark
                ,c."Code" as code
                ,t.tradenm as trade_type
                ,banknm as bankname
                ,accnum as account
                ,s."Value" as depositAndWithdrawalType
                ,c."Name" as "clientName"
                ,c.id as cltcd
                ,b.memo as memo
                FROM public.tb_banktransit b
                left join tb_trade t on t.trid = b.trid
                left join sys_code s on s."Code" = b.iotype
                left join company c on c.id = b.cltcd
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
    public Boolean saveBanktransit(BankTransitDto dto) {

        TB_BANKTRANSIT banktransit = BankTransitDto.toEntity(dto);
        tB_BANKTRANSITRepository.save(banktransit);

        return true;
    }

    @Transactional
    public void deleteBanktransit(List<Integer> parsedidList) {

        tB_BANKTRANSITRepository.deleteAllByIdInBatch(parsedidList);
    }

    @Transactional
    public void editBankTransit(Object list) {

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

            if(entity != null){

                Object remark = item.get("remark");
                Object cltcd = item.get("cltcd");
                Object tradeType = item.get("trade_type");
                Object iotype = item.get("depositandwithdrawaltype");
                Object memo = item.get("memo");

                entity.setRemark1(remark != null ? remark.toString() : null);
                entity.setCltcd(cltcd != null ? UtilClass.parseInteger(cltcd) : null);
                entity.setTrid(UtilClass.parseInteger(tradeType));
                entity.setIotype(iotype != null ? iotype.toString() : null);
                entity.setMemo(memo != null ? memo.toString() : null);
            }
        }
        System.out.println(entityMap);
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
                ,t.tradenm as trade_type
                ,b.balance
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
}
