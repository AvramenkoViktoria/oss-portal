package ua.edu.ukma.oss.web;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.edu.ukma.oss.auth.AuthPrincipal;
import ua.edu.ukma.oss.meeting.MeetingService;
import ua.edu.ukma.oss.organization.OrganizationDto;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.petition.PetitionDto;
import ua.edu.ukma.oss.petition.PetitionService;
import ua.edu.ukma.oss.petition.SubmitCmd;
import ua.edu.ukma.oss.web.view.PetitionRow;

/**
 * Petition submission and the author's own petitions.
 */
@Controller
class PetitionController {

    private final PetitionService petitions;
    private final OrganizationService organizations;
    private final MeetingService meetings;
    private final WebAccess access;

    PetitionController(PetitionService petitions, OrganizationService organizations, MeetingService meetings,
                       WebAccess access) {
        this.petitions = petitions;
        this.organizations = organizations;
        this.meetings = meetings;
        this.access = access;
    }

    @GetMapping("/petitions/new")
    String form(@RequestParam(required = false) String org, Model model) {
        model.addAttribute("orgs", organizations.all());
        model.addAttribute("selected", org == null ? null : access.org(org).code().name());
        return "pages/petitionNew";
    }

    @PostMapping("/petitions")
    String submit(@AuthenticationPrincipal AuthPrincipal user, @RequestParam Long orgId, @RequestParam String title,
                  @RequestParam String text, RedirectAttributes redirect) {
        petitions.submit(new SubmitCmd(user.userId(), orgId, title, text));
        redirect.addFlashAttribute("success", "Петицію подано. Статус можна відстежувати на цій сторінці.");
        return "redirect:/petitions/mine";
    }

    @GetMapping("/petitions/{id:\\d+}")
    String view(@PathVariable Long id, Model model) {
        PetitionDto petition = petitions.get(id);
        model.addAttribute("petition", petition);
        model.addAttribute("org", organizations.get(petition.orgId()));
        model.addAttribute("meetingTitle", petition.meetingId() == null ? null : meetings.get(petition.meetingId()).title());
        return "pages/petition";
    }

    @GetMapping("/petitions/mine")
    String mine(@AuthenticationPrincipal AuthPrincipal user, Model model) {
        Map<Long, OrganizationDto> orgs = organizations.all().stream()
                .collect(Collectors.toMap(OrganizationDto::id, Function.identity()));
        model.addAttribute("rows", petitions.byAuthor(user.userId()).stream()
                .map(p -> new PetitionRow(p, orgs.get(p.orgId()),
                        p.meetingId() == null ? null : meetings.get(p.meetingId()).title()))
                .toList());
        return "pages/petitionsMine";
    }
}
