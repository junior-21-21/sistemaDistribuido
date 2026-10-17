package com.example.demo.audit;

import org.hibernate.envers.RevisionListener;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuditRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        RevisionInfo revision = (RevisionInfo) revisionEntity;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        revision.setUsername(authentication == null || !authentication.isAuthenticated()
                ? "SYSTEM" : authentication.getName());
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) correlationId = MDC.get("traceId");
        revision.setCorrelationId(correlationId);
    }
}
