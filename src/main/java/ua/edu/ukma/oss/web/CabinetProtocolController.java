package ua.edu.ukma.oss.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
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
import ua.edu.ukma.oss.voting.VotingDto;
import ua.edu.ukma.oss.voting.VotingService;
import ua.edu.ukma.oss.web.view.ProtocolEntry;
import ua.edu.ukma.oss.web.view.ProtocolPage;

/**
 * The protocol lifecycle workspace: document, votings, approval and publication.
 */
@Controller
class CabinetProtocolController {

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

    @GetMapping("/cabinet/{code}/protocols/{id}")
    String workspace(@PathVariable String code, @PathVariable Long id, @AuthenticationPrincipal AuthPrincipal user,
                     Model model) {
        OrganizationDto org = owned(code, id, user);
        ProtocolPage page = views.protocolPage(id, true);
        model.addAttribute("org", org);
        model.addAttribute("page", page);
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
        byte[] content = file.getBytes();
        // Votings are read first: a protocol without a voting section is rejected before it is attached.
        List<VotingDto> found = votings.importFromDocument(id, file.getOriginalFilename(), content);
        writer.attachFile(id, file.getOriginalFilename(), new ByteArrayInputStream(content));
        redirect.addFlashAttribute("success", "Документ прикріплено, розпізнано голосувань: " + found.size());
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

    private OrganizationDto owned(String code, Long protocolId, AuthPrincipal user) {
        OrganizationDto org = access.requireEditor(user, code);
        if (!reader.get(protocolId).orgId().equals(org.id())) {
            throw new NotFoundException("Protocol", protocolId);
        }
        return org;
    }

    private static String back(String code, Long id) {
        return "redirect:/cabinet/" + code + "/protocols/" + id;
    }
}
