package ua.edu.ukma.oss.web;

import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.edu.ukma.oss.auth.AuthPrincipal;
import ua.edu.ukma.oss.auth.UserService;
import ua.edu.ukma.oss.meeting.MeetingService;
import ua.edu.ukma.oss.meeting.MeetingStatus;
import ua.edu.ukma.oss.organization.OrganizationDto;
import ua.edu.ukma.oss.petition.PetitionDto;
import ua.edu.ukma.oss.petition.PetitionLink;
import ua.edu.ukma.oss.petition.PetitionService;
import ua.edu.ukma.oss.shared.NotFoundException;

/**
 * Handling of the petition queue of one body.
 */
@Controller
class CabinetPetitionController {

    private final WebAccess access;
    private final CabinetSupport support;
    private final PetitionService petitions;
    private final MeetingService meetings;
    private final UserService users;

    CabinetPetitionController(WebAccess access, CabinetSupport support, PetitionService petitions,
                              MeetingService meetings, UserService users) {
        this.access = access;
        this.support = support;
        this.petitions = petitions;
        this.meetings = meetings;
        this.users = users;
    }

    @GetMapping("/cabinet/{code}/petitions")
    String list(@PathVariable String code, @AuthenticationPrincipal AuthPrincipal user, Model model) {
        OrganizationDto org = access.requireEditor(user, code);
        List<PetitionDto> all = petitions.byOrg(org.id());
        model.addAttribute("org", org);
        model.addAttribute("petitions", all);
        model.addAttribute("authors", users.fullNames(all.stream().map(PetitionDto::authorId).toList()));
        model.addAttribute("planned", meetings.byOrg(org.id()).stream()
                .filter(m -> m.status() == MeetingStatus.PLANNED).toList());
        return "cabinet/petitions";
    }

    @PostMapping("/cabinet/{code}/petitions/{id}/agenda")
    String toAgenda(@PathVariable String code, @PathVariable Long id, @AuthenticationPrincipal AuthPrincipal user,
                    @RequestParam Long meetingId, RedirectAttributes redirect) {
        OrganizationDto org = owned(code, id, user);
        support.meetingOf(org, meetingId);
        petitions.attach(id, meetingId, PetitionLink.AGENDA_ITEM);
        redirect.addFlashAttribute("success", "Петицію додано до порядку денного засідання");
        return "redirect:/cabinet/" + code + "/petitions";
    }

    @PostMapping("/cabinet/{code}/petitions/{id}/separate")
    String separateMeeting(@PathVariable String code, @PathVariable Long id,
                           @AuthenticationPrincipal AuthPrincipal user, @RequestParam String title,
                           @RequestParam String scheduledAt, @RequestParam(required = false) String streamUrl,
                           RedirectAttributes redirect) {
        owned(code, id, user);
        PetitionDto scheduled = petitions.scheduleSeparateMeeting(id, title, support.parseWhen(scheduledAt), streamUrl);
        redirect.addFlashAttribute("success", "Призначено окреме засідання для петиції");
        return "redirect:/cabinet/" + code + "/meetings/" + scheduled.meetingId();
    }

    @PostMapping("/cabinet/{code}/petitions/{id}/reject")
    String reject(@PathVariable String code, @PathVariable Long id, @AuthenticationPrincipal AuthPrincipal user,
                  @RequestParam String reason, RedirectAttributes redirect) {
        owned(code, id, user);
        petitions.reject(id, reason);
        redirect.addFlashAttribute("success", "Петицію відхилено");
        return "redirect:/cabinet/" + code + "/petitions";
    }

    private OrganizationDto owned(String code, Long petitionId, AuthPrincipal user) {
        OrganizationDto org = access.requireEditor(user, code);
        if (!petitions.get(petitionId).orgId().equals(org.id())) {
            throw new NotFoundException("Petition", petitionId);
        }
        return org;
    }
}
