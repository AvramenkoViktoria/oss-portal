package ua.edu.ukma.oss.web;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.edu.ukma.oss.notification.NotificationAdminService;
import ua.edu.ukma.oss.organization.Messenger;
import ua.edu.ukma.oss.organization.OrganizationDto;
import ua.edu.ukma.oss.organization.OrganizationService;

/**
 * Monitor of the notification pipeline and controls for the stub messengers.
 */
@Controller
class AdminNotificationController {

    private static final int RECENT = 50;

    private final NotificationAdminService notifications;
    private final OrganizationService organizations;

    AdminNotificationController(NotificationAdminService notifications, OrganizationService organizations) {
        this.notifications = notifications;
        this.organizations = organizations;
    }

    @GetMapping("/admin/notifications")
    String monitor(Model model) {
        Map<Long, OrganizationDto> orgs = organizations.all().stream()
                .collect(Collectors.toMap(OrganizationDto::id, Function.identity()));
        model.addAttribute("entries", notifications.recent(RECENT));
        model.addAttribute("orgs", orgs);
        model.addAttribute("delivered", notifications.deliveredMessages());
        model.addAttribute("outages", Arrays.stream(Messenger.values())
                .collect(Collectors.toMap(Function.identity(), notifications::isOutageSimulated)));
        return "admin/notifications";
    }

    @PostMapping("/admin/notifications/process")
    String process(RedirectAttributes redirect) {
        int attempted = notifications.processDueNow();
        redirect.addFlashAttribute("success", "Оброблено повідомлень: " + attempted);
        return "redirect:/admin/notifications";
    }

    @PostMapping("/admin/notifications/{id}/requeue")
    String requeue(@PathVariable Long id, RedirectAttributes redirect) {
        notifications.requeue(id);
        redirect.addFlashAttribute("success", "Повідомлення повернено в чергу");
        return "redirect:/admin/notifications";
    }

    @PostMapping("/admin/notifications/outage")
    String outage(@RequestParam Messenger messenger, @RequestParam boolean outage, RedirectAttributes redirect) {
        notifications.simulateOutage(messenger, outage);
        redirect.addFlashAttribute("success", outage
                ? "Імітацію збою " + messenger + " увімкнено" : "Імітацію збою " + messenger + " вимкнено");
        return "redirect:/admin/notifications";
    }
}
