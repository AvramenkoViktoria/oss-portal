package ua.edu.ukma.oss.voting.internal;

import java.util.List;
import org.springframework.stereotype.Component;
import ua.edu.ukma.oss.protocol.ApprovalRequirementProvider;
import ua.edu.ukma.oss.protocol.Requirement;

/**
 * Contributes the "every voting has recorded votes" precondition to protocol approval.
 */
@Component
class VotingApprovalRequirements implements ApprovalRequirementProvider {

    private final VotingRepository votings;

    VotingApprovalRequirements(VotingRepository votings) {
        this.votings = votings;
    }

    @Override
    public List<Requirement> requirementsFor(Long protocolId) {
        List<Voting> all = votings.findByProtocolIdOrderById(protocolId);
        boolean allRecorded = all.stream().allMatch(v -> v.getResult() != null);
        return List.of(new Requirement("votings", "Для кожного голосування внесено голоси", allRecorded));
    }
}
