package sv.gov.cnr.factelectrcnrservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sv.gov.cnr.factelectrcnrservice.entities.DteTransaccion;

@Repository
public interface DteTransaccionRepository extends JpaRepository<DteTransaccion, Long> {

     @Query(value = "SELECT * FROM cnrpos_dte_transaccion " +
             "WHERE ID_TRANSACCION = :transaccion ORDER BY ID_DTE_TRANSACCION DESC LIMIT 1",
             nativeQuery = true)
     DteTransaccion findFirstByTransaccionOrderByIdDteTransaccionDesc(Long transaccion);

     @Query(value = "SELECT DTE.*, " +
             "       (CASE WHEN EXISTS (SELECT 1 " +
             "                          FROM cnrpos_dte_transaccion " +
             "                          WHERE ID_TRANSACCION = :transaccion AND ESTADO_DTE = 'ANULADO') " +
             "             THEN 1 " +
             "             ELSE 0 " +
             "        END) AS ANULADO " +
             "FROM cnrpos_dte_transaccion DTE " +
             "WHERE ID_TRANSACCION = :transaccion AND ESTADO_DTE = 'PROCESADO' " +
             "ORDER BY ID_DTE_TRANSACCION DESC " +
             "LIMIT 1",
             nativeQuery = true)
     DteTransaccion getTransaccionForReport(Long transaccion);

     @Query(value = "SELECT COUNT(1) FROM cnrpos_dte_transaccion " +
             "WHERE ID_TRANSACCION = :transaccion AND ESTADO_DTE = 'ANULADO'",
             nativeQuery = true)
     Long dteAnuladoByTransaccion(Long transaccion);
}
