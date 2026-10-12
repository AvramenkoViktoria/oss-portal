package ua.edu.ukma.oss.web;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.meeting.MeetingService;
import ua.edu.ukma.oss.meeting.MeetingStatus;
import ua.edu.ukma.oss.meeting.ScheduleCmd;
import ua.edu.ukma.oss.organization.OrganizationDto;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.petition.PetitionDto;
import ua.edu.ukma.oss.petition.PetitionService;
import ua.edu.ukma.oss.petition.PetitionStatus;
import ua.edu.ukma.oss.protocol.ProtocolDto;
import ua.edu.ukma.oss.protocol.ProtocolReader;
import ua.edu.ukma.oss.protocol.ProtocolStatus;
import ua.edu.ukma.oss.web.view.CalendarView;
import ua.edu.ukma.oss.web.view.ProtocolEntry;

/**
 * Editors' cabinet: overview and meeting management of one body.
 */
@Controller
class CabinetController {

    private final WebAccess access;
    private final CabinetSupport support;
    private final MeetingService meetings;
    private final PetitionService petitions;
    private final ProtocolReader protocols;
    private final OrganizationService organizations;
    private final UserService users;
    private final SiteViews views;
    private final Clock clock;

    CabinetController(WebAccess access, CabinetSupport support, MeetingService meetings, PetitionService petitions,
                      ProtocolReader protocols, OrganizationService organizations, UserService users, SiteViews views, Clock clock) {
        this.access = access;
        this.support = support;
        this.meetings = meetings;
        this.petitions = petitions;
        this.protocols = protocols;
        this.organizations = organizations;
        this.users = users;
        this.views = views;
        this.clock = clock;
    }

    @GetMapping("/cabinet")
    String index(@AuthenticationPrincipal AuthPrincipal user) {
        return organizations.all().stream()
                .filter(o -> user.orgIds().contains(o.id())).findFirst()
                .map(o -> "redirect:/cabinet/" + CabinetSupport.codeOf(o))
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("No cabinet"));
    }

    @GetMapping("/cabinet/{code}")
    String dashboard(@PathVariable String code, @AuthenticationPrincipal AuthPrincipal user, Model model) {
        OrganizationDto org = access.requireEditor(user, code);
        Instant now = clock.instant();
        List<MeetingDto> all = meetings.byOrg(org.id());
        List<PetitionDto> queue = petitions.byOrg(org.id()).stream()
                .filter(p -> p.status() == PetitionStatus.SUBMITTED).toList();
        List<ProtocolDto> inWork = protocols.all(org.id()).stream()
                .filter(p -> p.status() != ProtocolStatus.PUBLISHED).toList();
        model.addAttribute("org", org);
        model.addAttribute("toHold", all.stream().filter(m -> m.status() == MeetingStatus.PLANNED
                && !m.scheduledAt().isAfter(now)).toList());
        model.addAttribute("upcoming", all.stream().filter(m -> m.status() == MeetingStatus.PLANNED
                && m.scheduledAt().isAfter(now)).sorted(Comparator.comparing(MeetingDto::scheduledAt)).toList());
        model.addAttribute("queue", queue);
        model.addAttribute("authors", users.fullNames(queue.stream().map(PetitionDto::authorId).toList()));
        model.addAttribute("inWork", inWork.stream().map(p -> new ProtocolEntry(p, meetings.get(p.meetingId()))).toList());
        return "cabinet/dashboard";
    }

    @GetMapping("/cabinet/{code}/meetings")
    String meetingList(@PathVariable String code, @RequestParam(required = false) String month,
                       @AuthenticationPrincipal AuthPrincipal user, Model model) {
        OrganizationDto org = access.requireEditor(user, code);
        model.addAttribute("org", org);
        model.addAttribute("calendar", views.calendar(CalendarView.parseMonth(month, views.currentMonth()),
                meetings.byOrg(org.id())));
        return "cabinet/meetings";
    }

    @PostMapping("/cabinet/{code}/meetings")
    String create(@PathVariable String code, @AuthenticationPrincipal AuthPrincipal user, @RequestParam String title,
                  @RequestParam String scheduledAt, @RequestParam(required = false) String streamUrl,
                  RedirectAttributes redirect) {
        OrganizationDto org = access.requireEditor(user, code);
        MeetingDto meeting = meetings.schedule(new ScheduleCmd(org.id(), title, support.parseWhen(scheduledAt), streamUrl));
        redirect.addFlashAttribute("success", "Засідання заплановано, анонс поставлено в чергу розсилки");
        return "redirect:/cabinet/" + code + "/meetings/" + meeting.id();
    }

    @GetMapping("/cabinet/{code}/meetings/{id}")
    String meeting(@PathVariable String code, @PathVariable Long id, @AuthenticationPrincipal AuthPrincipal user,
                   Model model) {
        OrganizationDto org = access.requireEditor(user, code);
        MeetingDto meeting = support.meetingOf(org, id);
        model.addAttribute("org", org);
        model.addAttribute("meeting", meeting);
        model.addAttribute("started", !meeting.scheduledAt().isAfter(clock.instant()));
        model.addAttribute("petitions", petitions.byMeeting(id));
        model.addAttribute("protocol", protocols.byMeeting(id).orElse(null));
        return "cabinet/meeting";
    }

    @PostMapping("/cabinet/{code}/meetings/{id}/update")
    String update(@PathVariable String code, @PathVariable Long id, @AuthenticationPrincipal AuthPrincipal user,
                  @RequestParam String title, @RequestParam String scheduledAt,
                  @RequestParam(required = false) String streamUrl, RedirectAttributes redirect) {
        support.meetingOf(access.requireEditor(user, code), id);
        meetings.reschedule(id, title, support.parseWhen(scheduledAt), streamUrl);
        redirect.addFlashAttribute("success", "Зміни збережено");
        return "redirect:/cabinet/" + code + "/meetings/" + id;
    }

    @PostMapping("/cabinet/{code}/meetings/{id}/agenda")
    String addAgendaItem(@PathVariable String code, @PathVariable Long id, @AuthenticationPrincipal AuthPrincipal user,
                         @RequestParam String title, RedirectAttributes redirect) {
        support.meetingOf(access.requireEditor(user, code), id);
        meetings.addAgendaItem(id, title);
        redirect.addFlashAttribute("success", "Пункт додано до порядку денного");
        return "redirect:/cabinet/" + code + "/meetings/" + id;
    }

    @PostMapping("/cabinet/{code}/meetings/{id}/held")
    String markHeld(@PathVariable String code, @PathVariable Long id, @AuthenticationPrincipal AuthPrincipal user,
                    RedirectAttributes redirect) {
        support.meetingOf(access.requireEditor(user, code), id);
        meetings.markHeld(id);
        redirect.addFlashAttribute("success", "Засідання проведено. Створено чернетку протоколу — відкрийте його нижче.");
        return "redirect:/cabinet/" + code + "/meetings/" + id;
    }

    @PostMapping("/cabinet/{code}/meetings/{id}/cancel")
    String cancel(@PathVariable String code, @PathVariable Long id, @AuthenticationPrincipal AuthPrincipal user,
                  RedirectAttributes redirect) {
        support.meetingOf(access.requireEditor(user, code), id);
        meetings.cancel(id);
        redirect.addFlashAttribute("success", "Засідання скасовано");
        return "redirect:/cabinet/" + code + "/meetings/" + id;
    }
}
