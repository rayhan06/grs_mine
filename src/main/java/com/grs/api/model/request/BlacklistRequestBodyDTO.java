package com.grs.api.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BlacklistRequestBodyDTO {
    private Long complainantId;
    private String blacklistReason;
}
