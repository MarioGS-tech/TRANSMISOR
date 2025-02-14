package sv.gov.cnr.factelectrcnrservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sv.gov.cnr.factelectrcnrservice.config.AppConfig;
import sv.gov.cnr.factelectrcnrservice.entities.Cola;
import sv.gov.cnr.factelectrcnrservice.entities.DteTransaccion;
import sv.gov.cnr.factelectrcnrservice.entities.Transaccion;
import sv.gov.cnr.factelectrcnrservice.exceptions.DTEException;
import sv.gov.cnr.factelectrcnrservice.factory.DTEFactory;
import sv.gov.cnr.factelectrcnrservice.models.dto.MotivoAnulacionDTO;
import sv.gov.cnr.factelectrcnrservice.models.enums.TipoDTE;
import sv.gov.cnr.factelectrcnrservice.repositories.DteTransaccionRepository;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DTEService {

    private final DTEFactory dteFactory;
    private final HaciendaApiService haciendaApiService;
    private final ReporteService reporteService;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    private final ColaService colaService;
    private final DteTransaccionService dteTransaccionService;
    private final DocumentosCNRServices documentosCNRServices;

    /**
     * @param transaccion transaccion en cola para crear DTE
     * @throws Exception error al generar instancia de DTE
     */
    public Object crearDTE(Transaccion transaccion) throws Exception{
        log.info("Creando DTE para la transacción: #" + transaccion.getIdTransaccion());
        var objetoDte = crearJSON(transaccion);
        guardarJson(transaccion, objetoDte);
        return objetoDte;
    }

    /**
     * @param transaccion transacción en cola para ser firmada
     * @throws DTEException error al firmar dte
     * @throws JsonProcessingException error al parsear json
     */
    public void firmarDte(Transaccion transaccion, Object dte) throws DTEException, JsonProcessingException {
        log.info("Firmando DTE para la transacción: #" + transaccion.getIdTransaccion());
        DteTransaccion infoDte = dteTransaccionService.findDteTransaccionByTransaccion(transaccion);
        var jsonFirmado = haciendaApiService.firmarDocumento(dte);
        guardarJsonFirmado(transaccion, jsonFirmado);
    }

    public String transmitirDte(Transaccion transaccion) throws DTEException {
        log.info("Transmitiendo DTE a servicios de Hacienda para la transacción: #"+ transaccion.getIdTransaccion() );
        return haciendaApiService.enviarDTE(transaccion);
    }

    public String consultarDte(Transaccion transaccion)  {
        try {
            return haciendaApiService.consultarDte(transaccion);
        } catch (DTEException e) {
            throw new RuntimeException("No fue posible realizar la consulta del DTE");
        }
    }

    public void enviarDte(Transaccion transaccion) throws Exception {
        log.info("Envio del DTE al receptor para la transacción: #" + transaccion.getIdTransaccion());
        ByteArrayOutputStream  dtePdf = reporteService.generateReportToStream(transaccion);
        emailService.sendDteViaEmail(transaccion, dtePdf, transaccion.getCliente().getEmail());
        documentosCNRServices.subirArchivo(dtePdf,transaccion);
    }

    public void enviarDteAnulado(Transaccion transaccion) throws Exception {
        log.info("Envio del DTE anulado al receptor para la transacción: #" + transaccion.getIdTransaccion());
        ByteArrayOutputStream  dtePdf = reporteService.generateReportToStream(transaccion);
        emailService.sendDteViaEmailAnulado(transaccion, dtePdf, transaccion.getCliente().getEmail());
        documentosCNRServices.subirArchivo(dtePdf,transaccion);
    }

    public String notificarContigencia(Transaccion transaccion) throws DTEException {
        return haciendaApiService.notificarContingencia(transaccion);
    }


    private void guardarJson(Transaccion transaccion, Object dte) throws JsonProcessingException {
        DteTransaccion infoDte = dteTransaccionService.findDteTransaccionByTransaccion(transaccion);
        infoDte.setDteJson(objectMapper.writeValueAsString(dte));
        dteTransaccionService.save(infoDte);
    }

    private void guardarJsonFirmado(Transaccion transaccion, String jsonFirmado){
        DteTransaccion infoDte = dteTransaccionService.findDteTransaccionByTransaccion(transaccion);
        infoDte.setDteJsonFirmado(jsonFirmado);
        dteTransaccionService.save(infoDte);
    }


    private Object crearJSON(Transaccion transaccion) throws Exception {
        Cola cola = colaService.obtenerInfoCola(transaccion);
        TipoDTE tipoDte;
         if(cola.getEsContingencia().equals(Boolean.TRUE) && cola.getNotificadoContigencia().equals(Boolean.FALSE)){
            tipoDte = TipoDTE.CONTINGENCIA;
        }else {
            tipoDte = TipoDTE.obtenerPorCodigo(transaccion.getTipoDTE());

        }
        var dte = dteFactory.fabricarObjetoDTE(tipoDte);
        return dte.crearDTEJSON(transaccion);
    }

    public Map<String, Object> invalidarDte(Transaccion transaccion, MotivoAnulacionDTO dataMotivo) throws DTEException, JsonProcessingException {
        var dte = dteFactory.fabricarObjetoDTE(TipoDTE.INVALIDACION);
        var jsonInvalidacion = dte.crearJsonInvalidacion(transaccion, dataMotivo);
        guardarJson(transaccion, jsonInvalidacion);
        firmarDte(transaccion, jsonInvalidacion);
        Map<String, Object> data = haciendaApiService.invalidarDte(transaccion);
        return data;
    }


}
