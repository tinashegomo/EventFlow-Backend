package com.tinasheGomo.EventManagementSystem.service.organization;

import com.tinasheGomo.EventManagementSystem.dto.organization.OrganizationRequestDTO;
import com.tinasheGomo.EventManagementSystem.dto.organization.OrganizationResponseDTO;
import com.tinasheGomo.EventManagementSystem.entity.organization.OrganizationEntity;
import com.tinasheGomo.EventManagementSystem.exception.exceptions.NotFoundException;
import com.tinasheGomo.EventManagementSystem.mapper.organization.OrganizationMapper;
import com.tinasheGomo.EventManagementSystem.repository.organization.OrganizationRepository;
import com.tinasheGomo.EventManagementSystem.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;

    public OrganizationResponseDTO getById(String id) {
        SecurityUtils.requireOrgAccess(id);
        OrganizationEntity org = organizationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Organization not found: " + id));
        return organizationMapper.toResponse(org);
    }

    @Transactional
    public OrganizationResponseDTO update(String id, OrganizationRequestDTO request) {
        SecurityUtils.requireOrgAccess(id);
        OrganizationEntity org = organizationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Organization not found: " + id));
        organizationMapper.updateFromDTO(request, org);
        org = organizationRepository.save(org);
        return organizationMapper.toResponse(org);
    }
}
