package ua.edu.ukma.oss.protocol;

import java.util.List;

/**
 * Extension point: other modules contribute preconditions of approval (for example that every
 * voting has recorded results) without the protocol module depending on them.
 */
public interface ApprovalRequirementProvider {

    List<Requirement> requirementsFor(Long protocolId);
}
