package com.tinasheGomo.EventManagementSystem.service.user;

import com.tinasheGomo.EventManagementSystem.dto.user.UpdateRoleRequestDTO;
import com.tinasheGomo.EventManagementSystem.dto.user.UserResponseDTO;
import com.tinasheGomo.EventManagementSystem.entity.organization.OrganizationEntity;
import com.tinasheGomo.EventManagementSystem.entity.user.UserEntity;
import com.tinasheGomo.EventManagementSystem.enums.UserRole;
import com.tinasheGomo.EventManagementSystem.exception.exceptions.BusinessRuleException;
import com.tinasheGomo.EventManagementSystem.exception.exceptions.NotFoundException;
import com.tinasheGomo.EventManagementSystem.mapper.user.UserMapper;
import com.tinasheGomo.EventManagementSystem.repository.organization.OrganizationRepository;
import com.tinasheGomo.EventManagementSystem.repository.user.UserRepository;
import com.tinasheGomo.EventManagementSystem.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final UserMapper userMapper;

    public UserResponseDTO getCurrentUser() {
        UserEntity user = SecurityUtils.getCurrentUserEntity();
        if (user == null) throw new NotFoundException("No authenticated user");
        return userMapper.toResponse(user);
    }

    /**
     * Stores the signed-in user's FCM registration token so push notifications can be
     * delivered. Users update their own record, so no org path or role check is needed
     * beyond authentication.
     */
    @Transactional
    public void updateMyFcmToken(String fcmToken) {
        UserEntity user = SecurityUtils.getCurrentUserEntity();
        if (user == null) throw new NotFoundException("No authenticated user");
        user.setFcmToken(fcmToken);
        userRepository.save(user);
    }

    public List<UserResponseDTO> getUsersByOrg(String orgId) {
        SecurityUtils.requireOrgAccess(orgId);
        OrganizationEntity org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new NotFoundException("Organization not found: " + orgId));
        return userMapper.toResponseList(userRepository.findByOrganization(org));
    }

    public UserResponseDTO getUserById(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        SecurityUtils.requireResourceAccess(user.getOrganization(), "user");
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponseDTO updateUserRole(String orgId, String userId, UpdateRoleRequestDTO request) {
        SecurityUtils.requireOrgAccess(orgId);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        SecurityUtils.requireResourceAccess(user.getOrganization(), "user");
        OrganizationEntity org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new NotFoundException("Organization not found: " + orgId));

        // The path org must be the org the target user actually belongs to.
        if (!org.getId().equals(user.getOrganization().getId())) {
            throw new NotFoundException("User not found in organisation " + orgId + ": " + userId);
        }

        boolean demotingAdmin = user.getRole() == UserRole.ADMIN && request.getRole() != UserRole.ADMIN;
        if (demotingAdmin && userRepository.countByOrganizationAndRole(org, UserRole.ADMIN) <= 1) {
            throw new BusinessRuleException("Cannot demote the last admin");
        }

        user.setRole(request.getRole());
        user = userRepository.save(user);
        return userMapper.toResponse(user);
    }
}
