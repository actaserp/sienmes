package mes.app.balju.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class BalJuOptimalStockService {

  @Autowired
  SqlRunner sqlRunner;

  public List<Map<String, Object>> getList(String matName, String status, Timestamp start, Timestamp end, String spjangcd) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("matName", matName);
    paramMap.addValue("status", status);
    paramMap.addValue("start", start);
    paramMap.addValue("end", end);
    paramMap.addValue("spjangcd", spjangcd);

    String sql = """
        WITH
        -- 0) 자재 마스터(+단위/안전재고)
        mat AS (
          SELECT
            m.id,
            m."Code"  AS material_code,
            m."Name"  AS material_name,
            u."Name"  AS unit_name,
            m.spjangcd,
           COALESCE(
              NULLIF(regexp_replace(m."Avrqty", '[^0-9\\.]', '', 'g'), '')::numeric,
              0
            ) AS safety_stock
          FROM material m
          LEFT JOIN unit u ON u.id = m."Unit_id"
        ),
        -- 1) 수주(완제품) 집계: 기간/사업장 필터, SujuQty2 우선
        orders_fg AS (
          SELECT
            s."Material_id" AS fg_id,
            SUM(
              CASE WHEN COALESCE(s."SujuQty2",0)>0 THEN s."SujuQty2"
                   ELSE s."SujuQty" END
            )::numeric AS fg_qty
          FROM suju_head h
          JOIN suju s ON s."SujuHead_id" = h.id
          WHERE h.spjangcd = :spjangcd
            AND h."JumunDate" >= COALESCE(CAST(:start AS date), h."JumunDate")
            AND h."JumunDate" <= COALESCE(CAST(:end   AS date), h."JumunDate")
          GROUP BY s."Material_id"
        ),
        -- 2) 유효 BOM 1건 선택(현재 유효 + 최신 StartDate 우선)
        bom_pick AS (
          SELECT DISTINCT ON (b."Material_id")
            b.id AS bom_id,
            b."Material_id" AS fg_id,
            b."OutputAmount"::numeric AS output_amount
          FROM bom b
          WHERE (b."StartDate" IS NULL OR b."StartDate" <= now())
            AND (b."EndDate"   IS NULL OR b."EndDate"   >= now())
          ORDER BY b."Material_id", COALESCE(b."StartDate",'1900-01-01'::timestamp) DESC
        ),
        -- 3) BOM 전개: 자재 필요량(= 수주량 × Amount/OutputAmount)
        exploded AS (
          SELECT
            bc."Material_id"     AS comp_id,
            o.fg_id,
            o.fg_qty,
            bp.output_amount,
            bc."Amount"::numeric AS amount_per_output,
            ROUND(
          (o.fg_qty * (bc."Amount"::numeric / NULLIF(bp.output_amount,0)))::numeric,
          4
        ) AS order_qty
          FROM orders_fg o
          JOIN bom_pick  bp ON bp.fg_id = o.fg_id
          JOIN bom_comp  bc ON bc."BOM_id" = bp.bom_id
        ),
        -- 4) 현재고(입출고 누적)
        stock_now AS (
          SELECT
            mi."Material_id" AS material_id,
            SUM(COALESCE(mi."InputQty",0)) - SUM(COALESCE(mi."OutputQty",0)) AS current_stock
          FROM mat_inout mi
          WHERE mi.spjangcd = :spjangcd
          GROUP BY mi."Material_id"
        ),
        -- 5) 발주 수량(취소 제외) : material 기준 오더 합
        po_order AS (
          SELECT
            b."Material_id" AS material_id,
            SUM(
              CASE WHEN COALESCE(b."SujuQty2",0)>0 THEN b."SujuQty2"
                   ELSE b."SujuQty" END
            )::numeric AS ordered_qty
          FROM balju_head bh
          JOIN balju b ON b."BaljuHead_id" = bh.id
          WHERE bh.spjangcd = :spjangcd
            AND COALESCE(b."State",'') <> 'canceled'
          GROUP BY b."Material_id"
        ),
        -- 6) 발주대상 입고누적(해당 PO건으로 입고된 수량 합계)
        po_receipt AS (
          SELECT
            b."Material_id" AS material_id,
            SUM(COALESCE(mi."InputQty",0))::numeric AS received_qty_on_po
          FROM balju b
          LEFT JOIN mat_inout mi
                 ON mi."Material_id" = b."Material_id"  -- 핵심 링크
          GROUP BY b."Material_id"
        ),
        -- 7) 미입고수량(오픈 발주잔량 = 발주합 - (해당발주로 입고된 합))
        incoming AS (
          SELECT
            m.id AS material_id,
            GREATEST(COALESCE(po.ordered_qty,0) - COALESCE(rcv.received_qty_on_po,0), 0)::numeric AS incoming_qty
          FROM material m
          LEFT JOIN po_order  po ON po.material_id = m.id
          LEFT JOIN po_receipt rcv ON rcv.material_id = m.id
        )
        -- 최종: 그리드 컬럼 매핑
        SELECT *
          FROM (
            SELECT
              m.material_code,
              m.material_name,
              m.unit_name,
              COALESCE(e.order_qty,0)::numeric      AS order_qty,
              COALESCE(i.incoming_qty,0)::numeric   AS incoming_qty,
              COALESCE(sn.current_stock,0)::numeric AS current_stock,
              COALESCE(m.safety_stock,0)::numeric   AS optimal_stock,
              GREATEST(
                (COALESCE(e.order_qty,0) + COALESCE(m.safety_stock,0))
                - (COALESCE(sn.current_stock,0) + COALESCE(i.incoming_qty,0)),
                0
              )::numeric AS need_more_qty,
              CASE
                WHEN (COALESCE(sn.current_stock,0) + COALESCE(i.incoming_qty,0))\s
                       >= (COALESCE(e.order_qty,0) + (m.safety_stock * 2)) THEN '여유'
                WHEN (COALESCE(sn.current_stock,0) + COALESCE(i.incoming_qty,0))\s
                       >= (COALESCE(e.order_qty,0) + COALESCE(m.safety_stock,0)) THEN '적정'
                ELSE '부족'
              END AS state
            FROM exploded e
            JOIN mat m            ON m.id = e.comp_id
            LEFT JOIN stock_now sn ON sn.material_id = e.comp_id
            LEFT JOIN incoming  i  ON i.material_id = e.comp_id
          ) t
          WHERE 1=1
      """;

    // 품명(키워드) 필터  ← 바깥 스코프 컬럼명 사용
    if (matName != null && !matName.isEmpty()) {
      sql += " AND t.material_name ILIKE :matName ";
      paramMap.addValue("matName", "%" + matName + "%");
    }

    // 상태 필터  ← raw_gap 기준으로 동일 CASE 재작성
    if (status != null && !status.isBlank() && !"전체".equals(status.trim())) {
      String st = status.trim();
      switch (st.toLowerCase()) {
        case "shortage":
        case "lack":
        case "insufficient": st = "부족"; break;
        case "proper":
        case "ok":
        case "equal":       st = "적정"; break;
        case "excess":
        case "surplus":     st = "여유"; break;
        default: break;
      }
      sql += " AND t.state = :status ";
      paramMap.addValue("status", st);
    }

    sql += " ORDER BY t.material_code";

//    log.info("paramMap:{}", paramMap);
//    log.info("수주량 대비 적정재고(Avrqty) 현황 sql:{}", sql);

    return sqlRunner.getRows(sql, paramMap);
  }


}
