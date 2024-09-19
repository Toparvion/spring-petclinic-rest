package org.springframework.samples.petclinic.service.perf.memory.ai;

import org.mapstruct.Mapper;

import org.springframework.samples.petclinic.rest.dto.SummaryDto;
import org.springframework.samples.petclinic.service.perf.memory.ai.AiConversation.Summary;

/**
 *
 * @author Vladimir Plizga
 */
@Mapper
public interface SummaryMapper {

    SummaryDto toSummaryDto(Summary summary);
}
