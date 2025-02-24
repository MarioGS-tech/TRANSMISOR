package sv.gov.cnr.factelectrcnrservice.repositories;


import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import sv.gov.cnr.factelectrcnrservice.models.dto.DatosReporteDTO;
import sv.gov.cnr.factelectrcnrservice.models.dto.ReporteIvaConsumidorFinalDTO;
import sv.gov.cnr.factelectrcnrservice.models.dto.ReporteIvaContribuyentesDTO;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ReporteRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<ReporteIvaContribuyentesDTO> obtenerRegistrosIvaContribuyente(DatosReporteDTO datosReporte){

        String fechaDesde = datosReporte.getFechaDesde();
        String fechaHasta = datosReporte.getFechaHasta();
        String sql = " SELECT "
                + "     dt.FECHA_EMISION, "
                + "     4 AS CLASE_DOCUMENTO, "
                + "     t.TIPO_DTE AS TIPO_DOCUMENTO, "
                + "     REPLACE(t.NUMERO_DTE, '-', '') AS NUMERO_RESOLUCION, "
                + "     dt.SELLO_RECIBIDO_MH AS NUMERO_SERIE, "
                + "     REPLACE(dt.CODIGO_GENERACION, '-', '') AS NUMERO_DOCUMENTO, "
                + "     t.ID_TRANSACCION AS NUMERO_CONTROL, "
                + "     CASE WHEN c.NIT_CUSTOMER IS NOT NULL THEN c.NIT_CUSTOMER ELSE c.NRC_CUSTOMER END AS NIT, "
                + "     c.NOMBRE_CLIENTE, "
                + "     TO_CHAR(t.TOTAL_EXENTO, 'FM9999999990.00') AS TOTAL_EXENTO, "
                + "     TO_CHAR(t.TOTAL_NOSUJETO, 'FM9999999990.00') AS TOTAL_NO_SUJETO, "
                + "     TO_CHAR(t.TOTAL_GRAVADO, 'FM9999999990.00') AS TOTAL_GRAVADO, "
                + "     CASE WHEN t.TIPO_DTE = '06' THEN TO_CHAR(t.TOTAL_TRANSACCION, 'FM9999999990.00') ELSE '0.00' END AS DEBITO_FISCAL, "
                + "     '0.00' AS VENTA_TERCEROS, "
                + "     '0.00' AS DEBITO_TERCEROS, "
                + "     TO_CHAR(t.TOTAL_TRANSACCION, 'FM9999999990.00') AS TOTAL_VENTAS, "
                + "     CASE WHEN c.NIT_CUSTOMER IS NULL AND c.NRC_CUSTOMER IS NULL AND c.TIPO_DOCUMENTO = '13' THEN c.NUMERO_DOCUMENTO ELSE NULL END AS DUI, "
                + "     1 AS ANEXO "
                + " FROM "
                + "     CNRPOS_TRANSACCION t "
                + " LEFT JOIN ( "
                + "     SELECT d.ID_TRANSACCION, d.FECHA_EMISION, d.SELLO_RECIBIDO_MH, d.CODIGO_GENERACION "
                + "     FROM CNRPOS_DTE_TRANSACCION d "
                + "     INNER JOIN ( "
                + "         SELECT ID_TRANSACCION, MAX(ID_DTE_TRANSACCION) AS ULT_ID_DTE_TRANSACCION "
                + "         FROM CNRPOS_DTE_TRANSACCION "
                + "         GROUP BY ID_TRANSACCION "
                + "     ) MAX_T ON d.ID_TRANSACCION = MAX_T.ID_TRANSACCION AND d.ID_DTE_TRANSACCION = MAX_T.ULT_ID_DTE_TRANSACCION "
                + " ) dt ON t.ID_TRANSACCION = dt.ID_TRANSACCION "
                + " LEFT JOIN CNRPOS_CLIENTE c ON t.ID_CLIENTE = c.ID_CLIENTE "
                + " WHERE "
                + "     t.STATUS = 2 "
                + "     AND t.TIPO_DTE IN ('03', '05', '06') "
                + "     AND TO_DATE(dt.FECHA_EMISION, 'YYYY-MM-DD') BETWEEN TO_DATE(?, 'YYYY-MM-DD') AND TO_DATE( ? , 'YYYY-MM-DD')";

        return jdbcTemplate.query(sql, ps -> {
            ps.setString(1, fechaDesde);
            ps.setString(2, fechaHasta);
        }, new TransactionRowMapperC());

    }

    public List<ReporteIvaConsumidorFinalDTO> obtenerRegistrosConsumidorFinal(DatosReporteDTO datosReporte) {
        String fechaDesde = datosReporte.getFechaDesde();
        String fechaHasta = datosReporte.getFechaHasta();


        String sql = "SELECT dt.FECHA_EMISION, 4 AS CLASE_DOCUMENTO, t.TIPO_DTE AS TIPO_DOCUMENTO, "
                + "REPLACE(t.NUMERO_DTE, '-', '') AS NUMERO_RESOLUCION, "
                + "dt.SELLO_RECIBIDO_MH AS NUMERO_SERIE, "
                + "NULL AS NUMERO_INTERNO_DEL, "
                + "NULL AS NUMERO_INTERNO_AL, "
                + "NULL AS NUMERO_DOCUMENTO_DEL, "
                + "NULL AS NUMERO_DOCUMENTO_AL, "
                + "NULL AS NRO_MAQUINA_REGISTRADORA, "
                + "TO_CHAR(t.TOTAL_EXENTO, 'FM9999999990.00') AS TOTAL_EXENTO, "
                + "'0.00' AS TOTAL_EXENTO_NO_SUJETO, "
                + "TO_CHAR(t.TOTAL_NOSUJETO, 'FM9999999990.00') AS VENTAS_NO_SUJETAS, "
                + "'0.00' AS VENTAS_GRAVADAS_LOCALES, "
                + "'0.00' AS EXPORTACIONES_CENTROAMERICA, "
                + "'0.00' AS EXPORTACIONES_NO_CENTROAMERICA, "
                + "'0.00' AS EXPORTACIONES_SERVICIOS, "
                + "'0.00' AS VENTAS_ZONA_FRANCA, "
                + "'0.00' AS CUENTA_TERCEROS_NO_DOMICILIADOS, "
                + "TO_CHAR(t.TOTAL_TRANSACCION, 'FM9999999990.00') AS TOTAL_VENTAS, "
                + "2 AS ANEXO "
                + "FROM CNRPOS_TRANSACCION t "
                + "LEFT JOIN ( "
                + "    SELECT d.ID_TRANSACCION, d.FECHA_EMISION, d.SELLO_RECIBIDO_MH, d.CODIGO_GENERACION "
                + "    FROM CNRPOS_DTE_TRANSACCION d "
                + "    INNER JOIN ( "
                + "        SELECT ID_TRANSACCION, MAX(ID_DTE_TRANSACCION) AS ULT_ID_DTE_TRANSACCION "
                + "        FROM CNRPOS_DTE_TRANSACCION "
                + "        GROUP BY ID_TRANSACCION "
                + "    ) MAX_T ON d.ID_TRANSACCION = MAX_T.ID_TRANSACCION AND d.ID_DTE_TRANSACCION = MAX_T.ULT_ID_DTE_TRANSACCION "
                + ") dt ON t.ID_TRANSACCION = dt.ID_TRANSACCION "
                + "LEFT JOIN CNRPOS_CLIENTE c ON t.ID_CLIENTE = c.ID_CLIENTE "
                + "WHERE t.STATUS = 2 AND t.TIPO_DTE IN ('01', '11') "
                + "AND TO_DATE(dt.FECHA_EMISION, 'YYYY-MM-DD') BETWEEN TO_DATE(?, 'YYYY-MM-DD') AND TO_DATE(?, 'YYYY-MM-DD')";

        return jdbcTemplate.query(sql, ps -> {
            ps.setString(1, fechaDesde);
            ps.setString(2, fechaHasta);
        }, new TransactionRowMapperCF());

    }

    private static class TransactionRowMapperC implements RowMapper<ReporteIvaContribuyentesDTO> {
        @Override
            public ReporteIvaContribuyentesDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
                String fechaEmision = rs.getString("FECHA_EMISION");
                BigDecimal claseDocumento = rs.getBigDecimal("CLASE_DOCUMENTO");
                String tipoDocumento = rs.getString("TIPO_DOCUMENTO");
                String numeroResolucion = rs.getString("NUMERO_RESOLUCION");
                String numeroSerie = rs.getString("NUMERO_SERIE");
                String numeroDocumento = rs.getString("NUMERO_DOCUMENTO");
                String numeroControl = rs.getString("NUMERO_CONTROL");
                String nit = rs.getString("NIT");
                String nombreCliente = rs.getString("NOMBRE_CLIENTE");
                String totalExento = rs.getString("TOTAL_EXENTO");
                String totalNoSujeto = rs.getString("TOTAL_NO_SUJETO");
                String totalGravado = rs.getString("TOTAL_GRAVADO");
                String debitoFiscal = rs.getString("DEBITO_FISCAL");
                String ventaTerceros = rs.getString("VENTA_TERCEROS");
                String debitoTerceros = rs.getString("DEBITO_TERCEROS");
                String totalVentas = rs.getString("TOTAL_VENTAS");
                String dui = rs.getString("DUI");
                BigDecimal anexo = rs.getBigDecimal("ANEXO");

                return new ReporteIvaContribuyentesDTO(
                        fechaEmision,
                        claseDocumento,
                        tipoDocumento,
                        numeroResolucion,
                        numeroSerie,
                        numeroDocumento,
                        numeroControl,
                        nit,
                        nombreCliente,
                        Double.valueOf(totalExento),
                        Double.valueOf(totalNoSujeto),
                        Double.valueOf(totalGravado),
                        Double.valueOf(debitoFiscal),
                        Double.valueOf(ventaTerceros),
                        Double.valueOf(debitoTerceros),
                        Double.valueOf(totalVentas),
                        dui,
                        anexo
                );
        }
    }

    private static class TransactionRowMapperCF implements RowMapper<ReporteIvaConsumidorFinalDTO> {

        @Override
        public ReporteIvaConsumidorFinalDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            String fechaEmision = rs.getString("FECHA_EMISION");
            BigDecimal claseDocumento = rs.getBigDecimal("CLASE_DOCUMENTO");
            String tipoDocumento = rs.getString("TIPO_DOCUMENTO");
            String numeroResolucion = rs.getString("NUMERO_RESOLUCION");
            String numeroSerie = rs.getString("NUMERO_SERIE");
            String numeroInternoDel = rs.getString("NUMERO_INTERNO_DEL");
            String numeroInternoAl = rs.getString("NUMERO_INTERNO_AL");
            String numeroDocumentoDel = rs.getString("NUMERO_DOCUMENTO_DEL");
            String numeroDocumentoAl = rs.getString("NUMERO_DOCUMENTO_AL");
            String nroMaquinaRegistradora = rs.getString("NRO_MAQUINA_REGISTRADORA");
            Double totalExento = rs.getDouble("TOTAL_EXENTO");
            Double totalExentoNoSujeto = rs.getDouble("TOTAL_EXENTO_NO_SUJETO");
            Double ventasNoSujetas = rs.getDouble("VENTAS_NO_SUJETAS");
            Double ventasGravadasLocales = rs.getDouble("VENTAS_GRAVADAS_LOCALES");
            Double exportacionesCentroamerica = rs.getDouble("EXPORTACIONES_CENTROAMERICA");
            Double exportacionesNoCentroamerica = rs.getDouble("EXPORTACIONES_NO_CENTROAMERICA");
            Double exportacionesServicios = rs.getDouble("EXPORTACIONES_SERVICIOS");
            Double ventasZonaFranca = rs.getDouble("VENTAS_ZONA_FRANCA");
            Double cuentaTercerosNoDomiciliados = rs.getDouble("CUENTA_TERCEROS_NO_DOMICILIADOS");
            Double totalVentas = rs.getDouble("TOTAL_VENTAS");
            BigDecimal anexo = rs.getBigDecimal("ANEXO");

            return new ReporteIvaConsumidorFinalDTO(
                    fechaEmision, claseDocumento, tipoDocumento, numeroResolucion, numeroSerie, numeroInternoDel,
                    numeroInternoAl, numeroDocumentoDel, numeroDocumentoAl, nroMaquinaRegistradora,
                    totalExento, totalExentoNoSujeto, ventasNoSujetas, ventasGravadasLocales,
                    exportacionesCentroamerica, exportacionesNoCentroamerica, exportacionesServicios,
                    ventasZonaFranca, cuentaTercerosNoDomiciliados, totalVentas, anexo
            );
        }
    }

}
