package com.work.broker.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventMessage implements Serializable {

    private String id;
    private String type;
    private String content;
    private LocalDateTime timestamp;
    private String source;
}
