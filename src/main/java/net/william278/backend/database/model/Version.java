package net.william278.backend.database.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "version")
public class Version {

    @Id
    Integer id;
    String name;
    Instant timestamp;
    @ManyToOne
    Project project;
    @ManyToOne
    Channel channel;

}
