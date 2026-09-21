package com.pickeat.pickeatbackend.domain.pick.dto;

import com.pickeat.pickeatbackend.domain.pick.entity.Pick;
import java.util.List;
import org.springframework.data.domain.Page;

public record PickListResponse(
        List<PickResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static PickListResponse from(Page<Pick> picks) {
        return new PickListResponse(
                picks.getContent().stream().map(PickResponse::from).toList(),
                picks.getNumber(),
                picks.getSize(),
                picks.getTotalElements(),
                picks.getTotalPages()
        );
    }
}
