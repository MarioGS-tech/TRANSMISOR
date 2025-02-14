package sv.gov.cnr.factelectrcnrservice.procesador;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sv.gov.cnr.factelectrcnrservice.entities.ComprobantePago;
import sv.gov.cnr.factelectrcnrservice.entities.DteTransaccion;
import sv.gov.cnr.factelectrcnrservice.exceptions.DTEException;
import sv.gov.cnr.factelectrcnrservice.models.dto.EspecificoCP;
import sv.gov.cnr.factelectrcnrservice.models.enums.EstatusCola;
import sv.gov.cnr.factelectrcnrservice.services.ColaService;
import sv.gov.cnr.factelectrcnrservice.services.ComprobantePagoService;
import sv.gov.cnr.factelectrcnrservice.services.DTEService;
import sv.gov.cnr.factelectrcnrservice.services.DteTransaccionService;
import sv.gov.cnr.factelectrcnrservice.services.TransaccionService;
import sv.gov.cnr.factelectrcnrservice.utils.Validator;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class EjecutorDeCola {

    private final ColaService colaService;
    private final LockRegistry lockRegistry;
    private final JdbcTemplate jdbcTemplate;
    private final DTEService dteService;
    private final TransaccionService transaccionService;
    private final Validator validator;
    private final DteTransaccionService dteTransaccionService;

    private final ComprobantePagoService comprobantePagoService;

    private static final String estadoAprobado = "PROCESADO";
    private static final String contingenciaRecibido = "RECIBIDO";

    @Async("dteNormalTaskExecutor")
    @Scheduled(fixedRate = 3000)
    public void envioDteOperacionNormal() throws Exception {
        Boolean presentado = false;
        var registroEnColaOpt = colaService.obtenerRegistroOperacionNormal();
        if (registroEnColaOpt.isPresent()) {

            var registroEnCola = registroEnColaOpt.get();

            Optional<DteTransaccion> transaccionDTEValidar = dteTransaccionService
                    .findDteTransaccionByTransaccionValidar(registroEnCola.getTransaccion());
            String estadoDTETransaccion = null;

            if (transaccionDTEValidar.isPresent()) {
                DteTransaccion transaccionDTEV = transaccionDTEValidar.get();
                estadoDTETransaccion = transaccionDTEV.getEstadoDte();
            }

            if (estadoDTETransaccion == null || !estadoDTETransaccion.equalsIgnoreCase(estadoAprobado)) {
                var lock = lockRegistry.obtain(String.valueOf(registroEnCola.getIdCola()));
                if (lock.tryLock()) {
                    var transaccion = registroEnCola.getTransaccion();

                    Boolean condicionesMinimas = validator.condicionesMinimas(transaccion);
                    try {
                        log.info("Elemento de la cola ID {} bloqueado", registroEnCola.getIdCola());
                        log.info("El siguente registro en cola es %s".formatted(registroEnCola));
                        if (condicionesMinimas) {
                            Object dte = dteService.crearDTE(transaccion); // Creación
                            dteService.firmarDte(transaccion, dte); // Firma
                            String estado = dteService.transmitirDte(transaccion); // Transmisión
                            if (estado.equals(estadoAprobado)) {
                                presentado = true;
                                if (registroEnCola.getEsContingencia()) {
                                    transaccion.setStatus(EstatusCola.APROBADO_CONTINGENCIA);
                                } else {
                                    transaccion.setStatus(EstatusCola.APROBADO);
                                }

                                registroEnCola.setEstatusCola(EstatusCola.ENVIADO);
                                dteService.enviarDte(transaccion); // Envío
                                // llamar metodo para la funcion ING_DTE_MODESP_CP
                                log.info("Cambio de especifico para la transaccion {}", transaccion.getIdTransaccion());
                                actualizarComprobantePago(transaccion.getIdTransaccion());

                            } else {
                                transaccion.setStatus(EstatusCola.RECHAZADO);
                                registroEnCola.setEstatusCola(EstatusCola.ERROR);
                            }
                            transaccionService.actualizarTransaccion(transaccion);
                            registroEnCola.setFinalizado(Boolean.TRUE);
                            colaService.save(registroEnCola);
                        } else {
                            log.warn("No se cumplieron las condiciones minimas de configuración para transmitir DTEs");
                        }

                    } catch (Exception e) {
                        log.error("Error al enviar el DTE: %s".formatted(e.getMessage()));
                        log.error("Error al conectarse con el firmador: %s".formatted((e.getMessage())));
                        log.info("Consulta de DTE");
                        if (!presentado) {
                            var estado = dteService.consultarDte(transaccion);
                            if (estado != null) {
                                if (estado.equals(estadoAprobado)) {
                                    if (registroEnCola.getEsContingencia()) {
                                        transaccion.setStatus(EstatusCola.APROBADO_CONTINGENCIA);
                                    } else {
                                        transaccion.setStatus(EstatusCola.APROBADO);
                                    }
                                    registroEnCola.setEstatusCola(EstatusCola.ENVIADO);
                                    dteService.enviarDte(transaccion);
                                } else {
                                    transaccion.setStatus(EstatusCola.RECHAZADO);
                                    registroEnCola.setEstatusCola(EstatusCola.ERROR);
                                }
                                registroEnCola.setFinalizado(Boolean.TRUE);
                                colaService.save(registroEnCola);
                            } else {
                                transaccion.setStatus(EstatusCola.ERROR);
                            }
                        } else {
                            registroEnCola.setFinalizado(Boolean.TRUE);
                        }
                    } finally {
                        log.info("Finalizó el proceso");
                        if (condicionesMinimas) {
                            if (registroEnCola.getEsContingencia().equals(Boolean.FALSE)) {
                                registroEnCola.setNroIntento(registroEnCola.getNroIntento() + 1);
                            } else {
                                registroEnCola.setNroIntentoCont(registroEnCola.getNroIntentoCont() + 1);
                            }
                            if (registroEnCola.getNroIntento() == 3 && !registroEnCola.getEsContingencia()) {
                                registroEnCola.setEsContingencia(Boolean.TRUE);
                                transaccion.setStatus(EstatusCola.MARCADO_CONTINGENCIA);
                            }
                        }
                        colaService.save(registroEnCola);
                        transaccionService.actualizarTransaccion(transaccion);
                        lock.unlock();
                    }
                } else {
                    log.warn("lock para el ID {} no adquirido", registroEnCola.getIdCola());
                }
            }
        } else {
            log.info("Cola de operación normal está vacia...");
        }
    }

    @Async("dteContingenciaTaskExecutor")
    @Scheduled(fixedRate = 8000)
    public void envioDteOperacionContingencia() throws Exception {
        var registroEnColaOpt = colaService.obtenerRegistroOperacionaContingencia();
        if (registroEnColaOpt.isPresent()) {
            var registroEnCola = registroEnColaOpt.get();
            var lock = lockRegistry.obtain(String.valueOf(registroEnCola.getIdCola()));
            if (lock.tryLock()) {
                var transaccion = registroEnCola.getTransaccion();
                try {
                    log.info("Elemento de la cola ID {} bloqueado", registroEnCola.getIdCola());
                    log.info("El siguente registro en cola es %s".formatted(registroEnCola));
                    Object dte = dteService.crearDTE(transaccion); // Creación
                    dteService.firmarDte(transaccion, dte); // Firma
                    String estado = dteService.notificarContigencia(transaccion); // Transmisión
                    if (estado.equals(contingenciaRecibido)) {
                        transaccion.setStatus(EstatusCola.APROBADO_CONTINGENCIA);
                        registroEnCola.setEstatusCola(EstatusCola.ENVIADO);
                        registroEnCola.setNotificadoContigencia(Boolean.TRUE);
                        colaService.save(registroEnCola);
                    } else {
                        transaccion.setStatus(EstatusCola.RECHAZADO);
                        registroEnCola.setFinalizado(Boolean.TRUE);
                        registroEnCola.setEstatusCola(EstatusCola.ERROR);
                        colaService.save(registroEnCola);
                    }
                    transaccionService.actualizarTransaccion(transaccion);
                } catch (Exception e) {
                    log.error("Error al enviar el DTE: %s".formatted(e.getMessage()));
                } finally {
                    log.info("Finalizó el proceso de notificación de contigencia");
                    lock.unlock();
                }
            } else {
                log.warn("lock para el ID {} no adquirido", registroEnCola.getIdCola());
            }
        } else {
            log.info("Cola de operación contingencia está vacia...");
        }
    }

    private void actualizarComprobantePago(Long idTransaccion) throws DTEException {
        List<ComprobantePago> comprobantesPago = comprobantePagoService.listComprobanteTransaccion(idTransaccion);

        for (ComprobantePago cp : comprobantesPago) {
            EspecificoCP especificoCP = comprobantePagoService.cambioEspecificoCP(cp.getNumeroComprobante());

            // Serializar el objeto JSON a una cadena
            String jsonResult = serializeToJson(especificoCP);

            // Almacenar el JSON serializado en el campo requestEspecifico
            cp.setRequestEspecifico(jsonResult);

            // Guardar el comprobante de pago actualizado
            comprobantePagoService.save(cp);
        }
    }

    // Método auxiliar para serializar el objeto JSON a una cadena
    private String serializeToJson(Object object) {
        try {
            // Utiliza una biblioteca de JSON como Jackson, Gson, etc.
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error al serializar el objeto a JSON", e);
        }
    }

}
