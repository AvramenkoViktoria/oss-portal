package ua.edu.ukma.oss.web;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.edu.ukma.oss.auth.AuthPrincipal;
import ua.edu.ukma.oss.meeting.MeetingService;
import ua.edu.ukma.oss.organization.OrganizationDto;
import ua.edu.ukma.oss.protocol.ProtocolDto;
import ua.edu.ukma.oss.protocol.ProtocolReader;
import ua.edu.ukma.oss.protocol.ProtocolStatus;
import ua.edu.ukma.oss.protocol.ProtocolWriter;
import ua.edu.ukma.oss.shared.BusinessRuleException;
import ua.edu.ukma.oss.shared.NotFoundException;
import ua.edu.ukma.oss.voting.CreateVotingCmd;
import ua.edu.ukma.oss.voting.RecordCmd;
import ua.edu.ukma.oss.voting.VoteChoice;
import ua.edu.ukma.oss.voting.VotingDto;
import ua.edu.ukma.oss.voting.VotingService;
import ua.edu.ukma.oss.web.view.ProtocolEntry;
import ua.edu.ukma.oss.web.view.ProtocolPage;

/**
 * The protocol lifecycle workspace: document, votings, approval and publication.
 */
@Controller
class CabinetProtocolController {

    private static final String CHOICE_PREFIX = "choice_";

    private final WebAccess access;
    private final SiteViews views;
    private final ProtocolReader reader;
    private final ProtocolWriter writer;
    private final VotingService votings;
    private final MeetingService meetings;

    CabinetProtocolController(WebAccess access, SiteViews views, ProtocolReader reader, ProtocolWriter writer,
                              VotingService votings, MeetingService meetings) {
        this.access = access;
        this.views = views;
        this.reader = reader;
        this.writer = writer;
        this.votings = votings;
        this.meetings = meetings;
    }

    @GetMapping("/cabinet/{code}/protocols")
    String board(@PathVariable String code, @AuthenticationPrincipal AuthPrincipal user, Model model) {
        OrganizationDto org = access.requireEditor(user, code);
        Map<ProtocolStatus, List<ProtocolEntry>> columns = new EnumMap<>(ProtocolStatus.class);
        Map<Long, Long> unmet = new HashMap<>();
        for (ProtocolStatus status : ProtocolStatus.values()) {
            columns.put(status, List.of());
        }
        for (ProtocolDto p : reader.all(org.id())) {
            List<ProtocolEntry> column = new java.util.ArrayList<>(columns.get(p.status()));
            column.add(new ProtocolEntry(p, meetings.get(p.meetingId())));
            columns.put(p.status(), column);
            if (p.status() == ProtocolStatus.DRAFT) {
                unmet.put(p.id(), reader.approvalRequirements(p.id()).stream().filter(r -> !r.met()).count());
            }
        }
        model.addAttribute("org", org);
        model.addAttribute("columns", columns);
        model.addAttribute("unmet", unmet);
        return "cabinet/protocols";
    }

    @GetMapping("/cabinet/{code}/protocols/{id}")
    String workspace(@PathVariable String code, @PathVariable Long id, @AuthenticationPrincipal AuthPrincipal user,
                     Model model) {
        OrganizationDto org = owned(code, id, user);
        ProtocolPage page = views.protocolPage(id, true);
        model.addAttribute("org", org);
        model.addAttribute("page", page);
        model.addAttribute("members", views.currentMembers(org.id()));
        model.addAttribute("editable", page.protocol().status() == ProtocolStatus.DRAFT);
        return "cabinet/protocol";
    }

    @PostMapping("/cabinet/{code}/protocols/{id}/file")
    String upload(@PathVariable String code, @PathVariable Long id, @AuthenticationPrincipal AuthPrincipal user,
                  @RequestParam("file") MultipartFile file, RedirectAttributes redirect) throws IOException {
        owned(code, id, user);
        if (file.isEmpty()) {
            throw new BusinessRuleException("Оберіть файл PDF або DOCX");
        }
        try (var content = file.getInputStream()) {
            writer.attachFile(id, file.getOriginalFilename(), content);
        }
        redirect.addFlashAttribute("success", "Документ прикріплено");
        return back(code, id);
    }

    @PostMapping("/cabinet/{code}/protocols/{id}/approve")
    String approve(@PathVariable String code, @PathVariable Long id, @AuthenticationPrincipal AuthPrincipal user,
                   RedirectAttributes redirect) {
        owned(code, id, user);
        writer.approve(id, user.userId());
        redirect.addFlashAttribute("success", "Протокол затверджено. Документ і голосування заблоковано.");
        return back(code, id);
    }

    @PostMapping("/cabinet/{code}/protocols/{id}/publish")
    String publish(@PathVariable String code, @PathVariable Long id, @AuthenticationPrincipal AuthPrincipal user,
                   RedirectAttributes redirect) {
        owned(code, id, user);
        writer.publish(id, user.userId());
        redirect.addFlashAttribute("success", "Протокол опубліковано. Анонс поставлено в чергу розсилки.");
        return back(code, id);
    }

    @PostMapping("/cabinet/{code}/protocols/{id}/votings")
    String createVoting(@PathVariable String code, @PathVariable Long id, @AuthenticationPrincipal AuthPrincipal user,
                        @RequestParam String question, @RequestParam(required = false) Long agendaItemId,
                        RedirectAttributes redirect) {
        owned(code, id, user);
        votings.create(new CreateVotingCmd(id, agendaItemId, question));
        redirect.addFlashAttribute("success", "Голосування додано — внесіть голоси членів");
        return back(code, id);
    }

    @PostMapping("/cabinet/{code}/protocols/{id}/votings/{votingId}/record")
    String record(@PathVariable String code, @PathVariable Long id, @PathVariable Long votingId,
                  @AuthenticationPrincipal AuthPrincipal user, @RequestParam Map<String, String> params,
                  RedirectAttributes redirect) {
        owned(code, id, user);
        ownedVoting(id, votingId);
        Map<Long, VoteChoice> choices = new HashMap<>();
        params.forEach((key, value) -> {
            if (key.startsWith(CHOICE_PREFIX)) {
                choices.put(Long.valueOf(key.substring(CHOICE_PREFIX.length())), VoteChoice.valueOf(value));
            }
        });
        VotingDto result = votings.record(new RecordCmd(votingId, choices));
        redirect.addFlashAttribute("success", "Голоси внесено: " + result.votesFor() + " за, "
                + result.votesAgainst() + " проти, " + result.votesAbstain() + " утрималось");
        return back(code, id);
    }

    @PostMapping("/cabinet/{code}/protocols/{id}/votings/{votingId}/delete")
    String deleteVoting(@PathVariable String code, @PathVariable Long id, @PathVariable Long votingId,
                        @AuthenticationPrincipal AuthPrincipal user, RedirectAttributes redirect) {
        owned(code, id, user);
        ownedVoting(id, votingId);
        votings.remove(votingId);
        redirect.addFlashAttribute("success", "Голосування видалено");
        return back(code, id);
    }

    private OrganizationDto owned(String code, Long protocolId, AuthPrincipal user) {
        OrganizationDto org = access.requireEditor(user, code);
        if (!reader.get(protocolId).orgId().equals(org.id())) {
            throw new NotFoundException("Protocol", protocolId);
        }
        return org;
    }

    private void ownedVoting(Long protocolId, Long votingId) {
        if (!votings.get(votingId).protocolId().equals(protocolId)) {
            throw new NotFoundException("Voting", votingId);
        }
    }

    private static String back(String code, Long id) {
        return "redirect:/cabinet/" + code + "/protocols/" + id;
    }
}
