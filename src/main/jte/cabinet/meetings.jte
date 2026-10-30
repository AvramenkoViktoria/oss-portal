@import ua.edu.ukma.oss.organization.OrganizationDto
@import ua.edu.ukma.oss.web.support.PageContext
@import ua.edu.ukma.oss.web.view.CalendarView
@param PageContext ctx
@param OrganizationDto org
@param CalendarView calendar
@param String view = "calendar"
!{String code = org.code().name().toLowerCase();}
@template.layout.page(ctx = ctx, title = "Засідання " + org.code().name(), section = "cabinet-" + code, content = @`
    @template.components.cabinetHead(org = org, active = "meetings")
    <section class="card">
        @template.components.calendar(ctx = ctx, cal = calendar, navPath = "/cabinet/" + code + "/meetings", meetingPath = "/cabinet/" + code + "/meetings/", view = view)
    </section>
    <section class="card section">
        <h2>Запланувати засідання</h2>
        <form method="post" action="/cabinet/${code}/meetings">
            <input type="hidden" name="${ctx.csrfParam()}" value="${ctx.csrfToken()}">
            <div class="grid two">
                <div class="field"><label for="title">Назва</label><input id="title" name="title" type="text" required></div>
                <div class="field"><label for="scheduledAt">Дата й час</label><input id="scheduledAt" name="scheduledAt" type="datetime-local" required></div>
            </div>
            <div class="field"><label for="streamUrl">Посилання на трансляцію (необов’язково)</label><input id="streamUrl" name="streamUrl" type="url"></div>
            <button class="btn" type="submit">Запланувати й анонсувати</button>
        </form>
    </section>
`)
