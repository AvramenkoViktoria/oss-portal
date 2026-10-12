package ua.edu.ukma.oss.web.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.edu.ukma.oss.auth.AuthPrincipal;
import ua.edu.ukma.oss.shared.BusinessRuleException;
import ua.edu.ukma.oss.shared.NotFoundException;

/**
 * Maps domain exceptions to user-facing outcomes: rule violations return to the form the
 * user came from with a message, missing and forbidden resources render an error page.
 */
@ControllerAdvice
class WebExceptionHandler {

    private final PageContextAdvice pageContexts;

    WebExceptionHandler(PageContextAdvice pageContexts) {
        this.pageContexts = pageContexts;
    }

    @ExceptionHandler(BusinessRuleException.class)
    String onRuleViolation(BusinessRuleException e, HttpServletRequest request, RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", e.getMessage());
        String referer = request.getHeader("Referer");
        return "redirect:" + (isLocal(referer, request) ? referer : "/");
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String onNotFound(Model model, HttpServletRequest request, @AuthenticationPrincipal AuthPrincipal user) {
        model.addAttribute("ctx", pageContexts.pageContext(user, request));
        model.addAttribute("code", 404);
        model.addAttribute("message", "Сторінку не знайдено");
        return "pages/error";
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    String onDenied(Model model, HttpServletRequest request, @AuthenticationPrincipal AuthPrincipal user) {
        model.addAttribute("ctx", pageContexts.pageContext(user, request));
        model.addAttribute("code", 403);
        model.addAttribute("message", "Недостатньо прав для цієї дії");
        return "pages/error";
    }

    private static boolean isLocal(String referer, HttpServletRequest request) {
        if (referer == null) {
            return false;
        }
        String base = request.getScheme() + "://" + request.getServerName();
        return referer.startsWith(base);
    }
}
