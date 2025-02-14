package sv.gov.cnr.factelectrcnrservice.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Entity
@Table(name = "CNRPOS_TOKEN_MH")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TokenMh {
    @Id
    private String token;
    @CreationTimestamp
    private Timestamp fechaGeneracion;
}
