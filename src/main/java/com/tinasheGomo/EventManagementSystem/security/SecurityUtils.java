package com.tinasheGomo.EventManagementSystem.security;

import com.tinasheGomo.EventManagementSystem.entity.organization.OrganizationEntity;
import com.tinasheGomo.EventManagementSystem.entity.user.UserEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static AuthUser getCurrentUser() {
        Authentication auth = getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser) {
            return (AuthUser) auth.getPrincipal();
        }
        return null;
    }

    public static String getCurrentUserEmail() {
        AuthUser user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }

    public static String getCurrentUserRole() {
        AuthUser user = getCurrentUser();
        return user != null ? user.getRole() : null;
    }

    public static UserEntity getCurrentUserEntity() {
        AuthUser authUser = getCurrentUser();
        return authUser != null ? authUser.getUser() : null;
    }

    /**
     * The organisation id of the authenticated user. Used as the anchor for every
     * tenant ownership check.
     */
    public static String getCurrentOrgId() {
        UserEntity user = getCurrentUserEntity();
        if (user == null || user.getOrganization() == null) {
            throw new AccessDeniedException("No organisation is associated with the current user");
        }
        return user.getOrganization().getId();
    }

    /**
     * Rejects the request when the org id in the path does not belong to the caller.
     * Use for org-scoped collection endpoints and for creating records under an org.
     */
    public static void requireOrgAccess(String orgId) {
        if (orgId == null || !getCurrentOrgId().equals(orgId)) {
            throw new AccessDeniedException("You do not have access to this organisation");
        }
    }

    /**
     * Rejects the request when the loaded entity belongs to another organisation.
     * Use after every findById on an org-owned entity.
     */
    public static void requireResourceAccess(OrganizationEntity owner, String resourceLabel) {
        if (owner == null || !getCurrentOrgId().equals(owner.getId())) {
            throw new AccessDeniedException("You do not have access to this " + resourceLabel);
        }
    }
}
