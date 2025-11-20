package mes.app.sales.service;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SujuDeliveryStatusService {

  @Autowired
  SqlRunner sqlRunner;


  public List<Map<String, Object>> getList(LocalDate start, LocalDate  end, String mat) {
    MapSqlParameterSource param = new MapSqlParameterSource();
    param.addValue("start", start);
    param.addValue("end", end);
    param.addValue("mat", mat);

    String sql = """
            SELECT
                c."Name" AS com_name,
                d."JumunDate",
                d.id AS suju_id,
                m."Name" AS mat_name,
                d."SujuQty",
                s.ship_date,
                COALESCE(s.ship_oty, 0) AS ship_oty,
                CASE
                    WHEN d."SujuQty" > 0 THEN
                        ROUND(
                          (COALESCE(s.ship_oty, 0) / d."SujuQty")::numeric * 100, 2)
                        ELSE
                          0
                END AS "SujuRate"
                          FROM suju d
              LEFT JOIN suju_head h ON h.id = d."SujuHead_id"
              LEFT JOIN (
                SELECT
                  s."SourceDataPk",                     -- 🔁 suju.id에 해당
                  SUM(s."Qty") AS ship_oty,             -- 출고 수량 합계
                  sh."ShipDate" as ship_date
                FROM shipment s
              left join shipment_head sh on sh.id = s."ShipmentHead_id"
                GROUP BY s."SourceDataPk", sh."ShipDate"
              ) s ON s."SourceDataPk" = d.id
              LEFT JOIN company c ON c.id = h."Company_id"
              LEFT JOIN material m ON m.id = d."Material_id"
              WHERE 1=1
              AND d."JumunDate" BETWEEN :start AND :end
        """;
    if (StringUtils.isEmpty(mat)==false)
      sql+="and upper(m.\"Name\") like concat('%%',upper(:mat),'%%')";

    sql+= """
        order by d."JumunDate", d.id;
        """;

//    log.info("수주별납품현황 SQL: {}", sql);
//    log.info("수주별납품현황 데이터: {}", param.getValues());
    return this.sqlRunner.getRows(sql, param);
  }
}
