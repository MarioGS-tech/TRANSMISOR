package sv.gov.cnr.factelectrcnrservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import sv.gov.cnr.factelectrcnrservice.entities.Rango;

@Repository
public interface RangoRepository extends JpaRepository<Rango, Long> {

    @Query(value = "SELECT r.ACTUAL_VALOR FROM cnrpos_rango r " +
            "WHERE r.ACTIVE = 1 AND r.ID_TIPO_RANGO = :tipoDte " +
            "AND r.ID_SUCURSAL = :idSucursal " +
            "AND r.ACTUAL_VALOR + 1 BETWEEN r.INICIO_RANGO AND r.FINAL_RANGO",
            nativeQuery = true)
    Integer findRangoActivoPorDte(@Param("tipoDte") Integer tipoDte, @Param("idSucursal") Long idSucursal);

    @Modifying
    @Transactional
    @Query(value = "UPDATE cnrpos_rango r SET r.ACTUAL_VALOR = :nuevoValor " +
            "WHERE r.ACTIVE = 1 AND r.ID_TIPO_RANGO = :tipoDte " +
            "AND r.ID_SUCURSAL = :idSucursal",
            nativeQuery = true)
    Integer updateRangoActivoPorDte(@Param("tipoDte") Integer tipoDte,
                                    @Param("nuevoValor") Integer nuevoValor,
                                    @Param("idSucursal") Long idSucursal);
}
